package org.sensorhub.impl.process.video.transcoder;

import static org.bytedeco.ffmpeg.global.avutil.*;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.bytedeco.ffmpeg.global.avcodec.*;

import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO Need to handle case where queues fill faster than packets can be processed. Should check queue size and free space when necessary to avoid crash.
public abstract class Coder<I, O> implements Runnable {

    protected static final Logger logger = LoggerFactory.getLogger(Coder.class);

    int codecId;
    protected AVCodecContext codec_ctx;
    protected volatile Queue<I> inPackets; // ONLY allow the main loop to poll
    protected volatile Queue<O> outPackets; // ONLY allow the main loop to add
    protected I inPacket;
    protected O outPacket;
    //volatile boolean isProcessing = false; // Set to true at the start of the loop, false at the end
    volatile boolean isGettingPackets = false;
    final Object waitingObj = new Object();
    Class<I> inputClass;
    Class<O> outputClass;
    HashMap<String, String> options;

    public AtomicBoolean doRun = new AtomicBoolean(true);

    public Coder(int codecId, Class<I> inputClass, Class<O> outputClass, HashMap<String, String> options) {
        super();

        assert inputClass == AVPacket.class || inputClass == AVFrame.class;
        assert outputClass == AVPacket.class || outputClass == AVFrame.class;
        assert options != null;

        this.codecId = codecId;
        this.inPackets = new ArrayDeque<>();
        this.outPackets = new ArrayDeque<>();
        this.inputClass = inputClass;
        this.outputClass = outputClass;
        this.options = options;
    }

    // Add packet to queue
    public void addPacket(final I packet) {
        inPackets.add(packet);
    }

    // Blocks while a frame is being processed
    public Queue<O> getPackets() throws InterruptedException {

        synchronized (waitingObj) {
            if (doRun.get()) {
                logger.debug("Waiting");
                waitingObj.wait();
                logger.debug("Done");
            }
        }

        if (outPackets.isEmpty()) {
            logger.debug("No output packets");
            return new ArrayDeque<O>();
        }

        logger.debug("Grabbing output packets");
        //isGettingPackets = true;
        //Queue<O> packets = new ArrayDeque<O>(outPackets);
        //deallocateOutQueue();
        Queue<O> packets = outPackets;
        outPackets = new ArrayDeque<>();
        return packets;
    }

    protected abstract void deallocateOutQueue();

    // To be implemented by subclasses, encoder/decoder
    protected abstract void initCodec();

    public void stop() {
        doRun.set(false);
    }

    // Take data from input queue and send to encoder/decoder
    protected abstract void sendInPacket();

    // Take data from encoder/decoder and send to output queue
    protected abstract void receiveOutPacket();

    // Allocate packets/frames
    protected abstract void allocatePackets();

    // Deallocate packets/frames
    protected abstract void deallocatePackets();

    // Set options in codec context. Context must be allocated prior to calling.
    protected void initOptions(AVCodecContext codec_ctx) {

        if (options.containsKey("fps")) {
            codec_ctx.time_base(av_make_q(1, Integer.parseInt(options.get("fps"))));
        } else {
            codec_ctx.time_base(av_make_q(1, 30));
        }

        if (options.containsKey("bit_rate")) {
            codec_ctx.bit_rate(Integer.parseInt(options.get("bit_rate")) * 1000);
        } else {
            codec_ctx.bit_rate(150*1000);
        }

        if (options.containsKey("width")) {
            codec_ctx.width(Integer.parseInt(options.get("width")));
        } else {
            codec_ctx.width(1280);
        }

        if (options.containsKey("height")) {
            codec_ctx.height(Integer.parseInt(options.get("height")));
        } else {
            codec_ctx.height(720);
        }

        if (options.containsKey("pix_fmt")) {
            if (options.get("pix_fmt").equals("yuv420p")) {
                codec_ctx.pix_fmt(AV_PIX_FMT_YUV420P);
            } else if (options.get("pix_fmt").equals("rgb24")) {
                codec_ctx.pix_fmt(AV_PIX_FMT_RGB24);
            }
            // TODO Add more uncompressed formats (or find a better way of doing this)
        } else {
            codec_ctx.pix_fmt(AV_PIX_FMT_YUV420P);
        }

        av_opt_set(codec_ctx.priv_data(), "preset", "ultrafast", 0);
        av_opt_set(codec_ctx.priv_data(), "tune", "zerolatency", 0);
        //codec_ctx.strict_std_compliance(AVCodecCon);
    }

    @Override
    public void run() {
        initCodec();
        inPacket = (I)(new Object());
        outPacket = (O)(new Object());

        allocatePackets();
        this.inPackets = new ArrayDeque<>();
        this.outPackets = new ArrayDeque<>();

        // init FFMPEG logging
        av_log_set_level(logger.isDebugEnabled() ? AV_LOG_INFO : AV_LOG_FATAL);
        //av_log_set_level(AV_LOG_DEBUG);

        // Actual start of run
        // Codec should be initialized by now
        doRun.set(true);

        while (doRun.get() && !Thread.currentThread().isInterrupted()) {
            while (inPackets.isEmpty()) {
                if (!doRun.get() || Thread.currentThread().isInterrupted()) { break; }
                Thread.onSpinWait();
            }
            while (!inPackets.isEmpty()) {
                if (!doRun.get() || Thread.currentThread().isInterrupted()) { break; }

                logger.debug("Queue size: {}", inPackets.size());
                // Get data from in queue
                // Send data to encoder/decoder
                logger.debug("{}: Sending packet", this.getClass().getName());
                sendInPacket();

                // Receive data from encoder/decoder
                // Add data to out queue
                logger.debug("{}: Receiving packet", this.getClass().getName());
                receiveOutPacket();

                //isProcessing = false;
                // Wake a thread waiting for packets.

                synchronized (waitingObj) {
                    logger.debug("Notifying waiting thread");
                    waitingObj.notify();
                }
            }
        }

        // End of coding
        synchronized (waitingObj) {
            waitingObj.notifyAll();
        }

        deallocatePackets();
        avcodec_free_context(codec_ctx);
        codec_ctx = null;
    }
}
