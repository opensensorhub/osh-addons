/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.impl.sensor.rtpcam;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import javax.swing.JFrame;
import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.codecs.h264.MappedH264ES;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;


public class TestH264FileVideo
{
    static DatagramSocket socket;


    public static void main(String[] args) throws Exception
    {
        JFrame window = new JFrame();
        window.setSize(1080, 720);
        window.setVisible(true);
        
        MappedH264ES es = new MappedH264ES(NIOUtils.fetchFrom(new File( "/home/alex/test.h264")));
        //MappedH264ES es = new MappedH264ES(NIOUtils.fetchFrom(new File( "/home/alex/Projects/Workspace_OGC/h264j/sample_clips/admiral.264")));
        Picture out = Picture.create(1280, 720, ColorSpace.YUV420);
        H264Decoder decoder = new H264Decoder();
        decoder.setDebug(true);
        
        Packet packet;
        do
        {
            packet = es.nextFrame();
            if (packet != null)
            {
                ByteBuffer data = packet.getData();
                Picture res = decoder.decodeFrame(data, out.getData());
                BufferedImage bi = AWTUtil.toBufferedImage(res);
                window.getContentPane().getGraphics().drawImage(bi, 0, 0, null);
            }
        }
        while (packet != null);
        
        window.dispose();
    }

}
