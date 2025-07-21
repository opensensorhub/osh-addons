package org.sensorhub.impl.process.video.transcoder.coders;

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
public abstract class Coder<I, O> extends Thread {

    protected static final Logger logger = LoggerFactory.getLogger(Coder.class);

    int codecId;
    private static final int MAX_QUEUE_SIZE = 500;
    protected AVCodecContext codec_ctx;
    protected Queue<I> inPackets; // ONLY allow the main loop to poll
    protected final Queue<O> outPackets; // ONLY allow the main loop to add
    protected I inPacket;
    protected O outPacket;
    //volatile boolean isProcessing = false; // Set to true at the start of the loop, false at the end
    volatile boolean isGettingPackets = false;
    final Object waitingObj = new Object();
    Class<I> inputClass;
    Class<O> outputClass;
    HashMap<String, Integer> options;

    public AtomicBoolean doRun = new AtomicBoolean(true);

    public Coder(int codecId, Class<I> inputClass, Class<O> outputClass, HashMap<String, Integer> options) {
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

    public Queue<O> getOutQueue() {
        return outPackets;
    }

    public void setInQueue(Queue <?> inPackets) {
        this.inPackets = (Queue<I>) inPackets;
    }

    // Blocks while a frame is being processed
    // DO NOT CALL THIS IF USING SET IN PACKETS
    // Old method, connect coders using getOutQueue and setInQueue
    /*
    public Queue<O> getPackets() throws InterruptedException {

        synchronized (waitingObj) {
            if (doRun.get()) {
                //logger.debug("Waiting");
                waitingObj.wait();
                //logger.debug("Done");
            }
        }

        if (outPackets.isEmpty()) {
            //logger.debug("No output packets");
            return new ArrayDeque<O>();
        }

        //logger.debug("Grabbing output packets");
        //isGettingPackets = true;
        //Queue<O> packets = new ArrayDeque<O>(outPackets);
        //deallocateOutQueue();
        Queue<O> packets = outPackets;
        outPackets = new ArrayDeque<>();
        return packets;
    }
     */

    // Safety net queue purging.
    // Sometimes, queues can grow very large (like when a thread hangs up or is paused in the debugger).
    // Can recover, so we don't want memory issues from too many items in the queues.
    private void queuePurge() {
        /*
        synchronized (inPackets) {
            if (inPackets.size() > MAX_QUEUE_SIZE) { logger.warn("Input queue is larger than max ({} > {}). Purging queue.", inPackets.size(), MAX_QUEUE_SIZE); }
            while (inPackets.size() > MAX_QUEUE_SIZE) {
                deallocateInputPacket(inPackets.poll());
            }
        }

         */

        synchronized (outPackets) {
            if (outPackets.size() > MAX_QUEUE_SIZE) { logger.warn("Output queue is larger than max ({} > {}). Purging queue.", outPackets.size(), MAX_QUEUE_SIZE); }
            while (outPackets.size() > MAX_QUEUE_SIZE) {
                deallocateOutputPacket(outPackets.poll());
            }
        }
    }

    protected abstract void deallocateInputPacket(I packet);

    protected abstract void deallocateOutputPacket(O packet);

    protected abstract void deallocateOutQueue();

    // To be implemented by subclasses, encoder/decoder
    protected abstract void initContext();

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

        codec_ctx.time_base(av_make_q(1, options.getOrDefault("fps", 30)));

        if (options.containsKey("bit_rate")) {
            codec_ctx.bit_rate(options.get("bit_rate") * 1000);
        } else {
            //codec_ctx.bit_rate(150*1000);
        }

        codec_ctx.width(options.getOrDefault("width", 1920));

        codec_ctx.height(options.getOrDefault("height", 1080));

        if (options.containsKey("pix_fmt")) {
            codec_ctx.pix_fmt(options.get("pix_fmt"));
        } else {
            if (codecId == AV_CODEC_ID_MJPEG) {
                codec_ctx.pix_fmt(AV_PIX_FMT_YUVJ420P);
            } else {
                codec_ctx.pix_fmt(AV_PIX_FMT_YUV420P);
            }
        }

        av_opt_set(codec_ctx.priv_data(), "preset", "ultrafast", 0);
        av_opt_set(codec_ctx.priv_data(), "tune", "zerolatency", 0);
        codec_ctx.strict_std_compliance(FF_COMPLIANCE_UNOFFICIAL); // Needed so that yuvj420p works (used for mjpeg)
    }

    @Override
    public void run() {
        initContext();
        inPacket = (I)(new Object());
        outPacket = (O)(new Object());

        allocatePackets();

        // init FFMPEG logging
        av_log_set_level(logger.isDebugEnabled() ? AV_LOG_INFO : AV_LOG_FATAL);
        //av_log_set_level(AV_LOG_DEBUG);

        // Actual start of run
        // Codec should be initialized by now
        doRun.set(true);

        while (doRun.get() && !Thread.currentThread().isInterrupted()) {
            while (inPackets == null || inPackets.isEmpty()) {
                if (!doRun.get() || Thread.currentThread().isInterrupted()) { break; }
                Thread.onSpinWait();
            }
            while (inPackets != null && !inPackets.isEmpty()) {
                if (!doRun.get() || Thread.currentThread().isInterrupted()) { break; }

                queuePurge();

                //logger.debug("Queue size: {}", inPackets.size());
                // Get data from in queue
                // Send data to encoder/decoder
                //logger.debug("{}: Sending packet", this.getClass().getName());
                sendInPacket();

                // Receive data from encoder/decoder
                // Add data to out queue
                //logger.debug("{}: Receiving packet", this.getClass().getName());
                receiveOutPacket();

                //isProcessing = false;
                // Wake a thread waiting for packets.

            }
        }

        // End of coding
        synchronized (waitingObj) {
            waitingObj.notifyAll();
        }

        deallocatePackets();
        if (codec_ctx != null) {
            avcodec_free_context(codec_ctx);
        }
        codec_ctx = null;
    }
}
