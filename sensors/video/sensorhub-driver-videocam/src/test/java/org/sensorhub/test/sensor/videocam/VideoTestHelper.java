/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.sensor.videocam;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.JFrame;
import net.opengis.swe.v20.DataBlock;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.vast.data.DataBlockMixed;


public class VideoTestHelper
{
    JFrame videoWindow;
    BufferedImage img;
    
    
    public void initWindow(ISensorDataInterface videoOutput) throws Exception
    {
        // prepare frame and buffered image
        int height = videoOutput.getRecordDescription().getComponent(1).getComponentCount();
        int width = videoOutput.getRecordDescription().getComponent(1).getComponent(0).getComponentCount();
        videoWindow = new JFrame("Video");
        videoWindow.setSize(width, height);
        videoWindow.setVisible(true);
        img = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
    }
    
    
    public void renderFrameRGB(DataBlock data)
    {
        DataBlock frameBlock = ((DataBlockMixed)data).getUnderlyingObject()[1];
        byte[] frameData = (byte[])frameBlock.getUnderlyingObject();
        
        byte[] destArray = ((DataBufferByte)img.getRaster().getDataBuffer()).getData();
        System.arraycopy(frameData, 0, destArray, 0, frameData.length);
        videoWindow.getContentPane().getGraphics().drawImage(img, 0, 0, null);
    }
    
    
    public void renderFrameJPEG(DataBlock data)
    {
        if (videoWindow == null)
            return;
        
        DataBlock frameBlock = ((DataBlockMixed)data).getUnderlyingObject()[1];
        byte[] frameData = (byte[])frameBlock.getUnderlyingObject();
        
        // uncompress JPEG data
        try
        {
            InputStream imageStream = new ByteArrayInputStream(frameData);                               
            ImageInputStream input = ImageIO.createImageInputStream(imageStream); 
            Iterator<ImageReader> readers = ImageIO.getImageReadersByMIMEType("image/jpeg");
            ImageReader reader = readers.next();
            reader.setInput(input);
            BufferedImage rgbImage = reader.read(0);
            videoWindow.getContentPane().getGraphics().drawImage(rgbImage, 0, 0, null);
        }
        catch (IOException e1)
        {
            throw new RuntimeException(e1);
        }
    }
    
    
    public void renderFrameH264(DataBlock data)
    {
        
    }
    
    
    public void dispose()
    {
        if (videoWindow != null)
            videoWindow.dispose();
    }
}
