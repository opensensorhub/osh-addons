package org.sensorhub.impl.process.video.transcoder;

import static org.bytedeco.ffmpeg.global.avutil.*;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.bytedeco.ffmpeg.global.avcodec.*;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class Coder<I, O> implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(Coder.class);

    String codecName;
    protected Queue<I> inPackets; // ONLY allow the main loop to poll
    protected Queue<O> outPackets; // ONLY allow the main loop to add
    private I inPacket;
    private O outPacket;
    //volatile boolean isProcessing = false; // Set to true at the start of the loop, false at the end
    //boolean isGettingPackets = false;
    Thread waitingThread;
    Class<I> inputClass;
    Class<O> outputClass;

    public AtomicBoolean doRun = new AtomicBoolean(true);

    public Coder(String codecName, Class<I> inputClass, Class<O> outputClass) {
        super();

        assert inputClass == AVPacket.class || inputClass == AVFrame.class;
        assert outputClass == AVPacket.class || outputClass == AVFrame.class;

        this.codecName = codecName;
        this.inPackets = new ConcurrentLinkedQueue<I>();
        this.outPackets = new ConcurrentLinkedQueue<O>();
        this.inputClass = inputClass;
        this.outputClass = outputClass;
    }

    // Add packet to queue
    public synchronized void addPacket(final I packet) {
        inPackets.add(packet);
    }

    // Blocks while a frame is being processed
    public synchronized Queue<O> getPackets() {
        waitingThread = Thread.currentThread();
        synchronized (waitingThread) {
            if (doRun.get()) {
                try {
                    waitingThread.wait();
                } catch (InterruptedException e) {
                    logger.error("Interrupted while waiting for packets", e);
                }
            }
        }

        if (outPackets.isEmpty()) {
            return null;
        }

        //isGettingPackets = true;
        Queue<O> packets = new ArrayDeque<O>(outPackets);
        outPackets.clear();
        //isGettingPackets = false;
        waitingThread = null;
        return packets;
    }

    // To be implemented by subclasses, encoder/decoder
    protected abstract void initCodec(AVCodec codec, AVCodecContext ctx);

    public void stop() {
        doRun.set(false);
    }

    // Take data from input queue and send to encoder/decoder
    protected abstract void sendPacket(AVCodecContext codec_ctx, I packet);

    // Take data from encoder/decoder and send to output queue
    protected abstract void receivePacket(AVCodecContext codec_ctx, O packet);

    // Allocate packets/frames
    protected abstract void allocatePackets(I inPacket, O outPacket);

    // Deallocate packets/frames
    protected abstract void deallocatePackets(I inPacket, O outPacket);

    @Override
    public void run() {
        AVCodecContext codec_ctx = null;
        AVCodec codec = null;

        // init decoder context
        initCodec(codec, codec_ctx);

        allocatePackets(inPacket, outPacket);

        // init FFMPEG objects
        av_log_set_level(logger.isDebugEnabled() ? AV_LOG_INFO : AV_LOG_FATAL);

        // Actual start of run
        // Codec should be initialized by now
        doRun.set(true);

        while (doRun.get()) {
            while (inPackets.isEmpty()) {
                Thread.onSpinWait();
            }
            while (!inPackets.isEmpty()) {
                // Get data from in queue
                // Send data to encoder/decoder
                sendPacket(codec_ctx, inPacket);

                // Receive data from encoder/decoder
                // Add data to out queue
                receivePacket(codec_ctx, outPacket);

                //isProcessing = false;
                if (waitingThread != null) {
                    waitingThread.notify();
                }
            }
        }

        // End of coding
        if (waitingThread != null) {
            waitingThread.notify();
        }

        deallocatePackets(inPacket, outPacket);

        avcodec_free_context(codec_ctx);
    }
}
