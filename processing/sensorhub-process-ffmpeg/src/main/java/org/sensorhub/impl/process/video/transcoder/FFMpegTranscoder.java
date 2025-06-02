/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.process.video.transcoder;

import net.opengis.swe.v20.*;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.swscale.SwsContext;
import org.bytedeco.javacpp.BytePointer;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.DataBlockByte;
import org.vast.data.DataBlockCompressed;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.RasterHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avutil.*;
import static org.bytedeco.ffmpeg.global.swscale.*;


/**
 * <p>
 * Implementation of video decoder based on FFMPEG
 * </p>
 *
 * @author Alex Robin
 * @date Jun 1, 2021
 */
// TODO: Needs serious restructuring, since two codecs cannot be opened at once. Need to be able to switch (deallocate and reallocate) contexts. Should be handled by writing some new methods. (Or maybe classes would be better?)
// TODO: Completely overhaul the structure. Simplify the execute method by breaking up each step into a separate method.
// TODO: Take the init code and separate into methods appropriately. These can then be reused when reallocating contexts when switching between encoding and decoding.
// TODO: This was originally FFMpegDecoder, but renamed to FFmpegTranscoder and given transcoding functionality. Need to change tests to include the new out codec parameter.
public class FFMpegTranscoder extends ExecutableProcessImpl
{
    private static final Logger logger = LoggerFactory.getLogger(FFMpegTranscoder.class);

	public static final OSHProcessInfo INFO = new OSHProcessInfo("video:FFMpegTranscoder", "FFMPEG Video Transcoder", null, FFMpegTranscoder.class);

	enum CodecEnum {
        //AUTO("auto"),
        H264("h264"),
        H265("hevc"),
        MJPEG("mjpeg"),
        VP8("vp8"),
        VP9("vp9"),
        MPEG2("mpeg2video"),
        RGB("rgb24"),
        YUV("yuv420p");

	    String ffmpegName;

	    CodecEnum(String ffmpegName)
	    {
	        this.ffmpegName = ffmpegName;
	    }
	}


    Time inputTimeStamp;
    Count inputWidth, inputHeight;
    DataArray imgIn;
    Time outputTimeStamp;
    Count outputWidth, outputHeight;
    DataArray imgOut;
    Text inCodecParam;
    Text outCodecParam;
    Count decimFactorParam;

    Encoder encoder = null;
    Decoder decoder = null;
    Thread decoderThread = null;
    Thread encoderThread = null;
    List<Thread> processThreads = null;

    //AVCodec decoder = null;
    //AVCodecContext decode_ctx = null;
    //AVCodec encoder = null;
    //AVCodecContext encode_ctx = null;
    SwsContext sws_ctx = null;
    AVFrame av_frame = null;
    AVFrame sws_frame = null;
    AVPacket dec_pkt = null;
    AVPacket enc_pkt = null;
    BytePointer nativeFrameData;
    int frameCounter = 0;
    int decimFactor = 1;
    boolean publish;
    CodecEnum inCodec;
    CodecEnum outCodec;
    Runnable inputProcess;


    public FFMpegTranscoder()
    {
    	super(INFO);
        RasterHelper swe = new RasterHelper();

        // inputs
        inputData.add("codedFrame", swe.createRecord()
                .label("Video Frame")
                .addField("time", inputTimeStamp = swe.createTime()
                        .asSamplingTimeIsoUTC()
                        .build())
                .addField("width", inputWidth = swe.createCount()
                        .id("IN_WIDTH")
                        .label("Input Frame Width")
                        .build())
                .addField("height", inputHeight = swe.createCount()
                        .id("IN_HEIGHT")
                        .label("Input Frame Height")
                        .build())
                .addField("img", imgIn = swe.newRgbImage(
                        inputWidth,
                        inputHeight,
                        DataType.BYTE))
                .build());

        // parameters
        paramData.add("inCodec", inCodecParam = swe.createText()
                .definition(SWEHelper.getPropertyUri("Codec"))
                .label("Input Codec Name")
                .addAllowedValues(FFMpegTranscoder.CodecEnum.class)
                .build());

        paramData.add("outCodec", outCodecParam = swe.createText()
                .definition(SWEHelper.getPropertyUri("Codec"))
                .label("Output Codec Name")
                .addAllowedValues(FFMpegTranscoder.CodecEnum.class)
                .build());

        paramData.add("decimFactor", decimFactorParam = swe.createCount()
                .definition(SWEConstants.DEF_COEF)
                .label("Decimation Factor")
                .description("Decimation factor of input frames. Only 1 frame every 'decimFactor' frames will be outputted")
                .build());

        // outputs
        outputData.add("outFrame", swe.createRecord()
                .label("Video Frame")
                .addField("time", outputTimeStamp = swe.createTime()
                        .asSamplingTimeIsoUTC()
                        .build())
                .addField("width", outputWidth = swe.createCount()
                        .id("OUT_WIDTH")
                        .label("Output Frame Width")
                        .build())
                .addField("height", outputHeight = swe.createCount()
                        .id("OUT_HEIGHT")
                        .label("Output Frame Height")
                        .build())
                .addField("img", imgOut = swe.newRgbImage(
                        outputWidth,
                        outputHeight,
                        DataType.BYTE))
                .build());
    }


    @Override
    public void notifyParamChange()
    {
        boolean outputSizeChanged = false;

        // we need to set output frame dimensions if it was set to a fixed value
        if (inputWidth.getData() != null && inputWidth.getData().getIntValue() > 0)
        {
            if (!outputWidth.hasData())
                outputWidth.getParent().assignNewDataBlock();
            outputWidth.getData().setIntValue(inputWidth.getData().getIntValue());
            outputSizeChanged = true;
        }

        if (inputHeight.getData() != null && inputHeight.getData().getIntValue() > 0)
        {
            if (!outputHeight.hasData())
                outputHeight.getParent().assignNewDataBlock();
            outputHeight.getData().setIntValue(inputHeight.getData().getIntValue());
            outputSizeChanged = true;
        }

        if (outputSizeChanged == true)
        {
            /*((DataArray)imgOut.getElementType()).updateSize(outputWidth.getData().getIntValue());
            imgOut.updateSize(outputHeight.getData().getIntValue());*/
            ((DataArray)imgOut).updateSize();
        }
    }


    @Override
    public void init() throws ProcessException
    {
        super.init();
        frameCounter = 0;
        if (processThreads != null) {
            processThreads.clear();
        } else {
            processThreads = new ArrayList<>();
        }

        // init decoder according to configured codec
        // TODO: Automatically detect input codec from compression in data struct?
        try
        {
            inCodec = FFMpegTranscoder.CodecEnum.valueOf(inCodecParam.getData().getStringValue());
            outCodec = FFMpegTranscoder.CodecEnum.valueOf(outCodecParam.getData().getStringValue());
            /*
            if (inCodec != CodecEnum.RGB) {
                decoder = new Decoder(inCodec.ffmpegName);
                decoderThread = new Thread(decoder);
                decoderThread.start();
            } else {
                decoder = null;
                decoderThread = null;
                logger.debug("No decoder needed for RGB input");
            }

            if (outCodec != CodecEnum.RGB) {
                encoder = new Encoder(outCodec.ffmpegName);

                encoderThread.start();
            } else {
                encoder = null;
                encoderThread = null;
                logger.debug("No encoder needed for RGB output");
            }

             */
            // TODO Make sure all possible combinations work
            // execute runs the input process
            // processThreads are always running, passing available data from decoder to encoder and encoder to output
            if (inCodec == CodecEnum.RGB) {
                if (outCodec == CodecEnum.RGB) {
                    inputProcess = null;
                } else {
                    // Encoder
                    // Input -> SWS Scale -> Encoder -> Output
                    encoder = new Encoder(outCodec.ffmpegName);
                    encoderThread = new Thread(encoder);
                    processThreads.add(new Thread(this::encoder_to_output));
                    inputProcess = this::input_to_encoder;
                }
            } else {
                if (outCodec == CodecEnum.RGB) {
                    // Decoder
                    // Input -> Decoder -> SWS Scale -> Output
                    decoder = new Decoder(inCodec.ffmpegName);
                    processThreads.add(new Thread(this::decoder_to_output));
                    inputProcess = this::input_to_decoder;
                } else {
                    // Transcoder
                    // Input -> Decoder -> SWS Scale -> Encoder -> Output
                    decoder = new Decoder(inCodec.ffmpegName);
                    encoder = new Encoder(outCodec.ffmpegName);
                    processThreads.add(new Thread(this::decoder_to_encoder));
                    processThreads.add(new Thread(this::encoder_to_output));
                    inputProcess = this::input_to_decoder;
                }
            }
            for (Thread thread : processThreads) {
                thread.start();
            }
            logger.debug("Using coder process: {}", inputProcess);
            // TODO Get rid of old code
            /*
            // init decoder context
            decoder = avcodec_find_decoder_by_name(inCodec.ffmpegName);
            decode_ctx = avcodec_alloc_context3(decoder);
            if (avcodec_open2(decode_ctx, decoder, (PointerPointer<?>)null) < 0) {
                throw new IllegalStateException("Error initializing " + inCodec + " decoder");
            }



            // init FFMPEG objects
            av_log_set_level(getLogger().isDebugEnabled() ? AV_LOG_INFO : AV_LOG_FATAL);

            */
            dec_pkt = av_packet_alloc();
            if (encoder != null) {
                enc_pkt = av_packet_alloc();
            } else {
                enc_pkt = null;
            }
            av_init_packet(dec_pkt);
            if (enc_pkt != null) {
                av_init_packet(enc_pkt);
            }
            av_frame = av_frame_alloc();
            sws_frame = av_frame_alloc();
            nativeFrameData = new BytePointer((long)50*1024);
        }
        catch (IllegalArgumentException e)
        {
            reportError("Unsupported codec. Must be one of " + Arrays.toString(FFMpegTranscoder.CodecEnum.values()));
        }

        // set decimation factor
        decimFactor = decimFactorParam.getData().getIntValue();
        if (decimFactor < 0)
            throw new ProcessException("Decimation factor must be > 0. Current value is " + decimFactor);
        if (decimFactor == 0)
            decimFactor = 1;
    }


    /*
     * To init scaler once we know the input frame size
     */
    protected void initScaler(AVFrame av_frame)
    {
        int frameWidth = av_frame.width();
        int frameHeight = av_frame.height();

        // init scaler
        sws_frame.format(AV_PIX_FMT_RGB24);
        sws_frame.width(frameWidth);
        sws_frame.height(frameHeight);
        av_image_alloc(sws_frame.data(), sws_frame.linesize(),
                frameWidth, frameHeight, AV_PIX_FMT_RGB24, 1);

        sws_ctx = sws_getContext(frameWidth, frameHeight, AV_PIX_FMT_YUV420P,
                frameWidth, frameHeight, AV_PIX_FMT_RGB24, SWS_BICUBIC, null, null, (double[])null);

        getLogger().debug("Resizing {}x{} -> {}x{}", av_frame.width(), av_frame.height(), frameWidth, frameHeight);
    }

    private void decoder_to_encoder() {
        while (!Thread.currentThread().isInterrupted()) {
            for (AVFrame frame : decoder.getPackets()) {
                swsScale(frame);
                encoder.addPacket(frame);
            }
        }
    }

    private void input_to_decoder() {
        // get input encoded frame data
        byte[] frameData = ((DataBlockCompressed)imgIn.getData()).getUnderlyingObject();
        //System.out.println("Frame size=" + frameData.length);

        // grow packet data buffer as needed
        if (nativeFrameData.capacity() < frameData.length)
        {
            nativeFrameData.deallocate();
            nativeFrameData = new BytePointer(Math.max(frameData.length, nativeFrameData.capacity()*2));
        }
        nativeFrameData.position(0);
        nativeFrameData.limit(0);
        nativeFrameData.put(frameData);

        // decode frame
        dec_pkt.data(nativeFrameData);
        dec_pkt.size(frameData.length);

        decoder.addPacket(dec_pkt);
    }

    private void input_to_encoder() {
        // get input encoded frame data
        byte[] frameData = ((DataBlockCompressed)imgIn.getData()).getUnderlyingObject();
        //System.out.println("Frame size=" + frameData.length);

        // grow packet data buffer as needed
        if (nativeFrameData.capacity() < frameData.length)
        {
            nativeFrameData.deallocate();
            nativeFrameData = new BytePointer(Math.max(frameData.length, nativeFrameData.capacity()*2));
        }
        nativeFrameData.position(0);
        nativeFrameData.limit(0);
        nativeFrameData.put(frameData);

        if (inCodec == CodecEnum.RGB) {
            av_frame.data(0, nativeFrameData);
            av_frame.format(AV_PIX_FMT_RGB24);
        } else {
            // TODO: reorganize this entire method to support YUV. Need 3 separate planes instead of 1.
        }

        // Scale frame
        swsScale(av_frame);

        // Send frame to encoder
        encoder.addPacket(av_frame);
    }



    private void encoder_to_output() {
        while (!Thread.currentThread().isInterrupted()) {
            for (AVPacket packet : encoder.getPackets()) {
                byte[] frameData = new byte[packet.size()];
                packet.data().get(frameData);
                ((DataBlockByte) imgOut.getData()).setUnderlyingObject(frameData);
                // also copy frame timestamp
                var ts = inputTimeStamp.getData().getDoubleValue();
                outputTimeStamp.getData().setDoubleValue(ts);
                try {
                    super.publishData();
                } catch (InterruptedException e) {
                    logger.error("Error publishing output packet", e);
                }
            }
        }
    }

    private void decoder_to_output() {
        while (!Thread.currentThread().isInterrupted()) {
            for (AVFrame frame : decoder.getPackets()) {
                swsScale(frame);
                byte[] frameData = new byte[frame.width() * frame.height() * 3];
                frame.data(0).get(frameData);
                //frame.data().get(frameData);
                ((DataBlockByte) imgOut.getData()).setUnderlyingObject(frameData);
                // also copy frame timestamp
                var ts = inputTimeStamp.getData().getDoubleValue();
                outputTimeStamp.getData().setDoubleValue(ts);
                try {
                    super.publishData();
                } catch (InterruptedException e) {
                    logger.error("Error publishing output frame", e);
                }
            }
        }
    }

    /**
     * Takes an uncompressed frame and converts it to RGB, changing the size to that specified in the parameters.
     * @param frame
     */
    private void swsScale(AVFrame frame) {
        if (sws_ctx == null )
            initScaler(frame);

        sws_scale(sws_ctx, frame.data(), frame.linesize(), 0, frame.height(), sws_frame.data(), sws_frame.linesize());

        // Replace the data in the input frame with the scaled frame
        frame.data().deallocate(); // This line may cause issues
        frame.data(0, sws_frame.data(0));
        frame.linesize(0, sws_frame.linesize(0));
        frame.width(sws_frame.width());
        frame.height(sws_frame.height());
        frame.format(AV_PIX_FMT_RGB24);
    }

    @Override
    public void execute() throws ProcessException
    {
        // Skip every decimFactor'th frame
        if (frameCounter++ % decimFactor == 0) {
            return;
        }


        // Start process. Encoding, decoding, or transcoding (or nothing)
        if (inputProcess != null) {
            publish = false;
            inputProcess.run();
        } else {
            ((DataBlockByte) imgOut.getData()).setUnderlyingObject(((DataBlockCompressed)imgIn.getData()).getUnderlyingObject());
            publish = true;
            return;
        }
        // TODO All code below should be moved elsewhere or removed entirely


        //decoder.addPacket(dec_pkt);

        /*
        avcodec_send_packet(decode_ctx, dec_pkt);
        // Receive the packet to be re-encoded (or output as-is)
        int ret2 = avcodec_receive_frame(decode_ctx, av_frame);
        av_packet_unref(dec_pkt);
        */


        //System.out.printf("decode: ret1 %d ret2 %d\n", ret1, ret2);
/*
        if (true)
        {
            // init scaler once we decode the 1st frame
            if (sws_ctx == null )
                initScaler(av_frame);


                publish = false;
                return;
            }
            publish = true;

            // apply scaler (needed to convert from YUV to RGB)
            //sws_scale(sws_ctx, av_frame.data(), av_frame.linesize(), 0, av_frame.height(), sws_frame.data(), sws_frame.linesize());

            // write decoded data to output
            // TODO use special datablock to encapsulate native buffer
            // would be more efficient when passing frame to other native libraries (e.g. OpenCV)

            frameData = new byte[av_frame.width() * av_frame.height() * 3];
            sws_frame.data(0).get(frameData);

            // Re-encode if the output codec is set (not RGB)
            if (outCodec != CodecEnum.RGB) {

                var outCodec = FFMpegTranscoder.CodecEnum.valueOf(outCodecParam.getData().getStringValue());

                // init encoder context
                if (outCodec != FFMpegTranscoder.CodecEnum.RGB) {
                    // Account for h264 encoder name
                    // TODO: Separate encoder enum (if there's more than just h264 with this issue) (or use codec id)
                    var codec = outCodec == FFMpegTranscoder.CodecEnum.H264 ? "libx264" : outCodec.ffmpegName;
                    encoder = avcodec_find_encoder_by_name(codec);
                    encode_ctx = avcodec_alloc_context3(encoder);

                    byte[] errorBuf = new byte[64];
                    av_strerror(-22, errorBuf, 64);
                    logger.debug("Error: " + new String(errorBuf));

                    if (avcodec_open2(encode_ctx, encoder, (PointerPointer<?>) null) < 0) {
                        throw new IllegalStateException("Error initializing " + codec + " encoder");
                    }
                } else {
                    encoder = null;
                    encode_ctx = null;
                }


                avcodec_send_frame(encode_ctx, sws_frame);
                avcodec_receive_packet(encode_ctx, enc_pkt);
                enc_pkt.data().get(frameData);
                ((DataBlockByte) imgOut.getData()).setUnderlyingObject(frameData);
            } else {
                ((DataBlockByte) imgOut.getData()).setUnderlyingObject(frameData);
            }



            publish = true;

        }
        */
    }


    @Override
    protected void publishData() throws InterruptedException
    {
        if (publish)
            super.publishData();
    }

    private void disposeCoder(Thread coderThread, Coder coder) {
        if (coderThread != null && coderThread.isAlive() && coder != null) {
            coder.doRun.set(false);
            try {
                coderThread.join();
            } catch (InterruptedException e) {
                logger.error("Error waiting for encoder thread to finish", e);
            }
            coderThread = null;
            coder = null;
        }
    }

    @Override
    public void dispose()
    {
        super.dispose();

        if (nativeFrameData != null) {
            nativeFrameData.deallocate();
            nativeFrameData = null;
        }

        disposeCoder(encoderThread, encoder);
        disposeCoder(decoderThread, decoder);

        if (dec_pkt != null) {
            av_packet_free(dec_pkt);
        }

        if (enc_pkt != null) {
            av_packet_free(enc_pkt);
        }

        if (av_frame != null) {
            av_frame_free(av_frame);
            av_frame = null;
        }
        
        if (sws_ctx != null) {
            sws_freeContext(sws_ctx);
            sws_ctx = null;
        }
        if (sws_frame != null) {
            av_frame_free(sws_frame);
            sws_frame = null;
        }
    }
}