/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.mpegts;

import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.PointerPointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.bytedeco.ffmpeg.global.avformat.av_read_frame;

/**
 * The class provides a wrapper to bytedeco.org JavaCpp-Platform.
 * "Bytedeco makes native libraries available to the Java platform
 * by offering ready-to-use bindings generated with the co-developed
 * JavaCPP technology."
 * <p>
 * Of particular interest is the platform support for FFmpeg,
 * specifically avutils which is used to demux the MPEG-TS streams into h264 video packets.
 * <p>
 * The MpegTsProcessor allows for easy interface and management of the logical stream,
 * allowing client applications to register callbacks for video and data buffers for further domain-specific processing.
 * <p>
 * <H1>Example Usage:</H1>
 * <pre><code>
 * MpegTsProcessor mpegTsProcessor = new MpegTsProcessor("src/test/resources/Flight_20190327_018.ts");
 * mpegTsProcessor.openStream();
 * mpegTsProcessor.queryEmbeddedStreams();
 * if (mpegTsProcessor.hasVideoStream()) {
 *      mpegTsProcessor.setVideoDataBufferListener(new DataBufferListener() {
 *          public void onDataBuffer(byte[] dataBuffer) {
 *                 ...
 *              }
 *          });
 * }
 * mpegTsProcessor.processStream();
 * </code></pre>
 * ...
 * <pre><code>
 * mpegTsProcessor.stopProcessingStream();
 * try {
 *      mpegTsProcessor.join();
 * } catch (InterruptedException e) {
 *      e.printStackTrace();
 * }
 * mpegTsProcessor.closeStream();
 * </code></pre>
 *
 * @author Nick Garay
 * @since Feb. 6, 2020
 */
public class MpegTsProcessor extends Thread {
    /**
     * Logging utility.
     */
    private static final Logger logger = LoggerFactory.getLogger(MpegTsProcessor.class);

    /**
     * Name of thread.
     */
    private static final String WORKER_THREAD_NAME = "STREAM-PROCESSOR";

    private final StreamContext videoStreamContext = new StreamContext();
    private final StreamContext audioStreamContext = new StreamContext();
    private final StreamContext dataStreamContext = new StreamContext();

    /**
     * Context used by underlying FFmpeg library to decode stream.
     */
    private AVFormatContext avFormatContext;

    /**
     * Flag indicating if processing of the transport stream should be terminated.
     */
    private final AtomicBoolean terminateProcessing = new AtomicBoolean(false);

    /**
     * Flag indicating whether the stream has been opened or connected successfully.
     */
    private boolean streamOpened = false;

    /**
     * A string representation of the file or url to use as the source of the transport stream to demux.
     */
    private final String streamSource;

    /**
     * FPS to enforce when playing back from a file.
     * Zero means the file will be played back as fast as possible.
     */
    int fps;

    /**
     * If true, play the video file continuously in a loop.
     */
    volatile boolean loop;

    /**
     * Executor to restart the stream in case of failure.
     */
    protected ScheduledExecutorService restartExecutor;

    /**
     * Constructor
     *
     * @param source A string representation of the file or url to use as the source of the transport stream to demux.
     */
    public MpegTsProcessor(String source) {
        this(source, 0, false);
    }

    /**
     * Constructor with more options when playing back from a file.
     *
     * @param source A string representation of the file or url to use as the source of the transport stream to demux.
     * @param fps    The desired playback FPS (use 0 for decoding the TS file as fast as possible).
     * @param loop   If true, play the video file continuously in a loop.
     */
    public MpegTsProcessor(String source, int fps, boolean loop) {
        super(WORKER_THREAD_NAME);

        this.streamSource = source;
        this.fps = fps;
        this.loop = loop;
    }

    /**
     * Attempts to open the stream given the {@link MpegTsProcessor#streamSource}.
     * Opening the stream entails establishing appropriate connection and ability to extract stream information.
     *
     * @return True if the stream is opened successfully, false otherwise.
     */
    public boolean openStream() {
        logger.debug("openStream");

        avformat.avformat_network_init();

        // Create a new AV Format Context for I/O
        avFormatContext = new AVFormatContext(null);

        // Set timeout
        var options = new AVDictionary(null);
        avutil.av_dict_set(options, "timeout", "3000000", 0);

        int returnCode = avformat.avformat_open_input(avFormatContext, streamSource, null, options);
        logger.debug("returnCode: {}", returnCode);

        // Attempt to open the stream, streamPath can be a file or URL
        if (returnCode == 0) {
            returnCode = avformat.avformat_find_stream_info(avFormatContext, (PointerPointer<?>) null);

            if (returnCode < 0) {
                logger.error("Failed to find stream info");
            } else {
                streamOpened = true;
                queryEmbeddedStreams();

                // Allocate the codec contexts and attempt to open them
                videoStreamContext.openCodecContext(avFormatContext);
                audioStreamContext.openCodecContext(avFormatContext);

                logger.debug("Stream opened {}", streamSource);
            }
        } else {
            logger.error("Failed to open stream: {}", streamSource);
        }

        return streamOpened;
    }

    public boolean isStreamOpened() {
        return streamOpened;
    }

    /**
     * Required to identify if the transport stream contains a video stream and/or an audio stream.
     * Invoked after {@link MpegTsProcessor#openStream()}.
     */
    private void queryEmbeddedStreams() {
        for (int streamId = 0; streamId < avFormatContext.nb_streams(); ++streamId) {
            int codecType = avFormatContext.streams(streamId).codecpar().codec_type();

            AVRational timeBase = avFormatContext.streams(streamId).time_base();
            double timeBaseUnits = (double) timeBase.num() / timeBase.den();

            if (!videoStreamContext.hasStream() && codecType == avutil.AVMEDIA_TYPE_VIDEO) {
                logger.debug("Video stream present with id: {}", streamId);

                videoStreamContext.setStreamId(streamId);
                videoStreamContext.setStreamTimeBase(timeBaseUnits);
            } else if (!audioStreamContext.hasStream() && codecType == avutil.AVMEDIA_TYPE_AUDIO) {
                logger.debug("Audio stream present with id: {}", streamId);

                audioStreamContext.setStreamId(streamId);
                audioStreamContext.setStreamTimeBase(timeBaseUnits);
            } else if (!dataStreamContext.hasStream() && codecType == avutil.AVMEDIA_TYPE_DATA) {
                logger.debug("Data stream present with id: {}", streamId);

                dataStreamContext.setStreamId(streamId);
                dataStreamContext.setStreamTimeBase(timeBaseUnits);
            }
        }
    }

    /**
     * Required to identify if the transport stream contains a video stream.
     * Should be used in conjunction with {@link MpegTsProcessor#setVideoDataBufferListener(DataBufferListener)}
     * to register callbacks for appropriate buffers.
     */
    public boolean hasVideoStream() {
        return videoStreamContext.hasStream();
    }

    /**
     * Required to identify if the transport stream contains an audio stream.
     * Should be used in conjunction with {@link MpegTsProcessor#setAudioDataBufferListener(DataBufferListener)}
     * to register callbacks for appropriate buffers.
     */
    public boolean hasAudioStream() {
        return audioStreamContext.hasStream();
    }

    /**
     * Required to identify if the transport stream contains a data stream.
     * Should be used in conjunction with {@link MpegTsProcessor#setDataDataBufferListener(DataBufferListener)}
     * to register callbacks for appropriate buffers.
     */
    public boolean hasDataStream() {
        return dataStreamContext.hasStream();
    }

    /**
     * Retrieves the average frame rate for the embedded video if there is one
     * Should be invoked after {@link MpegTsProcessor#hasVideoStream()}
     * to retrieve the video average frame rate.
     *
     * @return The average frame rate for the video.
     * @throws IllegalStateException if there is no video stream embedded
     */
    public double getVideoStreamAvgFrameRate() throws IllegalStateException {
        if (!videoStreamContext.hasStream()) {
            throw new IllegalStateException("Stream does not contain video frames");
        }

        AVRational rational = avFormatContext.streams(videoStreamContext.getStreamId()).avg_frame_rate();
        return (double) rational.num() / rational.den();
    }

    /**
     * Required to identify the width and height of the video frames.
     * Should be invoked after {@link MpegTsProcessor#hasVideoStream()} to retrieve the video frame dimensions.
     *
     * @return An int[] where index 0 is the width and index 1 is the height of the frames.
     * @throws IllegalStateException If there is no video stream embedded.
     */
    public int[] getVideoStreamFrameDimensions() {
        final int WIDTH_IDX = 0;
        final int HEIGHT_IDX = 1;

        logger.debug("getVideoStreamFrameDimensions");

        if (!videoStreamContext.hasStream()) {
            throw new IllegalStateException("Stream does not contain video frames");
        }

        AVCodecParameters codecParameters = avFormatContext.streams(videoStreamContext.getStreamId()).codecpar();
        int[] dimensions = {codecParameters.width(), codecParameters.height()};

        logger.debug("Frame [width, height] = [ {}, {} ]", dimensions[WIDTH_IDX], dimensions[HEIGHT_IDX]);

        return dimensions;
    }

    /**
     * Retrieves the sample rate for the embedded audio.
     * Should be invoked after {@link MpegTsProcessor#hasAudioStream()} to retrieve the audio sample rate.
     *
     * @return The audio sample rate.
     * @throws IllegalStateException If there is no audio stream embedded.
     */
    public int getAudioSampleRate() {
        if (!audioStreamContext.hasStream()) {
            throw new IllegalStateException("Stream does not contain audio data");
        }

        int sampleRate = avFormatContext.streams(audioStreamContext.getStreamId()).codecpar().sample_rate();

        logger.debug("Audio sample rate: {}", sampleRate);
        return sampleRate;
    }

    /**
     * Registers a video buffer listener to call if clients are interested in demuxed video buffers
     *
     * @param videoDataBufferListener The listener to invoke when a video buffer is retrieved from
     *                                the transport stream.
     */
    public void setVideoDataBufferListener(@Nonnull DataBufferListener videoDataBufferListener) {
        videoStreamContext.setDataBufferListener(videoDataBufferListener);
    }

    /**
     * Registers an audio buffer listener to call if clients are interested in demuxed audio buffers
     *
     * @param audioDataBufferListener The listener to invoke when an audio buffer is retrieved from
     *                                the transport stream.
     */
    public void setAudioDataBufferListener(@Nonnull DataBufferListener audioDataBufferListener) {
        audioStreamContext.setDataBufferListener(audioDataBufferListener);
    }

    public void setInjectVideoExtradata(boolean injectVideoExtradata) {
        videoStreamContext.setInjectingExtradata(injectVideoExtradata);
    }

    /**
     * Registers a data buffer listener to call if clients are interested in demuxed data buffers
     *
     * @param dataDataBufferListener The listener to invoke when a data buffer is retrieved from
     *                               the transport stream.
     */
    public void setDataDataBufferListener(@Nonnull DataBufferListener dataDataBufferListener) {
        dataStreamContext.setDataBufferListener(dataDataBufferListener);
    }

    /**
     * Retrieves the codec name for the video stream.
     * Should be invoked after {@link MpegTsProcessor#hasVideoStream()} to retrieve the video codec name.
     *
     * @return The codec name for the video stream.
     */
    public String getVideoCodecName() {
        return videoStreamContext.getCodecName();
    }

    /**
     * Retrieves the codec name for the audio stream.
     * Should be invoked after {@link MpegTsProcessor#hasAudioStream()} to retrieve the audio codec name.
     *
     * @return The codec name for the audio stream.
     */
    public String getAudioCodecName() {
        return audioStreamContext.getCodecName();
    }

    /**
     * Starts the threaded process for demuxing the transport stream.
     * Should only be invoked if the stream is successfully opened.
     * <p>
     * Call this method instead of invoking {@link MpegTsProcessor#start()} directly,
     * as this method will ensure the codec context is set up for use with the transport stream.
     *
     * @throws IllegalStateException If the stream has not been opened or failed to open.
     */
    public void processStream() throws IllegalStateException {
        logger.debug("processStream");

        if (streamOpened) {
            start();
        } else {
            throw new IllegalStateException("Stream has not been opened or failed to open");
        }
    }

    @Override
    public void run() {
        if (fps > 0) {
            // Use a scheduled executor to enforce the FPS
            ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
            long initialDelay = 0;
            long period = fps > 0 ? (1000 / fps) : 0;

            executor.scheduleAtFixedRate(() -> {
                if (terminateProcessing.get()) {
                    executor.shutdown();
                } else {
                    processPacket();
                }
            }, initialDelay, period, TimeUnit.MILLISECONDS);
        } else {
            // Process packets as fast as possible
            while (!terminateProcessing.get()) {
                processPacket();
            }
        }
    }

    /**
     * Stream processing where the demuxing is invoked on underlying FFmpeg libraries and callbacks,
     * if registered, are invoked for appropriate buffers.
     */
    private void processPacket() {
        if (!streamOpened) return;

        AVPacket avPacket = new AVPacket();

        int ret = av_read_frame(avFormatContext, avPacket);

        if (ret < 0) {
            if (loop) {
                avformat.av_seek_frame(avFormatContext, 0, 0, avformat.AVSEEK_FLAG_ANY);
            } else {
                closeStream();
            }
        } else {
            videoStreamContext.processPacket(avPacket);
            audioStreamContext.processPacket(avPacket);
            dataStreamContext.processPacket(avPacket);
        }

        // Fully deallocate packet
        avcodec.av_packet_unref(avPacket);
        avPacket.deallocate();
    }

    /**
     * Closes the transport stream, releasing allocated resources including the codec context.
     */
    public void closeStream() {
        logger.debug("closeStream");

        if (streamOpened) {
            videoStreamContext.closeCodecContext();
            audioStreamContext.closeCodecContext();

            if (avFormatContext != null) {
                avformat.avformat_close_input(avFormatContext);
            }

            streamOpened = false;
        }

        if (restartExecutor != null) {
            restartExecutor.shutdown();
        }
    }

    /**
     * Indicate to processor to stop processing packets from the stream.
     */
    public void stopProcessingStream() {
        logger.debug("stopProcessingStream");

        if (streamOpened) {
            loop = false;
            terminateProcessing.set(true);
        }
    }

    /**
     * Set the flag to attempt reconnection to the stream in case of failure.
     * When set to true, the processor will attempt to reconnect to the stream if the connection is lost.
     *
     * @param reconnect True to attempt reconnection, false otherwise.
     */
    public void setReconnect(boolean reconnect) {
        if (reconnect) {
            restartExecutor = Executors.newScheduledThreadPool(1);
            restartExecutor.scheduleAtFixedRate(() -> {
                if (!streamOpened) {
                    logger.debug("Attempting to reconnect to stream.");
                    openStream();
                }
            }, 5, 5, TimeUnit.SECONDS);
        } else {
            if (restartExecutor != null) {
                restartExecutor.shutdown();
            }
        }
    }
}
