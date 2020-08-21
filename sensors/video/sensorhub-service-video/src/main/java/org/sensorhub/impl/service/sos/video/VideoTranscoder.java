/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2020 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sos.video;

import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.xml.namespace.QName;
import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.swscale.SwsContext;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.javacpp.PointerScope;
import org.sensorhub.impl.service.sos.ISOSCustomSerializer;
import org.sensorhub.impl.service.sos.ISOSDataProvider;
import org.sensorhub.impl.service.sos.SOSProviderUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.cdm.common.DataStreamWriter;
import org.vast.data.DataBlockMixed;
import org.vast.ows.OWSRequest;
import org.vast.ows.OWSUtils;
import org.vast.ows.sos.GetResultRequest;
import org.vast.swe.AbstractDataWriter;
import org.vast.swe.FilteredWriter;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.swe.fast.DataBlockProcessor;
import org.vast.swe.fast.FilterByDefinition;
import com.google.common.collect.Sets;
import net.opengis.swe.v20.BinaryBlock;
import net.opengis.swe.v20.BinaryEncoding;
import net.opengis.swe.v20.BinaryMember;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avutil.*;
import static org.bytedeco.ffmpeg.global.swscale.*;


/**
 * <p>
 * Custom serializer implementaion for real-time video transcoding
 * </p><p>
 * It can be enabled by appending the responseFormat configured in the SOS
 * settings to the request URL. Additional URL parameters are available to
 * specify frame width/height, framerate and bitrate.
 * </p><p>
 * Transcoding works only with H264 for now.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Jun 14, 2020
 */
public class VideoTranscoder implements ISOSCustomSerializer
{
    private static final Logger log = LoggerFactory.getLogger(VideoTranscoder.class);
    
    private static final Set<String> IMG_ARRAY_COMPONENT_NAMES = Sets.newHashSet("img", "videoFrame");
    private static final Map<String, CodecConfig> CODEC_TABLE = new HashMap<>();
    
    static {
        CODEC_TABLE.put("H264", new CodecConfig("h264", "libx264"));
        CODEC_TABLE.put("H265", new CodecConfig("hevc", "libx265"));
        CODEC_TABLE.put("HEVC", new CodecConfig("hevc", "libx265"));
        CODEC_TABLE.put("VP8", new CodecConfig("vp8", "libvpx"));
        CODEC_TABLE.put("VP9", new CodecConfig("vp9", "libvpx-vp9"));
    }
    
    static class CodecConfig
    {
        String decoderName;
        String encoderName;
        
        CodecConfig(String decName, String encName)
        {
            this.decoderName = decName;
            this.encoderName = encName;
        }
    }
    
    
    @Override
    public void write(ISOSDataProvider dataProvider, OWSRequest request) throws IOException
    {
        request.getHttpResponse().setContentType(OWSUtils.BINARY_MIME_TYPE);
        OutputStream os = new BufferedOutputStream(request.getResponseStream());
        GetResultRequest gReq = (GetResultRequest)request;
        
        String fpsString = (String)gReq.getExtensions().get(new QName("video_fps"));
        int fps = fpsString != null ? Integer.valueOf(fpsString) : 30;
        String bitrateString = (String)gReq.getExtensions().get(new QName("video_bitrate"));
        int bitrate = bitrateString != null ? Integer.valueOf(bitrateString)*1000 : 150*1000;
        String frameWidthString = (String)gReq.getExtensions().get(new QName("video_width"));
        int frameWidth = frameWidthString != null ? Integer.valueOf(frameWidthString) : 320;
        String frameHeightString = (String)gReq.getExtensions().get(new QName("video_height"));
        int frameHeight = frameHeightString != null ? Integer.valueOf(frameHeightString) : 180;
        String frameSizeScalingString = (String)gReq.getExtensions().get(new QName("video_scale"));
        double frameSizeScale = frameSizeScalingString != null ? Double.valueOf(frameSizeScalingString) : 1.0;
        
        if (fps <= 0)
            throw new IllegalArgumentException("Invalid frame rate requested");
        if (bitrate <= 0)
            throw new IllegalArgumentException("Invalid bitrate requested");
        if (fps <= 0 || bitrate <= 0 || frameWidth <= 0 || frameHeight <= 0 || frameSizeScale <= 0)
            throw new IllegalArgumentException("Invalid frame size requested");
        
        AVCodec decoder = null;
        AVCodec encoder = null;
        AVCodecContext decode_ctx = null;
        AVCodecContext encode_ctx = null;
        SwsContext sws_ctx = null;
        DataStreamWriter writer;
        CodecConfig codecConfig = null;
        AVFrame av_frame = null;
        AVFrame sws_frame = null;
        AVPacket dec_pkt = null;
        AVPacket enc_pkt = null;
        int imgCompIdx = -1;
        
        try (PointerScope scope = new PointerScope())
        {
            try
            {
                // get index of image component
                DataComponent dataStruct = dataProvider.getResultStructure();            
                for (int i = dataStruct.getComponentCount()-1; i >= 0; i--)
                {
                    if (IMG_ARRAY_COMPONENT_NAMES.contains(dataStruct.getComponent(i).getName()))
                    {
                        imgCompIdx = i;
                        break;
                    }
                }
                
                if (imgCompIdx < 0)
                    throw new IllegalArgumentException("Requested data stream does not contain video frames");
                
                
                // get native codec
                DataEncoding enc = dataProvider.getDefaultResultEncoding();
                if (enc instanceof BinaryEncoding)
                {
                    for (BinaryMember m: ((BinaryEncoding)enc).getMemberList())
                    {
                        if (m instanceof BinaryBlock)
                        {
                            String codecID = ((BinaryBlock) m).getCompression();
                            codecConfig = CODEC_TABLE.get(codecID);
                            break;
                        }
                    }                
                }            
                            
                if (codecConfig == null || imgCompIdx < 0)
                    throw new IllegalArgumentException("Transcoding is not supported for this video stream");
                
                // init FFMPEG objects
                av_log_set_level(log.isDebugEnabled() ? AV_LOG_INFO : AV_LOG_FATAL);
                dec_pkt = av_packet_alloc();
                av_init_packet(dec_pkt);
                enc_pkt = av_packet_alloc();
                av_init_packet(enc_pkt);
                av_frame = av_frame_alloc();
                sws_frame = av_frame_alloc();
                                
                // init decoder context
                decoder = avcodec_find_decoder_by_name(codecConfig.decoderName);
                decode_ctx = avcodec_alloc_context3(decoder);
                if (avcodec_open2(decode_ctx, decoder, (PointerPointer<?>)null) < 0) {
                    throw new IllegalStateException("Error initializing decoder " + codecConfig.decoderName);
                }
                
                // create decoder context
                encoder = avcodec_find_encoder_by_name(codecConfig.encoderName);
                encode_ctx = avcodec_alloc_context3(encoder);
                
                // prepare writer for selected encoding
                writer = SWEHelper.createDataWriter(dataProvider.getDefaultResultEncoding());
                
                // we also do filtering here in case data provider hasn't modified the datablocks
                // always keep sampling time and entity ID if present
                gReq.getObservables().add(SWEConstants.DEF_SAMPLING_TIME);
                String entityComponentUri = SOSProviderUtils.findEntityIDComponentURI(dataProvider.getResultStructure());
                if (entityComponentUri != null)
                    gReq.getObservables().add(entityComponentUri);
                // temporary hack to switch btw old and new writer architecture
                if (writer instanceof AbstractDataWriter)
                    writer = new FilteredWriter((AbstractDataWriter)writer, gReq.getObservables());
                else
                    ((DataBlockProcessor)writer).setDataComponentFilter(new FilterByDefinition(gReq.getObservables()));
                writer.setDataComponents(dataProvider.getResultStructure());
                writer.setOutput(os);
            }
            catch (IOException e)
            {
                throw new IOException("Error initializing video transcoding", e);
            }
            
            try
            {        
                // transcode and write all records
                long pts = 0;
                DataBlock nextRecord;
                BytePointer nativeFrameData = new BytePointer(50*1024);
                while ((nextRecord = dataProvider.getNextResultRecord()) != null)
                {        
                    // clone datablock so we don't interfere with other concurrently running streams
                    nextRecord = nextRecord.clone();
                    
                    // get frame data
                    DataBlock frameBlk = ((DataBlockMixed)nextRecord).getUnderlyingObject()[imgCompIdx];
                    byte[] frameData = (byte[])frameBlk.getUnderlyingObject();
                    
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
                    int ret1 = avcodec_send_packet(decode_ctx, dec_pkt);
                    int ret2 = avcodec_receive_frame(decode_ctx, av_frame);
                    av_packet_unref(dec_pkt);
                    //System.out.printf("decode: ret1 %d ret2 %d\n", ret1, ret2);
                    
                    if (ret2 == 0)
                    {                
                        // init scaler and encoder once we decode the 1st frame
                        if (sws_ctx == null )
                        {
                            // determine frame size
                            if (frameWidthString == null && frameHeightString == null)
                            {
                                frameWidth = (int)Math.round(av_frame.width() * frameSizeScale);
                                frameHeight = (int)Math.round(av_frame.height() * frameSizeScale);
                            }
                            else if (frameWidthString != null && frameHeightString == null)
                            {
                                frameHeight = (int)Math.round(frameWidth * ((double)av_frame.height()/av_frame.width()));
                            }
                            else if (frameWidthString == null && frameHeightString != null)
                            {
                                frameWidth = (int)Math.round(frameHeight * ((double)av_frame.width()/av_frame.height()));
                            }
                            
                            // make sure frame dimensions are multiple of 2
                            if (frameWidth % 2 != 0)
                                frameWidth++;
                            if (frameHeight % 2 != 0)
                                frameHeight++;
                            
                            // init scaler
                            sws_frame.format(AV_PIX_FMT_YUV420P);
                            sws_frame.width(frameWidth);
                            sws_frame.height(frameHeight);
                            av_image_alloc(sws_frame.data(), sws_frame.linesize(), 
                                    frameWidth, frameHeight, AV_PIX_FMT_YUV420P, 1);
                            
                            sws_ctx = sws_getContext(av_frame.width(), av_frame.height(), AV_PIX_FMT_YUV420P,
                                    frameWidth, frameHeight, AV_PIX_FMT_YUV420P, SWS_BICUBIC, null, null, (double[])null);
                                                    
                            log.debug("Resizing {}x{} -> {}x{}", av_frame.width(), av_frame.height(), frameWidth, frameHeight);
                            log.debug("Target bitrate = {}", bitrate);
                            
                            // init encoder
                            AVRational timeBase = new AVRational();
                            timeBase.num(1);
                            timeBase.den(fps);
                            encode_ctx.time_base(timeBase);
                            encode_ctx.width(frameWidth);
                            encode_ctx.height(frameHeight);
                            encode_ctx.pix_fmt(encoder.pix_fmts().get(0));
                            encode_ctx.bit_rate(bitrate);
                            //encode_ctx.sample_rate(fps);
                            av_opt_set(encode_ctx.priv_data(), "preset", "ultrafast", 0);
                            av_opt_set(encode_ctx.priv_data(), "tune", "zerolatency", 0);
                            if (avcodec_open2(encode_ctx, encoder, (PointerPointer<?>)null) < 0) {
                                throw new IllegalStateException("Error initializing encoder for codec " + codecConfig.encoderName);
                            }
                        }
                        
                        // scale frame to desired resolution (width/height)
                        sws_scale(sws_ctx, av_frame.data(), av_frame.linesize(), 0, av_frame.height(), sws_frame.data(), sws_frame.linesize());
                        
                        // encode
                        av_frame.pts(pts++);
                        ret1 = avcodec_send_frame(encode_ctx, sws_frame);
                        //System.out.printf("encode: ret1 %d\n", ret1);
                        //System.out.println("EAGAIN=" + avutil.AVERROR_EAGAIN());
                        //System.out.println("ENOMEM=" + avutil.AVERROR_ENOMEM());
                        
                        while (ret1 >= 0)
                        {
                            //av_init_packet(enc_pkt);
                            ret1 = avcodec_receive_packet(encode_ctx, enc_pkt);
                            //System.out.printf("encode: ret1 %d ret2 %d\n", ret1, ret2);
                            
                            if (ret1 == 0)
                            {
                                // repackage in datablock
                                //System.out.println("encoded pkt size = " + enc_pkt.size());
                                //frameData = enc_pkt.data().getStringBytes();
                                frameData = new byte[enc_pkt.size()];
                                enc_pkt.data().get(frameData);
                                frameBlk.setUnderlyingObject(frameData);                
                                                
                                // write record to output stream
                                writer.write(nextRecord);
                                writer.flush();
                            }
                            
                            av_packet_unref(enc_pkt);
                        }
                    }
                }
            }
            catch (EOFException e)
            {
                // this happens if output stream is closed by client
                // we stop silently in that case
            }
            catch (Exception e)
            {
                throw new IOException("Error while transcoding video data", e);
            }
        }
        finally
        {
            if (dec_pkt != null) {
                av_packet_unref(dec_pkt);
                av_packet_free(dec_pkt);
            }
            if (enc_pkt != null) {
                av_packet_unref(enc_pkt);
                av_packet_free(enc_pkt);
            }
            if (av_frame != null) {
                //av_freep(av_frame.data());
                av_frame_free(av_frame);
            }
            if (sws_frame != null) {
                av_freep(sws_frame.data());
                av_frame_free(sws_frame);
            }
            if (decode_ctx != null) {
                avcodec_close(decode_ctx);
                avcodec_free_context(decode_ctx);
            }
            if (encode_ctx != null) {
                avcodec_close(encode_ctx);
                avcodec_free_context(encode_ctx);
            }
            if (sws_ctx != null) {
                sws_freeContext(sws_ctx);
            }
        }
    }
}
