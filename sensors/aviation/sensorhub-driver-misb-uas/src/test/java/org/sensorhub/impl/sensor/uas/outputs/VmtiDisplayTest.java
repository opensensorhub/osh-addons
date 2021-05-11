/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.uas.outputs;

import org.sensorhub.impl.sensor.uas.UasSensor;
import org.sensorhub.impl.sensor.uas.config.UasConfig;
import org.vast.data.DataBlockCompressed;
import org.vast.data.DataBlockList;
import org.vast.data.DataBlockMixed;
import org.vast.swe.SWEHelper;
import net.opengis.swe.v20.DataBlock;
import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.swscale.SwsContext;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.PointerPointer;
import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.codecs.h264.MappedH264ES;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.event.IEventListener;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.api.data.DataEvent;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.TreeMap;
import javax.swing.JFrame;
import static org.bytedeco.ffmpeg.global.avcodec.av_init_packet;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_alloc;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_unref;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_alloc_context3;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_find_decoder_by_name;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_find_encoder_by_name;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_open2;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_receive_frame;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_send_packet;
import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_BGR24;
import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_YUV420P;
import static org.bytedeco.ffmpeg.global.avutil.av_frame_alloc;
import static org.bytedeco.ffmpeg.global.avutil.av_image_alloc;
import static org.bytedeco.ffmpeg.global.swscale.SWS_BICUBIC;
import static org.bytedeco.ffmpeg.global.swscale.sws_getContext;
import static org.bytedeco.ffmpeg.global.swscale.sws_scale;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class VmtiDisplayTest {

    
    public static void main(String[] args) throws Exception {

        int frameWidth = 640;
        int frameHeight = 480;
        
        final JFrame window = new JFrame();
        window.setSize(frameWidth, frameHeight);
        window.setVisible(true);
        window.setResizable(false);
        var graphicCtx = window.getContentPane().getGraphics();
                
        URL resource = UasSensor.class.getResource("sample-stream-original.ts");
        UasConfig config = new UasConfig();

        assert resource != null;
        config.connection.transportStreamPath = new File(resource.toURI()).getPath();
        config.outputs.enableTargetIndicators = true;
        config.outputs.enableVideo = true;

        UasSensor driver = new UasSensor();
        driver.setConfiguration(config);
        driver.init();
        driver.start();
        
        class Target {
            int x;
            int y;
        }
        
        TreeMap<Integer, Target> targets = new TreeMap<>();
        
        // register listener on VMTI output
        IStreamingDataInterface vmtiOutput = driver.getOutputs().get("TargetIndicators");
        var dataWriter = SWEHelper.createDataWriter(vmtiOutput.getRecommendedEncoding());
        dataWriter.setDataComponents(vmtiOutput.getRecordDescription());
        dataWriter.setOutput(System.out);

        vmtiOutput.registerListener(event -> {

            DataEvent newDataEvent = (DataEvent) event;
            DataBlock dataBlock = newDataEvent.getRecords()[0];
            
            try {
                dataWriter.write(dataBlock);
                dataWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            var frameNum = dataBlock.getIntValue(1);
            
            if (frameNum > 0) {
                targets.clear();
                
                var targetSeries = (DataBlockList)((DataBlockMixed)dataBlock).getUnderlyingObject()[3];
                for (int i = 0; i < targetSeries.getListSize(); i++) {
                    var target = targetSeries.get(i);
                    var id = target.getIntValue(0);
                    var x = target.getIntValue(1);
                    var y = target.getIntValue(2);
                                    
                    targets.compute(id, (k,v) -> {
                        if (v == null)
                            v = new Target();
                        v.x = x;
                        v.y = y;
                        return v;
                    });
                }
            }
        });
        
        
        // register listener on video output
        IStreamingDataInterface videoOutput = driver.getOutputs().get("UasVideo");
        
        // init decoder context
        AVCodec decoder = avcodec_find_decoder_by_name("h264");
        AVCodecContext decode_ctx = avcodec_alloc_context3(decoder);
        if (avcodec_open2(decode_ctx, decoder, (PointerPointer<?>)null) < 0) {
            throw new IllegalStateException("Error initializing H264 decoder");
        }
        
        // init scaler
        AVFrame sws_frame = av_frame_alloc();
        sws_frame.format(AV_PIX_FMT_BGR24);
        sws_frame.width(frameWidth);
        sws_frame.height(frameHeight);
        av_image_alloc(sws_frame.data(), sws_frame.linesize(), 
                frameWidth, frameHeight, AV_PIX_FMT_BGR24, 1);
        
        SwsContext sws_ctx = sws_getContext(frameWidth, frameHeight, AV_PIX_FMT_YUV420P,
                frameWidth, frameHeight, AV_PIX_FMT_BGR24, SWS_BICUBIC, null, null, (double[])null);
        
        // init packet and frame buffers
        AVPacket dec_pkt = av_packet_alloc();
        av_init_packet(dec_pkt);
        AVFrame av_frame = av_frame_alloc();
        BytePointer nativeFrameData = new BytePointer(50*1024);
        BufferedImage bi = new BufferedImage(frameWidth, frameHeight, BufferedImage.TYPE_3BYTE_BGR);
        videoOutput.registerListener(event -> {

            DataEvent newDataEvent = (DataEvent) event;
            DataBlock dataBlock = newDataEvent.getRecords()[0];
            var h264DataBlk = (DataBlockCompressed) ((DataBlockMixed)dataBlock).getUnderlyingObject()[1];
            var h264Data = h264DataBlk.getUnderlyingObject();
            
            nativeFrameData.position(0);
            nativeFrameData.limit(0);
            nativeFrameData.put(h264Data);
            
            // decode frame
            dec_pkt.data(nativeFrameData);
            dec_pkt.size(h264Data.length);
            int ret1 = avcodec_send_packet(decode_ctx, dec_pkt);
            int ret2 = avcodec_receive_frame(decode_ctx, av_frame);
            av_packet_unref(dec_pkt);
            //System.out.printf("decode: ret1 %d ret2 %d\n", ret1, ret2);
            
            if (ret2 == 0)
            {
                sws_scale(sws_ctx, av_frame.data(), av_frame.linesize(), 0, av_frame.height(), sws_frame.data(), sws_frame.linesize());
                var imgData = ((DataBufferByte)bi.getRaster().getDataBuffer()).getData();
                var buf = sws_frame.data(0).get(imgData);
                
                // draw image
                graphicCtx.setColor(Color.YELLOW);
                graphicCtx.drawImage(bi, 0, 0, null);
                
                // draw targets
                for (var target: targets.entrySet()) {
                    var id = target.getKey();
                    int x = target.getValue().x;
                    int y = target.getValue().y;
                    graphicCtx.drawRect(x-2, y-2, 4, 4);
                    var idText = (""+id).toCharArray();
                    graphicCtx.drawChars(idText, 0, idText.length, x, y-20);
                }
            }
        });
    }
}
