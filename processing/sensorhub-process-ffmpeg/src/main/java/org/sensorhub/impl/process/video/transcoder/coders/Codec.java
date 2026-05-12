package org.sensorhub.impl.process.video.transcoder.coders;

import static org.bytedeco.ffmpeg.global.avutil.*;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.bytedeco.ffmpeg.global.avcodec.*;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.sensorhub.impl.process.video.transcoder.helpers.CodecInfo;
import org.sensorhub.impl.process.video.transcoder.helpers.CodecOptions;
import org.sensorhub.impl.process.video.transcoder.helpers.FullCodecEnum;
import org.sensorhub.impl.process.video.transcoder.helpers.FullPixelEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Codec<I extends Pointer, O extends Pointer> implements AutoCloseable {


    public interface CoderCallback<O extends Pointer> {
        // The recipient does not need to deallocate the output; this is done automatically
        public abstract void onPacket(O packet);
    }

    protected static final Logger logger = LoggerFactory.getLogger(Codec.class);

    private static int codecCount = 0;
    private final int codecNum = codecCount++;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(
            r -> new Thread(r, "ffmpeg-codec-thread-" + codecNum));
    private final ExecutorService outputExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "ffmpeg-codec-output-thread-" + codecNum));
    private final Map<CoderCallback<O>, ExecutorService> callbackMap = new HashMap<>();

    CodecInfo inputFormat;
    CodecInfo outputFormat;
    protected AVCodecContext codec_ctx;
    protected AVCodec codec;
    protected final Queue<O> outQueue = new ArrayDeque<>(10);
    private final AtomicBoolean isProcessing = new AtomicBoolean(false); // Set false to indicate packets should no longer be accepted
    final Object contextLock = new Object();
    Class<I> inputClass;
    Class<O> outputClass;
    CodecOptions options;
    AtomicBoolean isNotifying = new AtomicBoolean(false);

    public Codec(CodecInfo inFormatInfo, CodecInfo outFormatInfo, Class<I> inputClass, Class<O> outputClass, CodecOptions options) {
        super();

        if ((inputClass != AVPacket.class && inputClass != AVFrame.class)
        || (outputClass != AVPacket.class && outputClass != AVFrame.class)) {
            throw new IllegalArgumentException("Input and output classes must be either AVPacket or AVFrame");
        }

        this.inputFormat = inFormatInfo.clone();
        this.inputClass = inputClass;
        this.outputFormat = outFormatInfo.clone();
        this.outputClass = outputClass;
        this.options = options;
    }

    public FullPixelEnum init() {
        initContext();
        initOptions();
        var pixFmt = openContext();
        isProcessing.set(true);
        return pixFmt;
    }

    private void submitTask(Runnable task) {
        if (!executor.isShutdown()) {
            executor.submit(() -> {
                try {
                    task.run();
                } catch (Throwable t) {
                    // Prevent silent thread replacement — log and propagate
                    // only after codec state is consistent
                    logger.error("Fatal error in codec runner thread", t);
                    //throw t; // will kill this task but not spawn a new thread silently
                }
            });
        }
    }

    public boolean isReady() {
        return isProcessing.get();
    }

    public Class getOutputClass() {
        return outputClass;
    }

    public Class getInputClass() {
        return inputClass;
    }

    protected abstract void initContext();

    protected FullPixelEnum openContext() {
        int ret;
        FullPixelEnum pixelFmt;
        if ((ret = avcodec_open2(codec_ctx, codec, (PointerPointer<?>) null)) < 0) {
            logFFmpeg(ret);
            throw new IllegalStateException("Error opening codec " + codec.name().getString());
        }
        try {
            var desc = av_pix_fmt_desc_get(codec_ctx.pix_fmt());
            pixelFmt = FullPixelEnum.valueOf(desc.name().getString().toUpperCase());
            inputFormat.pixelFmt = pixelFmt;
            outputFormat.pixelFmt = pixelFmt;
            desc.deallocate();
        } catch (Exception e) {
            pixelFmt = null;
            logger.warn("Could not determine codec info for " + codec.name().getString(), e);
        }
        return pixelFmt;
    }

    protected static void logFFmpeg(int retCode) {
        BytePointer buf = new BytePointer(AV_ERROR_MAX_STRING_SIZE);
        av_strerror(retCode, buf, buf.capacity());
        logger.warn("FFmpeg returned error code {}: {}", retCode, buf.getString());
    }

    protected static void setCodecPixFmt(AVCodecContext codec_ctx, FullPixelEnum desiredFmt) {
        String codecString = codec_ctx.codec().name().getString();
        PointerPointer<IntPointer> pixelFmts = new PointerPointer<>(1);

        avcodec_get_supported_config(codec_ctx, null, AV_CODEC_CONFIG_PIX_FORMAT, 0, pixelFmts, (IntPointer) null);

        IntPointer fmts = pixelFmts.get(IntPointer.class, 0);
        // If null, all formats are supported
        if (fmts == null || fmts.isNull()) {
            if (desiredFmt == FullPixelEnum.NONE) {
                codec_ctx.pix_fmt(AV_PIX_FMT_YUV420P);
            } else {
                codec_ctx.pix_fmt(desiredFmt.ffmpegId);
            }
        }
        else {
            boolean found = false;
            for (int i = 0; fmts.get(i) != AV_PIX_FMT_NONE; i++) {
                if (fmts.get(i) == desiredFmt.ffmpegId) {
                    found = true;
                    codec_ctx.pix_fmt(fmts.get(i));
                    break;
                }
            }
            if (!found) {
                logger.warn("Preferred pixel format for codec {} could not be found", codecString);
                codec_ctx.pix_fmt(fmts.get(0));
            }
        }

        if (fmts != null)
            fmts.deallocate();
        pixelFmts.deallocate();
    }

    /**
     * Set certain options in the codec context.
     * Context must be allocated first using {@link #initContext()}.
     */
    protected void initOptions() {

        if (options.bitRate() > 0) {
            codec_ctx.bit_rate(options.bitRate() * 1000);
        } else {
            codec_ctx.bit_rate(0);
        }

        codec_ctx.width(options.width());
        codec_ctx.height(options.height());

        if (options.fps() > 0) {
            codec_ctx.framerate(av_make_q(options.fps(), 1));
            codec_ctx.time_base(av_make_q(1, options.fps()));
        } else {
            codec_ctx.framerate(av_make_q(25, 1));
            codec_ctx.time_base(av_make_q(1, 25));
        }

        if (inputFormat.codec.ffmpegId == AV_CODEC_ID_H264) {
            // OpenH264 only supports Baseline (66) and Main (77)
            codec_ctx.profile(AV_PROFILE_H264_MAIN);

            // Enable frame skip so bitrate control works correctly,
            // or it falls back to quality mode and ignores the bitrate setting
            av_opt_set(codec_ctx.priv_data(), "skip_frames", "1", 0);

            // OpenH264 uses slice_mode instead of preset
            av_opt_set(codec_ctx.priv_data(), "slice_mode", "auto", 0);
        }

        //av_opt_set(codec_ctx.priv_data(), "preset", options.preset(), 0);
        //av_opt_set(codec_ctx.priv_data(), "tune", options.tune(), 0);
        codec_ctx.strict_std_compliance(options.compliance()); // Needed so that yuvj420p works (used for mjpeg, must be set to unofficial)
    }

    public abstract void deallocateInputPacket(I packet);
    public abstract void deallocateOutputPacket(O packet);
    protected abstract O cloneOutput(O packet);
    protected abstract void processInputPacket(I inputPacket);

    // Take data from input queue and send to encoder/decoder
    public void submitInputPacket(I inputPacket) {
        synchronized (contextLock) {
            if (inputPacket == null || !isProcessing.get()) {
                return;
            }
            submitTask(() -> {
                // Process the input
                processInputPacket(inputPacket);
                deallocateInputPacket(inputPacket);

                if (!outQueue.isEmpty() && isNotifying.compareAndSet(false, true)) {
                    outputExecutor.submit(() -> {
                        while (!outQueue.isEmpty()) {
                            var outputPacket = outQueue.poll();
                            notifyCallbacks(outputPacket);
                        }
                        isNotifying.set(false);
                    });
                }
            });
        }
    }

    private void notifyCallbacks(O outputPacket) {
        for (var entry: callbackMap.entrySet()) {
            var clonedOutputPacket = cloneOutput(outputPacket);
            entry.getValue().submit(() -> {
                entry.getKey().onPacket(clonedOutputPacket);
            });
        }
        deallocateOutputPacket(outputPacket);
    }

    public void registerCallback(CoderCallback<O> callback) {
        if (!callbackMap.containsKey(callback)) {
            callbackMap.put(callback, Executors.newSingleThreadExecutor(r -> new Thread(r, "ffmpeg-codec-" + codecNum + "-callback-thread")));
        } else {
            logger.warn("This callback was already registered for codec " + codecNum);
        }
    }

    public void unregisterCallback(CoderCallback<O> callback) {
        callbackMap.remove(callback);
    }

    public void unregisterAllCallbacks() {
        callbackMap.clear();
    }

    @Override
    public void close() {
        synchronized (contextLock) {
            if (isProcessing.compareAndSet(true, false)) {

                // Submit cleanup *before* shutdown so it is the last task to run
                executor.submit(this::cleanup);
                executor.shutdownNow();
                try {
                    executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                } catch (InterruptedException ignored) {
                    logger.warn("Interrupted while waiting for ffmpeg thread to finish");
                    Thread.currentThread().interrupt();
                }

                outputExecutor.shutdownNow();
                try {
                    outputExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                } catch (InterruptedException ignored) {
                    logger.warn("Interrupted while waiting for ffmpeg thread to finish");
                    Thread.currentThread().interrupt();
                }

                unregisterAllCallbacks();
            }
        }
    }

    private void cleanup() {
        if (codec_ctx != null) {
            avcodec_free_context(codec_ctx);
        }
        codec_ctx = null;
        codec = null;

        unregisterAllCallbacks();

        for (var packet : outQueue) {
            deallocateOutputPacket(packet);
        }
    }
}
