/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.process.video;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avutil.*;
import static org.bytedeco.ffmpeg.global.swscale.*;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.opengis.swe.v20.Count;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.Text;
import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.swscale.SwsContext;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.PointerPointer;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.vast.data.DataBlockByte;
import org.vast.data.DataBlockCompressed;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.RasterHelper;


/**
 * <p>
 * Implementation of video decoder based on FFMPEG
 * </p>
 *
 * @author Alex Robin
 * @date Jun 1, 2021
 */
public class FFMpegDecoder extends ExecutableProcessImpl
{
	public static final OSHProcessInfo INFO = new OSHProcessInfo("video:FFMpegDecoder", "FFMPEG Video Decoder", null, FFMpegDecoder.class);
	
	enum CodecEnum {
	    //AUTO("auto"),
	    H264("h264"),
	    H265("hevc"),
	    VP8("vp8"),
	    VP9("vp9"),
	    MPEG2("mpeg2video");
	    
	    String ffmpegName;
	    
	    CodecEnum(String ffmpegName)
	    {
	        this.ffmpegName = ffmpegName;
	    }
	}
	
	private Count inputWidth, inputHeight;
	private DataArray imgIn;
	private Count outputWidth, outputHeight;
    private DataArray imgOut;
    private Text codecParam;
    
    AVCodec decoder = null;
    AVCodecContext decode_ctx = null;
    SwsContext sws_ctx = null;
    AVFrame av_frame = null;
    AVFrame sws_frame = null;
    AVPacket dec_pkt = null;
    BytePointer nativeFrameData;
    
    
    public FFMpegDecoder()
    {
    	super(INFO);
        RasterHelper swe = new RasterHelper();
    	
    	// inputs
        inputData.add("codedFrame", swe.createRecord()
            .label("Video Frame")
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
        codecParam = swe.createText()
            .definition(SWEHelper.getPropertyUri("Codec"))
            .label("Codec Name")
            .addAllowedValues(Stream.of(CodecEnum.values())
                .map(e -> e.toString())
                .collect(Collectors.toList()))
            .build();
        codecParam.assignNewDataBlock();
        paramData.add("codec", codecParam);
        
        // outputs
        outputData.add("rgbFrame", swe.createRecord()
            .label("Video Frame")
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
        
        // init decoder according to configured codec
        try
        {
            var codec = CodecEnum.valueOf(codecParam.getData().getStringValue());
                        
            // init decoder context
            decoder = avcodec_find_decoder_by_name(codec.ffmpegName);
            decode_ctx = avcodec_alloc_context3(decoder);
            if (avcodec_open2(decode_ctx, decoder, (PointerPointer<?>)null) < 0) {
                throw new IllegalStateException("Error initializing " + codec + " decoder");
            }
            
            // init FFMPEG objects
            av_log_set_level(getLogger().isDebugEnabled() ? AV_LOG_INFO : AV_LOG_FATAL);
            dec_pkt = av_packet_alloc();
            av_init_packet(dec_pkt);
            av_frame = av_frame_alloc();
            sws_frame = av_frame_alloc();
            nativeFrameData = new BytePointer(50*1024);
        }
        catch (IllegalArgumentException e)
        {
            throw new ProcessException("Unsupported codec. Must be one of " + Arrays.toString(CodecEnum.values()), e);
        }        
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
    

    @Override
    public void execute() throws ProcessException
    {
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
        /*int ret1 = */avcodec_send_packet(decode_ctx, dec_pkt);
        int ret2 = avcodec_receive_frame(decode_ctx, av_frame);
        av_packet_unref(dec_pkt);
        //System.out.printf("decode: ret1 %d ret2 %d\n", ret1, ret2);
        
        if (ret2 == 0)
        {                
            // init scaler once we decode the 1st frame
            if (sws_ctx == null )
                initScaler(av_frame);
            
            // apply scaler (needed to convert from YUV to RGB)
            sws_scale(sws_ctx, av_frame.data(), av_frame.linesize(), 0, av_frame.height(),sws_frame.data(), sws_frame.linesize());
            
            // write decoded data to output
            // TODO use special datablock to encapsulate native buffer
            // would be more efficient when passing frame to other native libraries (e.g. OpenCV)
            
            frameData = new byte[av_frame.width() * av_frame.height() * 3];
            sws_frame.data(0).get(frameData);
            ((DataBlockByte)imgOut.getData()).setUnderlyingObject(frameData);
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

        if (decode_ctx != null) {
            avcodec_close(decode_ctx);
            avcodec_free_context(decode_ctx);
            decode_ctx = null;
        }
        if (dec_pkt != null) {
            av_packet_free(dec_pkt);
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