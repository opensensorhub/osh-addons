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
import org.vast.data.DataArrayImpl;
import org.vast.data.DataBlockByte;
import org.vast.data.DataBlockCompressed;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.RasterHelper;

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
    int frameCounter = 0;
    int decimFactor = 1;
    boolean publish;
    CodecEnum inCodec;
    CodecEnum outCodec;
    Runnable inputProcess;
    int swsPixFmt = AV_PIX_FMT_YUV420P;
    RasterHelper swe = new RasterHelper();
    BinaryBlock compressedBlock;
    BinaryEncoding dataEnc;

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

        // TODO Add width/height parameters for scaling
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
        outputData.add("outFrame", swe.createRecord()
                .name("outFrame")
                .label("Output Video Frame")
                .description("")
                .definition(SWEHelper.getPropertyUri("VideoFrame"))
                .addField("sampleTime", outputTimeStamp =  swe.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Sample Time")
                        .description("Time of data collection").build())
                .addField("img", imgOut = swe.newRgbImage(swe.createCount().id("width").build(), swe.createCount().id("height").build(), DataType.BYTE))
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
                inputProcess = null;
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

        // TODO Figure out if this allows for video to be resized, or is just accommodating changing inputs
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

            dataEnc = swe.newBinaryEncoding(ByteOrder.BIG_ENDIAN, ByteEncoding.RAW);

            BinaryComponent timeEnc = swe.newBinaryComponent();
            timeEnc.setRef("/" + ((DataRecord)outputData.get(0)).getComponent(0).getName());
            timeEnc.setCdmDataType(DataType.DOUBLE);
            dataEnc.addMemberAsComponent(timeEnc);

            compressedBlock = swe.newBinaryBlock();
            compressedBlock.setRef("/" + ((DataRecord)outputData.get(0)).getComponent(1).getName());
            compressedBlock.setCompression(outCodecParam.getData().getStringValue().toUpperCase());
            dataEnc.addMemberAsBlock(compressedBlock);
            try {
                SWEHelper.assignBinaryEncoding((DataRecord) outputData.get(0), dataEnc);
            } catch (Exception e) {
                logger.warn("Invalid encoding");
            }

            // Set options for the codecs
            // May add more, so using hashmap
            // May need two hashmaps. For fps, need to work around the decimation factor (or just remove it)
            // TODO Put this in its own function
            decOptions = new HashMap<>();
            encOptions = new HashMap<>();

            DataComponent temp;
            temp = inputFps;
            int fps = 0;
            if (temp != null && temp.hasData()) {
                fps = temp.getData().getIntValue();
            }
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

            temp = inputBitrate;
            int bitrate = 0;
            if (temp != null && temp.hasData()) {
                bitrate = temp.getData().getIntValue();
            }
            if (bitrate > 0) {
                decOptions.put("bit_rate", String.valueOf(bitrate));
                encOptions.put("bit_rate", String.valueOf(bitrate)); // Just assuming input br is the same as out, could this change?
            }
            int width = 0;
            temp = inputWidth;
            if (temp != null && temp.hasData() && temp.getData().getIntValue() > 0) {
                width = temp.getData().getIntValue();
            } else {
                width = imgIn.getComponent("row").getComponentCount();
            }
            if (width > 0) {
                decOptions.put("width", String.valueOf(width));
                encOptions.put("width", String.valueOf(width));
            }
            temp = inputHeight;
            int height = 0;
            if (temp != null && temp.hasData() && temp.getData().getIntValue() > 0) {
                height = temp.getData().getIntValue();
            } else {
                height = imgIn.getComponentCount();
            }
            if (height > 0) {
                decOptions.put("height", String.valueOf(height));
                encOptions.put("height", String.valueOf(height));
            }

            dec_pkt = av_packet_alloc();
            av_init_packet(dec_pkt);

            // TODO Make sure all possible combinations work
            // execute runs the input process
            // processThreads are always running, passing available data from decoder to encoder and encoder to output
            initProcessThreads();


            logger.debug("Using coder process: {}", inputProcess);

            nativeFrameData = new BytePointer((long)50*1024);
        }
        catch (IllegalArgumentException e)
        {
            reportError("Unsupported codec" + inCodecParam.getData().getStringValue() + ". Must be one of " + Arrays.toString(FFMpegTranscoder.CodecEnum.values()));
        }

        super.init();
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

                    //swsScale(frame);
                    encoder.addPacket(av_frame_clone(frame));
                    av_frame_free(frame);
                }
            }
        }
    }
    // TODO Update this to match input_to_encoder
    private void input_to_decoder() {
        if (imgIn == null || imgIn.getData() == null || imgIn.getData().getUnderlyingObject() == null) {
            logger.warn("Input image is null");
            return;
        }
        // get input encoded frame data
        byte[] frameData = ((DataBlockCompressed)imgIn.getData()).getUnderlyingObject().clone();
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

        decoder.addPacket(av_packet_clone(dec_pkt));
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
        //swsScale(av_frame);

        // Send frame to encoder
        encoder.addPacket(av_frame);
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
                    frameData = null;
                    av_packet_free(packet);
                    packet = null;
                }
            }
        }
    }

    private void decoder_to_output() {
        while (doRun.get() && !Thread.currentThread().isInterrupted()) {
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
                    //swsScale(frame);
                    int size = frame.width() * frame.height() * 3;
                    byte[] frameData = new byte[size];
                    frame.data(0).get(frameData);
                    //frame.data().get(frameData);
                    if (imgOut.getComponent("width").getData().getIntValue() != frame.width() || imgOut.getComponent("height").getData().getIntValue() != frame.height()) {
                        logger.debug("Updating size");
                        imgOut.getComponent("width").setData(swe.createCount().value(frame.width()).build().getData());
                        imgOut.getComponent("height").setData(swe.createCount().value(frame.height()).build().getData());
                        imgOut.updateSize(); // Not sure if this line is necessary
                    }
                    ((DataBlockByte) imgOut.getData()).setUnderlyingObject(frameData);
                    // also copy frame timestamp
                    var ts = inputTimeStamp.getData().getDoubleValue();
                    outputTimeStamp.getData().setDoubleValue(ts);

                    try {
                        super.publishData();
                    } catch (InterruptedException e) {
                        logger.error("Error publishing output frame", e);
                    }
                    av_frame_free(frame);
                }
            }
        }
    }

    @Override
    public void execute() throws ProcessException
    {
        // Start the threads if not already started
        if (!isRunning.get()) {
            startProcessThreads();
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