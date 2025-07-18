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
import org.bytedeco.javacpp.DoublePointer;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.DataBlockCompressed;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.RasterHelper;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

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

	public enum CodecEnum {
        //AUTO("auto"),
        H264(AV_CODEC_ID_H264),
        H265(AV_CODEC_ID_H265),
        MJPEG(AV_CODEC_ID_MJPEG),
        VP8(AV_CODEC_ID_VP8),
        VP9(AV_CODEC_ID_VP9),
        MPEG2(AV_CODEC_ID_MPEG2TS), // Not 100% sure if this one is correct
        MPEG4(AV_CODEC_ID_MPEG4),
        RGB(AV_PIX_FMT_RGB24),  // Keeping the uncompressed formats in this enum; shouldn't cause problems
        YUV(AV_PIX_FMT_YUV420P);

	    int ffmpegId;

	    CodecEnum(int ffmpegId)
	    {
	        this.ffmpegId = ffmpegId;
	    }
	}

    AtomicBoolean doRun = new AtomicBoolean(true);
    AtomicBoolean isRunning = new AtomicBoolean(false);
    Time inputTimeStamp;
    Count inputWidth, inputHeight;
    DataArray imgIn;
    Time outputTimeStamp;
    Count outputWidth, outputHeight, inputFps, inputBitrate;
    DataArray imgOut;
    Text inCodecParam;
    Text outCodecParam;
    Count decimFactorParam;

    Encoder encoder = null;
    Decoder decoder = null;
    List<Thread> processThreads = null;

    HashMap<String, String> decOptions = new HashMap<>();
    HashMap<String, String> encOptions = new HashMap<>();

    SwsContext sws_ctx = null;
    AVFrame av_frame = null;
    AVFrame sws_frame = null;
    AVPacket dec_pkt = null;
    AVPacket enc_pkt = null;
    BytePointer nativeFrameData;
    BytePointer planeY;
    BytePointer planeU;
    BytePointer planeV;
    int frameCounter = 0;
    int decimFactor = 1;
    boolean publish;
    boolean validInputSize = false;
    boolean validOutputSize = false;
    CodecEnum inCodec;
    CodecEnum outCodec;
    Runnable inputProcess;
    int swsPixFmt = AV_PIX_FMT_YUV420P;
    RasterHelper swe = new RasterHelper();

    int width, height, outWidth, outHeight;

    public FFMpegTranscoder()
    {
    	super(INFO);
        RasterHelper swe = new RasterHelper();



        // inputs
        inputData.add("codedFrame", swe.createRecord()
                .name("inFrame")
                .label("Input Video Frame")
                .description("")
                .definition(SWEHelper.getPropertyUri("VideoFrame"))
                .addField("time", inputTimeStamp = swe.createTime()
                        .asSamplingTimeIsoUTC()
                        .build())
                .addField("width", inputWidth = swe.createCount()
                        .id("width")
                        .label("Input Frame Width")
                        .build())
                .addField("height", inputHeight = swe.createCount()
                        .id("height")
                        .label("Input Frame Height")
                        .build())
                .addField("sampleTime", inputTimeStamp = swe.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Sample Time")
                        .description("Time of data collection").build())
                .addField("img", imgIn = swe.newRgbImage(inputWidth, inputHeight, DataType.BYTE))
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

        // outputs
        // Input image will be scaled to match output size
        outputData.add("outFrame", swe.createRecord()
                .name("outFrame")
                .label("Output Video Frame")
                .description("")
                .definition(SWEHelper.getPropertyUri("VideoFrame"))
                .addField("sampleTime", outputTimeStamp =  swe.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Sample Time")
                        .description("Time of data collection").build())
                .addField("outWidth", outputWidth = swe.createCount()
                        .id("outWidth")
                        .label("Output Frame Width")
                        .build())
                .addField("outHeight", outputHeight = swe.createCount()
                        .id("outHeight")
                        .label("Output Frame Height")
                        .build())
                .addField("img", imgOut = swe.newRgbImage(outputWidth, outputHeight, DataType.BYTE))
                .build());


    }

    private void initProcessThreads() {
        try {
            stopProcessThreads();
        } catch (Exception e){
            logger.error("Transcoder could not stop process threads during re-init.", e);
        }
        processThreads.clear();
        inputProcess = null;
        encoder = null;
        decoder = null;

        if (inCodec == CodecEnum.RGB || inCodec == CodecEnum.YUV) {
            if (outCodec == CodecEnum.RGB || outCodec == CodecEnum.YUV) {
                inputProcess = this::input_to_output;
            } else {
                // Encoder
                // Input -> SW Scale -> Encoder -> Output
                encoder = new Encoder(outCodec.ffmpegId, encOptions);
                //encoderThread = new Thread(encoder);
                processThreads.add(new Thread(this::encoder_to_output));
                processThreads.add(new Thread(encoder));
                inputProcess = this::input_to_encoder;
            }
        } else {
            if (outCodec == CodecEnum.RGB || outCodec == CodecEnum.YUV) {
                // Decoder
                // Input -> Decoder -> SWS Scale -> Output
                decoder = new Decoder(inCodec.ffmpegId, decOptions);
                //decoderThread = new Thread(decoder);
                processThreads.add(new Thread(this::decoder_to_output));
                processThreads.add(new Thread(decoder));
                inputProcess = this::input_to_decoder;
            } else {
                // Transcoder
                // Input -> Decoder -> SWS Scale -> Encoder -> Output
                decoder = new Decoder(inCodec.ffmpegId, decOptions);
                encoder = new Encoder(outCodec.ffmpegId, encOptions);
                processThreads.add(new Thread(this::decoder_to_encoder));
                processThreads.add(new Thread(this::encoder_to_output));
                processThreads.add(new Thread(decoder));
                processThreads.add(new Thread(encoder));
                inputProcess = this::input_to_decoder;
            }
        }
    }

    private void startProcessThreads() {
        doRun.set(true);
        if (processThreads == null || processThreads.isEmpty() || processThreads.get(0).getState() != Thread.State.NEW) {
            initProcessThreads();
        }

        for (Thread thread : processThreads) {
            thread.start();
        }
        if (encoder != null) {
            encoder.doRun.set(true);
        }
        if (decoder != null) {
            decoder.doRun.set(true);
        }
        isRunning.set(true);
    }

    private void stopProcessThreads() throws InterruptedException {
        doRun.set(false); //TODO These atomic booleans may be entirely unnecessary, remove
        if (processThreads != null) {
            for (Thread thread : processThreads) {
                thread.interrupt();
                thread.join();
            }
        }
        isRunning.set(false);
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
    public void stop() {
        if (isRunning.get()) {
            try {
                stopProcessThreads();
            } catch (InterruptedException e) {
                logger.warn("Interrupted while stopping process threads");
            }
        }
        super.stop();
    }

    private void setImgEncoding() {
        var dataEncIn = swe.newBinaryEncoding(ByteOrder.BIG_ENDIAN, ByteEncoding.RAW);

        BinaryComponent timeEncIn = swe.newBinaryComponent();
        timeEncIn.setRef("/" + ((DataRecord)inputData.get(0)).getComponent(0).getName());
        timeEncIn.setCdmDataType(DataType.DOUBLE);
        dataEncIn.addMemberAsComponent(timeEncIn);

        var compressedBlockIn = swe.newBinaryBlock();
        compressedBlockIn.setRef("/" + ((DataRecord)inputData.get(0)).getComponent("img").getName());
        compressedBlockIn.setCompression(inCodecParam.getData().getStringValue().toUpperCase());
        dataEncIn.addMemberAsBlock(compressedBlockIn);
        try {
            SWEHelper.assignBinaryEncoding((DataRecord) inputData.get(0), dataEncIn);
        } catch (Exception e) {
            logger.warn("Invalid encoding");
        }
        imgIn.setEncoding(dataEncIn);

        var dataEncOut = swe.newBinaryEncoding(ByteOrder.BIG_ENDIAN, ByteEncoding.RAW);

        BinaryComponent timeEnc = swe.newBinaryComponent();
        timeEnc.setRef("/" + ((DataRecord)outputData.get(0)).getComponent(0).getName());
        timeEnc.setCdmDataType(DataType.DOUBLE);
        dataEncOut.addMemberAsComponent(timeEnc);

        var compressedBlock = swe.newBinaryBlock();
        compressedBlock.setRef("/" + ((DataRecord)outputData.get(0)).getComponent("img").getName());
        compressedBlock.setCompression(outCodecParam.getData().getStringValue().toUpperCase());
        dataEncOut.addMemberAsBlock(compressedBlock);
        try {
            SWEHelper.assignBinaryEncoding((DataRecord) outputData.get(0), dataEncOut);
        } catch (Exception e) {
            logger.warn("Invalid encoding");
        }
        imgOut.setEncoding(dataEncOut);
    }

    @Override
    public void init() throws ProcessException
    {
        doRun.set(true);
        isRunning.set(false);

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
            inCodec = FFMpegTranscoder.CodecEnum.valueOf(inCodecParam.getData().getStringValue().toUpperCase());
            outCodec = FFMpegTranscoder.CodecEnum.valueOf(outCodecParam.getData().getStringValue().toUpperCase());

            setImgEncoding();

            // Set options for the codecs
            // May add more, so using hashmap
            // May need two hashmaps. For fps, need to work around the decimation factor (or just remove it)
            decOptions = new HashMap<>();
            encOptions = new HashMap<>();

            //DataComponent temp;
            int fps = safeGetCountVal(inputFps);
            if (fps > 0) {
                decOptions.put("fps", String.valueOf(fps));
                encOptions.put("fps", String.valueOf(fps));
            }
            decOptions.put("pix_fmt", "yuv420p");
            if (outCodec == CodecEnum.RGB) {
                encOptions.put("pix_fmt", "rgb24");
            } else {
                encOptions.put("pix_fmt", "yuv420p");
            }

            int bitrate = safeGetCountVal(inputBitrate);
            if (bitrate > 0) {
                decOptions.put("bit_rate", String.valueOf(bitrate));
                encOptions.put("bit_rate", String.valueOf(bitrate)); // Just assuming input br is the same as out, could this change?
            }

            width = safeGetCountVal(inputWidth);
            if (width <= 0) {
                width = imgIn.getComponent("row").getComponentCount();
            }
            decOptions.put("width", String.valueOf(width));

            height = safeGetCountVal(inputHeight);
            if (height <= 0) {
                height = imgIn.getComponentCount();
            }
            decOptions.put("height", String.valueOf(height));


            outHeight = safeGetCountVal(outputHeight);
            if (outHeight <= 0) {
                try {
                    outHeight = imgOut.getComponentCount();
                } catch (Exception ignored) {
                    outHeight = 0;
                }
            }
            if (outHeight > 0) {
                encOptions.put("height", String.valueOf(outHeight));
            } else {
                encOptions.put("height", String.valueOf(height));
            }

            outWidth = safeGetCountVal(outputWidth);
            if (outWidth <= 0) {
                try {
                    outWidth = imgIn.getComponent("row").getComponentCount();
                } catch (Exception ignored) {
                    outWidth = 0;
                }
            }
            if (outWidth > 0) {
                encOptions.put("width", String.valueOf(outWidth));
            } else {
                encOptions.put("width", String.valueOf(width));
            }

            dec_pkt = av_packet_alloc();
            av_init_packet(dec_pkt);

            // Scale image if input and output dimensions are not equal or if converting between uncompressed formats
            if (height != outHeight || outWidth != outHeight || (isUncompressed(inCodec) && isUncompressed(outCodec) && inCodec != outCodec)) {
                int inFormat = inCodec == CodecEnum.RGB ? AV_PIX_FMT_RGB24 : AV_PIX_FMT_YUV420P;
                int outFormat = outCodec == CodecEnum.RGB ? AV_PIX_FMT_RGB24 : AV_PIX_FMT_YUV420P;
                sws_frame = av_frame_alloc();
                sws_frame.width(outWidth);
                sws_frame.height(outHeight);
                av_image_alloc(sws_frame.data(), sws_frame.linesize(),
                        outWidth, outHeight, outFormat, 1);
                sws_ctx = sws_getContext(width, height, inFormat, outWidth, outHeight, outFormat, SWS_BICUBIC, null, null, (DoublePointer) null);
                //sws_frame = av_frame_alloc();
            } else {
                sws_ctx = null;
            }


            // TODO Make sure all possible combinations work
            // execute runs the input process
            // processThreads are always running, passing available data from decoder to encoder and encoder to output
            initProcessThreads();


            logger.debug("Using coder process: {}", inputProcess);
            imgOut.setData(new DataBlockCompressed());
            nativeFrameData = new BytePointer((long)50*1024);
            if (inCodec == CodecEnum.YUV) {
                planeY = new BytePointer((long) 50 * 1024);
                planeU = new BytePointer((long) 50 * 1024);
                planeV = new BytePointer((long) 50 * 1024);
                av_frame = new AVFrame();
                av_frame.width(width);
                av_frame.height(height);
                av_frame.format(AV_PIX_FMT_YUV420P);
                av_frame_get_buffer(av_frame, 32);
            } else if (inCodec == CodecEnum.RGB) {
                av_frame = new AVFrame();
                av_frame.width(width);
                av_frame.height(height);
                av_frame.format(AV_PIX_FMT_RGB24);
                av_frame_get_buffer(av_frame, 32);
            }
        }
        catch (IllegalArgumentException e)
        {
            reportError("Unsupported codec" + inCodecParam.getData().getStringValue() + ". Must be one of " + Arrays.toString(FFMpegTranscoder.CodecEnum.values()));
        }

        super.init();
    }

    private boolean isUncompressed(CodecEnum codec) {
        return codec == CodecEnum.RGB || codec == CodecEnum.YUV;
    }

    private int safeGetCountVal(Count count) {
        int val = 0;
        if (count != null && count.hasData()) {
            try {
                val = count.getValue();
            } catch (Exception ignored) {}
        }
        return val;
    }

    private void decoder_to_encoder() {
        while (doRun.get() && !Thread.currentThread().isInterrupted()) {
            // Block until frames are received
            Queue<AVFrame> frames = null;
            try {
                 frames = decoder.getPackets();
            } catch (InterruptedException e) {
                logger.debug("Interrupted while decoding frames");
                Thread.currentThread().interrupt();
                return;
            }

            if (frames != null && !frames.isEmpty()) {
                for (AVFrame frame : frames) {
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }

                    frame = swsScale(frame);
                    if (frame != null) {
                        encoder.addPacket(av_frame_clone(frame));
                        av_frame_free(frame);
                    }
                }
            }
        }
    }

    private void input_to_output() {
        if (imgIn == null || imgIn.getData() == null || imgIn.getData().getUnderlyingObject() == null) {
            logger.warn("Input image is null");
            return;
        }

        // get input encoded frame data
        byte[] frameData = ((DataBlockCompressed)imgIn.getData()).getUnderlyingObject().clone();
        //System.out.println("Frame size=" + frameData.length);

        if (inCodec == CodecEnum.RGB) {
            setRgbFrame(frameData);
        } else {
            setYuvFrame(frameData);
        }

        swsScale(av_frame_clone(av_frame));
        if (sws_frame != null) {
            if (outCodec == CodecEnum.RGB) {
                frameData = new byte[width * height * 3];
                sws_frame.data(0).get(frameData);
            } else {
                // YUV420P: U and V planes are half the Y plane
                frameData = new byte[width * height * 2];
                frameData = getYuvFrameData(sws_frame);
            }

            publishFrameData(frameData);
            frameData = null;
            //av_frame_unref(av_frame);
            //av_frame_unref(sws_frame);
            //av_frame_free(av_frame);
        }
    }


    private void input_to_decoder() {
        if (imgIn == null || imgIn.getData() == null || imgIn.getData().getUnderlyingObject() == null) {
            logger.warn("Input image is null");
            return;
        }

        // get input encoded frame data
        byte[] frameData = ((DataBlockCompressed)imgIn.getData()).getUnderlyingObject().clone();
        //System.out.println("Frame size=" + frameData.length);

        // grow packet data buffer as needed
        setNativeBytePointer(frameData);

        // decode frame
        dec_pkt.data(nativeFrameData);
        dec_pkt.size(frameData.length);

        decoder.addPacket(av_packet_clone(dec_pkt));
    }

    private void input_to_encoder() {
        // get input encoded frame data
        byte[] frameData = ((DataBlockCompressed)imgIn.getData()).getUnderlyingObject().clone();
        //System.out.println("Frame size=" + frameData.length);

        if (inCodec == CodecEnum.RGB) {
            setRgbFrame(frameData);
        } else {
            setYuvFrame(frameData);
        }

        swsScale(av_frame_clone(av_frame));
        if (sws_frame != null) {
            // Send frame to encoder
            encoder.addPacket(av_frame_clone(sws_frame));
        }
    }

    private void setRgbFrame(byte[] frameData) {
        //setNativeBytePointer(frameData);
        //av_frame.linesize(0, width * 3);
        int ret = av_frame_make_writable(av_frame);
        if (ret >= 0) {
            allocateFrameBufs(CodecEnum.RGB);
            av_frame.data(0).position(0);
            av_frame.data(0).put(frameData.clone(), 0, frameData.length);
        } else {
            logFFmpeg(ret);
        }
    }

    private void allocateFrameBufs(CodecEnum codec) {
        if (av_frame == null) {
            throw new NullPointerException("Frame is null");
        }
        av_frame.width(width);
        av_frame.height(height);
        av_frame.format(codec == CodecEnum.RGB ? AV_PIX_FMT_RGB24 : AV_PIX_FMT_YUV420P);
        av_frame_get_buffer(av_frame, 32);
    }

    private void setNativeBytePointer(byte[] frameData) {
        if (nativeFrameData.capacity() != frameData.length) {
            nativeFrameData.deallocate();
            nativeFrameData = new BytePointer(frameData.length);
        }

        nativeFrameData.position(0);
        nativeFrameData.limit(0);
        nativeFrameData.put(frameData.clone());
    }

    private void setYuvFrame(byte[] frameData) {
        // TODO: No info (that I am aware of) for how YUV should be stored in OSH. Need to find an example or figure out how it should be stored.
        // This is placeholder. VERY POSSIBLY INCORRECT.
        // For now, assuming Y, U, and V planes are stored one after the other
        // For 4:2:0, U and V sizes are half of Y

        int planeSize = frameData.length / 2;
        int uvPlaneSize = planeSize / 2;

        int ret = av_frame_make_writable(av_frame);
        if (ret >= 0) {
            allocateFrameBufs(CodecEnum.RGB);
            av_frame.data(0).position(0);
            av_frame.data(0).put(Arrays.copyOfRange(frameData, 0, planeSize));
            av_frame.data(1).put(Arrays.copyOfRange(frameData, planeSize, planeSize + uvPlaneSize));
            av_frame.data(2).put(Arrays.copyOfRange(frameData, planeSize + uvPlaneSize, planeSize + uvPlaneSize * 2));
        } else {
            logFFmpeg(ret);
        }
    }

    private AVFrame swsScale(AVFrame inFrame) {
        if (sws_ctx == null) {
            sws_frame = inFrame;
            return inFrame;
        }
        int ret = sws_scale_frame(sws_ctx, sws_frame, inFrame);

        //int ret = sws_scale(sws_ctx, inFrame.data(), inFrame.linesize(), 0, inFrame.height(), sws_frame.data(), sws_frame.linesize());


        if (ret < 0) {
            logFFmpeg(ret);
            return null;
        } else {
            // For some reason, av_frame_clone returns null here
            return av_frame_clone(sws_frame);
        }
    }

    private void logFFmpeg(int retCode) {
        BytePointer buf = new BytePointer(AV_ERROR_MAX_STRING_SIZE);
        av_strerror(retCode, buf, buf.capacity());
        logger.warn("FFmpeg returned error code {}: {}", retCode, buf.getString());
    }

    private void encoder_to_output() {
        while (doRun.get() && !Thread.currentThread().isInterrupted()) {
            Queue<AVPacket> packets = null;
            try {
                packets = encoder.getPackets();
            } catch (InterruptedException e) {
                logger.debug("Interrupted while decoding packets");
                Thread.currentThread().interrupt();
                return;
            }

            if (packets != null && !packets.isEmpty()) {
                for (AVPacket packet : packets) {
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    byte[] frameData = new byte[packet.size()];
                    packet.data().get(frameData);

                    publishFrameData(frameData);
                    //frameData = null;
                    av_packet_free(packet);
                    //packet = null;
                }
            }
        }
    }

    private void publishFrameData(byte[] frameData) {

        ((DataBlockCompressed) imgOut.getData()).setUnderlyingObject(frameData.clone());
        // also copy frame timestamp
        double ts;
        if (inputTimeStamp != null && inputTimeStamp.getData() != null) {
            ts = inputTimeStamp.getData().getDoubleValue();
        } else {
            ts = System.currentTimeMillis();
        }
        outputTimeStamp.getData().setDoubleValue(ts);
        try {
            //logger.debug("Publishing");
            super.publishData();
        } catch (Exception e) {
            logger.error("Error publishing output packet", e);
            return;
        }
    }

    private void decoder_to_output() {
        while (doRun.get() && !Thread.currentThread().isInterrupted()) {
            Queue<AVFrame> frames = null;
            try {
                frames = decoder.getPackets();
            } catch (InterruptedException e) {
                logger.debug("Interrupted while decoding packets");
                Thread.currentThread().interrupt();
                return;
            }

            if (frames != null && !frames.isEmpty()) {
                for (AVFrame frame : frames) {
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    frame = swsScale(frame);
                    if (frame != null) {
                        byte[] frameData;

                        if (outCodec == CodecEnum.RGB) {
                            frameData = new byte[width * height * 3];
                            frame.data(0).get(frameData);
                        } else {
                            // YUV420P: U and V planes are half the Y plane
                            frameData = new byte[width * height * 2];
                            frameData = getYuvFrameData(frame);
                        }

                        publishFrameData(frameData);
                        frameData = null;
                        av_frame_free(frame);
                    }
                    frame = null;
                }
            }
        }
    }

    public byte[] getYuvFrameData(AVFrame inFrame) {
        ByteBuffer frameData = ByteBuffer.allocate(inFrame.data().sizeof());
        byte[] temp = new byte[inFrame.data(0).sizeof()];
        // TODO verify this works
        for (int i = 0; i < 3; i++) {
            inFrame.data(i).get(temp);
            frameData.put(temp);
        }
        return frameData.array().clone();
    }


    @Override
    public void execute() throws ProcessException
    {
        // Start the threads if not already started
        if (!isRunning.get()) {
            startProcessThreads();
        }

        publish = false;
        inputProcess.run();
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

        doRun.set(false);
        if (encoder != null) {
            encoder.doRun.set(false);
        }
        if (decoder != null) {
            decoder.doRun.set(false);
        }
        if (processThreads != null) {
            for (Thread t : processThreads) {
                t.interrupt();
                try {
                    t.join();
                } catch (InterruptedException e) {
                    logger.error("Error waiting for process thread {} to finish", t.getName());
                }
            }
        }
        //disposeCoder(encoderThread, encoder);
        //disposeCoder(decoderThread, decoder);
    }
}