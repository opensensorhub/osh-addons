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
import org.bytedeco.javacpp.BytePointer;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.sensorhub.impl.process.video.transcoder.coders.*;
import org.sensorhub.impl.process.video.transcoder.formatters.*;
import org.sensorhub.impl.process.video.transcoder.helpers.CodecInfo;
import org.sensorhub.impl.process.video.transcoder.helpers.CodecOptions;
import org.sensorhub.impl.process.video.transcoder.helpers.FullCodecEnum;
import org.sensorhub.impl.process.video.transcoder.helpers.FullPixelEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.DataBlockCompressed;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.RasterHelper;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.bytedeco.ffmpeg.global.avutil.*;

/**
 * <p>
 * Implementation of video transcoder based on FFMPEG
 * </p>
 *
 * @author Kyle Fitzpatrick
 * @since Aug 2025
 */

public class FFMpegTranscoder extends ExecutableProcessImpl
{
    private static final Logger logger = LoggerFactory.getLogger(FFMpegTranscoder.class);

	public static final OSHProcessInfo INFO = new OSHProcessInfo("video:FFMpegTranscoder", "FFMPEG Video Transcoder", null, FFMpegTranscoder.class);

    AtomicBoolean isInit = new AtomicBoolean(false);
    Time inputTimeStamp;
    Count inputWidth, inputHeight;
    DataArray imgIn;
    Time outputTimeStamp;
    Count outputWidth, outputHeight, inputFps, inputBitrate;
    DataArray imgOut;
    Text inCodecParam;
    Text outCodecParam;

    List<Codec> videoProcs;
    AVByteFormatter inputFormatter, outputFormatter;

    CodecOptions decOptions, encOptions;

    final boolean publish = false;
    CodecInfo inCodec;
    CodecInfo outCodec;
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
                //.addAllowedValues(CodecEnum.class)
                .build());

        paramData.add("outCodec", outCodecParam = swe.createText()
                .definition(SWEHelper.getPropertyUri("Codec"))
                .label("Output Codec Name")
                //.addAllowedValues(CodecEnum.class)
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

    private void initFormatters() throws ProcessException {
        inputFormatter = getFormatter(inCodec, width, height);
        outputFormatter = getFormatter(outCodec, outWidth, outHeight);
    }

    /**
     * Initializes all encoder/decoder/swscaler objects. These objects are added to the {@link FFMpegTranscoder#videoProcs}
     * queue in the order they should process the incoming data. At most, the flow will be Decoder -> SWScale -> Encoder.
     */
    private void initCodecs() {
        if (videoProcs != null) {
            videoProcs.clear();
        }
        videoProcs = new ArrayList<>();

        Decoder decoder = null;
        Encoder encoder = null;
        SwScaler swScaler = null;
        //FullPixelEnum decOutPixFmt = outCodec.pixelFmt;
        //FullPixelEnum encInPixFmt = outCodec.pixelFmt;

        if (!isUncompressed(inCodec)) {
            decoder = new Decoder(inCodec, outCodec, decOptions);
            outCodec.pixelFmt = decoder.init();
            //videoProcs.add(decoder);
        }
        if (!isUncompressed(outCodec)) {
            encoder = new Encoder(inCodec, outCodec, encOptions);
            inCodec.pixelFmt = encoder.init();
            //videoProcs.add(encoder);
        }

        logger.info("Input pixel format: {}, Output pixel format: {}", inCodec.pixelFmt, outCodec.pixelFmt);

        if (inCodec.pixelFmt == null || outCodec.pixelFmt == null) { logger.warn("Pixel format is null"); }

        if (width != outWidth || height != outHeight
                || (isUncompressed(inCodec) && isUncompressed(outCodec))
                || (inCodec.pixelFmt != outCodec.pixelFmt)) {

            swScaler = new SwScaler(inCodec, outCodec, width, height, outWidth, outHeight);
            swScaler.init();
            //videoProcs.add(swScaler);
        }

        if (decoder != null) { videoProcs.add(decoder); }
        if (swScaler != null) { videoProcs.add(swScaler); }
        if (encoder != null) { videoProcs.add(encoder); }

    }

    private void initPipeline() {
        // Frame pipe between decoder, swscaler, encoder
        for (int i = 0; i < videoProcs.size() - 1; i++) {
            var nextProc = videoProcs.get(i + 1);
            videoProcs.get(i).registerCallback(packet -> {
                nextProc.submitInputPacket(packet);
            });
        }

        // Output
        var finalProc = videoProcs.get(videoProcs.size() - 1);
        finalProc.registerCallback(packet -> {
            try {
                publishFrameData(outputFormatter.convertOutput(packet));
            } finally {
                finalProc.deallocateOutputPacket(packet);
            }
        });
    }

    /**
     * Invoked on process stop and init.
     * Stop all {@link Codec} thread objects stored in {@link FFMpegTranscoder#videoProcs}.
     */
    private void stopProcessing() throws InterruptedException {
        isInit.set(false);
        if (videoProcs != null) {
            for (Codec codec : videoProcs) {
                codec.close();
            }
            videoProcs.clear();
        }
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
        try {
            stopProcessing();
        } catch (InterruptedException e) {
            logger.warn("Interrupted while stopping process threads");
        }
        super.stop();
    }

    /**
     * Add codecs for input/output video to the corresponding data structure encodings.
     * Helps identify codec used during serialization.
     */
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
        if (!isInit.compareAndSet(false, true)) {
            return;
        }

        // init decoder according to configured codec
        // TODO: Automatically detect input codec from compression in data struct?
        try
        {
            //inCodec = CodecEnum.valueOf(inCodecParam.getData().getStringValue().toUpperCase());
            //outCodec = CodecEnum.valueOf(outCodecParam.getData().getStringValue().toUpperCase());
            inCodec = CodecInfo.newCodecInfoFromName(inCodecParam.getData().getStringValue());
            outCodec = CodecInfo.newCodecInfoFromName(outCodecParam.getData().getStringValue());

            setImgEncoding();
            initCodecOptions();
            initFormatters();
            initCodecs();
            initPipeline();

            imgOut.setData(new DataBlockCompressed());
        }
        catch (IllegalArgumentException e)
        {
            reportError("Unsupported codec " + inCodecParam.getData().getStringValue() + ". Must be one of " + Arrays.toString(CodecEnum.values()), e);
        }

        super.init();
    }

    /**
     * Creates maps of options for the encoder/decoder, including framerate, pixel format, bitrate, and image size.
     */
    private void initCodecOptions() {
        var decOptionBuilder = new CodecOptions.Builder();
        var encOptionBuilder = new CodecOptions.Builder();

        //DataComponent temp;
        int fps = safeGetCountVal(inputFps);
        int bitrate = safeGetCountVal(inputBitrate);
        width = safeGetCountVal(inputWidth);
        if (width <= 0) {
            try {
                width = imgIn.getComponent("row").getComponentCount();
            } catch (Exception e) {
                width = 1920;
                logger.warn("Input width not specified, using default: 1920", e);
            }
        }

        height = safeGetCountVal(inputHeight);
        if (height <= 0) {
            try {
                height = imgIn.getComponentCount();
            } catch (Exception e) {
                height = 1080;
                logger.warn("Input height not specified, using default: 1080", e);
            }
        }


        outHeight = safeGetCountVal(outputHeight);
        if (outHeight <= 0) {
            try {
                outHeight = imgOut.getComponentCount();
            } catch (Exception e) {
                outHeight = height;
                logger.warn("Output height not specified, using input height", e);
            }
        }

        outWidth = safeGetCountVal(outputWidth);
        if (outWidth <= 0) {
            try {
                outWidth = imgIn.getComponent("row").getComponentCount();
            } catch (Exception e) {
                outWidth = width;
                logger.warn("Output width not specified, using input width", e);
            }
        }

        decOptions = decOptionBuilder.setFps(fps).setBitRate(bitrate)
                .setWidth(width).setHeight(height).presetUltraFast().tuneZeroLatency()
                .setComplianceUnofficial().build();
        encOptions = encOptionBuilder.setFps(fps).setBitRate(bitrate)
                .setWidth(outWidth).setHeight(outHeight).presetUltraFast().tuneZeroLatency()
                .setComplianceUnofficial().build();
    }

    /**
     * Creates a data formatter for the corresponding codec and image size. Used for converting between raw byte arrays
     * and FFmpeg's {@link AVFrame} and {@link AVPacket} structs.
     * @param codec Video codec or uncompressed format.
     * @param width Video frame width.
     * @param height Video frame height.
     * @return Formatter object.
     * @throws ProcessException Thrown when width and height are not provided for an uncompressed format.
     */
    private AVByteFormatter getFormatter(CodecInfo codec, @Nullable Integer width, @Nullable Integer height) throws ProcessException {
        try {
            if (isUncompressed(codec))
                return new FrameFormatter(width, height, codec.pixelFmt.ffmpegId);
            else
                return new PacketFormatter();
            /*
            return switch (codec) {
                case RGB -> new RgbFormatter(width, height);
                case YUV -> new YuvFormatter(width, height);
                default -> new PacketFormatter();
            };
             */
        } catch (NullPointerException e) {
            reportError("Formatter for " + codec + " requires non-null width and height.", e);
        }
        return null;
    }

    /**
     * @param codec Video codec or uncompressed format.
     * @return Is the codec {@link FullCodecEnum#RAWVIDEO}?
     */
    private boolean isUncompressed(CodecInfo codec) {
        return codec.codec == FullCodecEnum.RAWVIDEO;
    }

    /**
     * Accesses count value, checking for exceptions and missing data.
     * @param count {@link Count} object to access.
     * @return Value of {@code count} or 0 if not accessible.
     */
    private int safeGetCountVal(Count count) {
        int val = 0;
        if (count != null && count.hasData()) {
            try {
                val = count.getValue();
            } catch (Exception ignored) {}
        }
        return val;
    }

    /**
     * Convert an FFmpeg return code to a string and log to the console.
     * @param retCode Integer returned from an FFmpeg library function.
     */
    private void logFFmpeg(int retCode) {
        BytePointer buf = new BytePointer(AV_ERROR_MAX_STRING_SIZE);
        av_strerror(retCode, buf, buf.capacity());
        logger.warn("FFmpeg returned error code {}: {}", retCode, buf.getString());
    }

    /**
     * Set the underlying data in the output record and invoke {@link ExecutableProcessImpl#publishData()}.
     * @param frameData Output video packet/frame data.
     */
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

    @Override
    public void execute() throws ProcessException
    {
        if (imgIn == null || imgIn.getData() == null || imgIn.getData().getUnderlyingObject() == null) {
            logger.warn("Input image is null");
            return;
        }

        // Start the threads if not already started
        if (!isInit.get()) {
            init();
        }

        if (!isVideoProcChainReady()) {
            logger.warn("Video processor not ready");
            return;
        }

        videoProcs.get(0).submitInputPacket(
                inputFormatter.convertInput(((DataBlockCompressed)imgIn.getData()).getUnderlyingObject().clone())
        );
    }

    private boolean isVideoProcChainReady() {
        for (Codec proc : videoProcs) {
            if (!proc.isReady()) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void publishData() throws InterruptedException
    {
        if (publish)
            super.publishData();
    }

    @Override
    public void dispose()
    {
        super.dispose();

        isInit.set(false);

        if (videoProcs != null) {
            for (Codec proc : videoProcs) {
                proc.close();
            }
        }
    }
}