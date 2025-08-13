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
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.Pointer;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.sensorhub.impl.process.video.transcoder.coders.Coder;
import org.sensorhub.impl.process.video.transcoder.coders.Decoder;
import org.sensorhub.impl.process.video.transcoder.coders.Encoder;
import org.sensorhub.impl.process.video.transcoder.coders.SwScaler;
import org.sensorhub.impl.process.video.transcoder.formatters.AVByteFormatter;
import org.sensorhub.impl.process.video.transcoder.formatters.PacketFormatter;
import org.sensorhub.impl.process.video.transcoder.formatters.RgbFormatter;
import org.sensorhub.impl.process.video.transcoder.formatters.YuvFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.DataBlockCompressed;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.RasterHelper;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avutil.*;
import static org.bytedeco.ffmpeg.global.swscale.*;

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

    List<Coder<?, ?>> videoProcs;
    AVByteFormatter<Pointer> inputFormatter, outputFormatter;
    ArrayDeque<Pointer> inputPackets;
    ArrayDeque<Pointer> outputPackets;
    Thread outputThread;

    HashMap<String, Integer> decOptions = new HashMap<>();
    HashMap<String, Integer> encOptions = new HashMap<>();

    final boolean publish = false;
    CodecEnum inCodec;
    CodecEnum outCodec;
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
                .addAllowedValues(CodecEnum.class)
                .build());

        paramData.add("outCodec", outCodecParam = swe.createText()
                .definition(SWEHelper.getPropertyUri("Codec"))
                .label("Output Codec Name")
                .addAllowedValues(CodecEnum.class)
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

    /**
     * Initializes all encoder/decoder/swscaler objects. These objects are added to the {@link FFMpegTranscoder#videoProcs}
     * queue in the order they should process the incoming data. At most, the flow will be Decoder -> SWScale -> Encoder.
     */
    private void initCoders() {
        try {
            stopProcessThreads();
        } catch (Exception e){
            logger.error("Transcoder could not stop process threads during re-init.", e);
        }
        if (videoProcs != null) {
            videoProcs.clear();
        }
        videoProcs = new ArrayList<>();

        if (!isUncompressed(inCodec)) {
            videoProcs.add(new Decoder(inCodec.ffmpegId, decOptions));
        }
        if (width != outWidth || height != outHeight || (isUncompressed(inCodec) && isUncompressed(outCodec))) {
            int inFmt = isUncompressed(inCodec) ? inCodec.ffmpegId : AV_PIX_FMT_YUV420P;
            int outFmt = isUncompressed(outCodec) ? outCodec.ffmpegId : AV_PIX_FMT_YUV420P;
            videoProcs.add(new SwScaler(inFmt, outFmt, width, height, outWidth, outHeight));
        }
        if (!isUncompressed(outCodec)) {
            videoProcs.add(new Encoder(outCodec.ffmpegId, encOptions));
        }

        inputPackets = new ArrayDeque<>();
        if (videoProcs.get(0) != null) {
            videoProcs.get(0).setInQueue(inputPackets);
        }
        for (int i = 1; i < videoProcs.size(); i++) {
            videoProcs.get(i).setInQueue(videoProcs.get(i - 1).getOutQueue());
        }
        try {
            outputPackets = (ArrayDeque<Pointer>) videoProcs.get(videoProcs.size() - 1).getOutQueue();
        } catch (Exception e) {
            logger.warn("No processes running on video input. Check codec/resolution settings.", e);
            outputPackets = inputPackets;
        }
    }

    /**
     * Invoked during the first call to {@link FFMpegTranscoder#execute()} (when {@link FFMpegTranscoder#isRunning} is false).
     * Start all {@link Coder} thread objects stored in {@link FFMpegTranscoder#videoProcs}.
     */
    private void startProcessThreads() {
        doRun.set(true);
        if (videoProcs == null || videoProcs.isEmpty() || videoProcs.get(0).getState() != Thread.State.NEW) {
            initCoders();
        }

        for (Thread process : videoProcs) {
            process.start();
        }
        outputThread.start();

        isRunning.set(true);
    }

    /**
     * Invoked on process stop and init.
     * Stop all {@link Coder} thread objects stored in {@link FFMpegTranscoder#videoProcs}.
     */
    private void stopProcessThreads() throws InterruptedException {
        doRun.set(false); //TODO These atomic booleans may be entirely unnecessary, remove
        if (videoProcs != null) {
            for (Thread thread : videoProcs) {
                thread.interrupt();
                thread.join();
            }
        }
        outputThread.interrupt();
        outputThread.join();
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
        doRun.set(true);
        isRunning.set(false);

        // init decoder according to configured codec
        // TODO: Automatically detect input codec from compression in data struct?
        try
        {
            inCodec = CodecEnum.valueOf(inCodecParam.getData().getStringValue().toUpperCase());
            outCodec = CodecEnum.valueOf(outCodecParam.getData().getStringValue().toUpperCase());

            setImgEncoding();

            initCodecOptions();

            // processThreads are always running, passing available data from decoder to encoder and encoder to output
            initCoders();
            outputThread = new Thread(this::outputProcess);

            inputFormatter = getFormatter(inCodec, width, height);
            outputFormatter = getFormatter(outCodec, outWidth, outHeight);

            imgOut.setData(new DataBlockCompressed());
        }
        catch (IllegalArgumentException e)
        {
            reportError("Unsupported codec" + inCodecParam.getData().getStringValue() + ". Must be one of " + Arrays.toString(CodecEnum.values()));
        }

        super.init();
    }

    /**
     * Creates maps of options for the encoder/decoder, including framerate, pixel format, bitrate, and image size.
     */
    private void initCodecOptions() {
        decOptions = new HashMap<>();
        encOptions = new HashMap<>();

        //DataComponent temp;
        int fps = safeGetCountVal(inputFps);
        if (fps > 0) {
            decOptions.put("fps", fps);
            encOptions.put("fps", fps);
        }

        encOptions.put("pix_fmt", AV_PIX_FMT_YUV420P);
        decOptions.put("pix_fmt", AV_PIX_FMT_YUV420P);

        int bitrate = safeGetCountVal(inputBitrate);
        if (bitrate > 0) {
            decOptions.put("bit_rate", bitrate);
            encOptions.put("bit_rate", bitrate); // Just assuming input br is the same as out, could this change?
        }

        width = safeGetCountVal(inputWidth);
        if (width <= 0) {
            width = imgIn.getComponent("row").getComponentCount();
        }
        decOptions.put("width", width);

        height = safeGetCountVal(inputHeight);
        if (height <= 0) {
            height = imgIn.getComponentCount();
        }
        decOptions.put("height", height);


        outHeight = safeGetCountVal(outputHeight);
        if (outHeight <= 0) {
            try {
                outHeight = imgOut.getComponentCount();
            } catch (Exception ignored) {
                outHeight = 0;
            }
        }
        if (outHeight > 0) {
            encOptions.put("height", outHeight);
        } else {
            encOptions.put("height", height);
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
            encOptions.put("width", outWidth);
        } else {
            encOptions.put("width", width);
        }
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
    private AVByteFormatter getFormatter(CodecEnum codec, @Nullable Integer width, @Nullable Integer height) throws ProcessException {
        try {
            return switch (codec) {
                case RGB -> new RgbFormatter(width, height);
                case YUV -> new YuvFormatter(width, height);
                default -> new PacketFormatter();
            };
        } catch (NullPointerException e) {
            reportError("Raw formatter for " + codec + " requires non-null width and height.", e);
        }
        return null;
    }

    /**
     * @param codec Video codec or uncompressed format.
     * @return Is the codec {@link CodecEnum#RGB} or {@link CodecEnum#YUV}?
     */
    private boolean isUncompressed(CodecEnum codec) {
        return codec == CodecEnum.RGB || codec == CodecEnum.YUV;
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
        if (!isRunning.get()) {
            startProcessThreads();
        }

        inputPackets.add(
                inputFormatter.convertInput(
                        ((DataBlockCompressed)imgIn.getData()).getUnderlyingObject().clone()
                ));

    }

    /**
     * Runs inside a separate thread. Receives any {@link AVPacket}s or {@link AVFrame}s from the last {@link Coder}
     * in the {@link FFMpegTranscoder#videoProcs} queue, converts the struct to bytes, and publishes the data.
     * @see FFMpegTranscoder#publishFrameData(byte[])
     */
    private void outputProcess() {
        while (doRun.get() && !Thread.currentThread().isInterrupted()) {
            while (outputPackets == null || outputPackets.isEmpty()) {
                Thread.onSpinWait();
            }
            if (outputPackets != null && !outputPackets.isEmpty()) {
                for (var packet : outputPackets) {
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    publishFrameData(outputFormatter.convertOutput(outputPackets.poll()));
                    //frameData = null;
                    //packet = null;
                }
            }
        }
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

        doRun.set(false);

        if (videoProcs != null) {
            for (Thread t : videoProcs) {
                t.interrupt();
                try {
                    t.join();
                } catch (InterruptedException e) {
                    logger.error("Error waiting for process thread {} to finish", t.getName());
                }
            }
        }
        if (outputThread != null) {
            outputThread.interrupt();
            try {
                outputThread.join();
            } catch (InterruptedException e) {
                logger.error("Error waiting for process thread {} to finish", outputThread.getName());
            }
        }
    }
}