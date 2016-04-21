/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.impl.sensor.v4l;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.JFrame;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.common.Event;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.sensor.ISensorControlInterface;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.v4l.V4LCameraDriver;
import org.sensorhub.impl.sensor.v4l.V4LCameraConfig;
import org.vast.data.DataValue;
import org.vast.sensorML.SMLUtils;
import org.vast.swe.SWEUtils;
import static org.junit.Assert.*;


public class TestV4LCameraDriver implements IEventListener
{
    final static int MAX_FRAMES = 300;
    V4LCameraDriver driver;
    V4LCameraConfig config;
    int actualWidth, actualHeight;
    JFrame videoWindow;
    BufferedImage img;
    int frameCount;
    
    
    @Before
    public void init() throws Exception
    {
        config = new V4LCameraConfig();
        config.deviceName = "/dev/video0";
        config.id = UUID.randomUUID().toString();
        config.autoStart = true;
        
        driver = new V4LCameraDriver();
        driver.init(config);
        driver.start();
    }
    
    
    private void startCapture() throws Exception
    {
        // update config to start capture
        config.defaultParams.doCapture = true;
        driver.updateConfig(config);
    }
    
    
    @Test
    public void testGetOutputDesc() throws Exception
    {
        for (ISensorDataInterface di: driver.getObservationOutputs().values())
        {
            DataComponent dataMsg = di.getRecordDescription();
            new SWEUtils(SWEUtils.V2_0).writeComponent(System.out, dataMsg, false, true);
            
            DataEncoding dataEnc = di.getRecommendedEncoding();
            new SWEUtils(SWEUtils.V2_0).writeEncoding(System.out, dataEnc, true);
        }
    }
    
    
    @Test
    public void testGetCommandDesc() throws Exception
    {
        for (ISensorControlInterface ci: driver.getCommandInputs().values())
        {
            DataComponent commandMsg = ci.getCommandDescription();
            new SWEUtils(SWEUtils.V2_0).writeComponent(System.out, commandMsg, false, true);
        }
    }
    
    
    @Test
    public void testGetSensorDesc() throws Exception
    {
        AbstractProcess smlDesc = driver.getCurrentDescription();
        new SMLUtils(SMLUtils.V2_0).writeProcess(System.out, smlDesc, true);
    }
    
    
    private void initWindow() throws Exception
    {
        // prepare frame and buffered image
        ISensorDataInterface di = driver.getObservationOutputs().get("camOutput");
        int height = di.getRecordDescription().getComponent(1).getComponentCount();
        int width = di.getRecordDescription().getComponent(1).getComponent(0).getComponentCount();
        videoWindow = new JFrame("Video");
        videoWindow.setSize(width, height);
        videoWindow.setVisible(true);
        img = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
    }
    
    
    @Test
    public void testCaptureAtDefaultRes() throws Exception
    {
        initWindow();
        
        // register listener on data interface
        ISensorDataInterface di = driver.getObservationOutputs().values().iterator().next();
        di.registerListener(this);
        startCapture();
        
        // start capture and wait until we receive the first frame
        synchronized (this)
        {
            while (frameCount < MAX_FRAMES)
                this.wait();
            driver.stop();
        }
        
        assertTrue(actualWidth == config.defaultParams.imgWidth);
        assertTrue(actualHeight == config.defaultParams.imgHeight);
    }
    
    
    @Test
    public void testChangeParams() throws Exception
    {
        // register listener on data interface
        ISensorDataInterface di = driver.getObservationOutputs().values().iterator().next();
        di.registerListener(this);
        
        int expectedWidth = config.defaultParams.imgWidth = 320;
        int expectedHeight = config.defaultParams.imgHeight = 240;
        
        // start capture and wait until we receive the first frame
        synchronized (this)
        {
            startCapture();
            this.wait();
        }
        
        assertTrue(actualWidth == expectedWidth);
        assertTrue(actualHeight == expectedHeight);
    }
    
    
    @Test
    public void testSendCommand() throws Exception
    {
        // register listener on data interface
        ISensorDataInterface di = driver.getObservationOutputs().values().iterator().next();
        di.registerListener(this);
        
        // start capture and wait until we receive the first frame
        synchronized (this)
        {            
            startCapture();
            this.wait();
        }        
        
        int expectedWidth = 160;
        int expectedHeight = 120;
        
        ISensorControlInterface ci = driver.getCommandInputs().values().iterator().next();
        DataBlock commandData = ci.getCommandDescription().createDataBlock();
        int fieldIndex = 0;
        commandData.setStringValue(fieldIndex++, "YUYV");
        if (((DataValue)ci.getCommandDescription().getComponent(1)).getDataType() != DataType.INT)
        {
            commandData.setStringValue(fieldIndex++, expectedWidth+"x"+expectedHeight);
        }
        else
        {
            commandData.setIntValue(fieldIndex++, expectedWidth);
            commandData.setIntValue(fieldIndex++, expectedHeight);
        }
        commandData.setIntValue(fieldIndex++, 10);
        
        // send command to control interface
        ci.execCommand(commandData);
        
        // start capture and wait until we receive the first frame
        // after we changed settings
        synchronized (this)
        {            
            this.wait();
        }
        
        assertTrue(actualWidth == expectedWidth);
        assertTrue(actualHeight == expectedHeight);
    }
    
    
    @Override
    public void handleEvent(Event<?> e)
    {
        assertTrue(e instanceof SensorDataEvent);
        SensorDataEvent newDataEvent = (SensorDataEvent)e;
        DataComponent camDataStruct = newDataEvent.getSource().getRecordDescription();
        
        actualWidth = camDataStruct.getComponent(1).getComponent(0).getComponentCount();
        actualHeight = camDataStruct.getComponent(1).getComponentCount();
        
        System.out.println("New data received from sensor " + newDataEvent.getSensorID());
        System.out.println("Image is " + actualWidth + "x" + actualHeight);
        
        camDataStruct.setData(newDataEvent.getRecords()[0]);
        byte[] frameData = (byte[])camDataStruct.getComponent(1).getData().getUnderlyingObject();
        
        /*// use RGB data directly
        byte[] destArray = ((DataBufferByte)img.getRaster().getDataBuffer()).getData();
        System.arraycopy(frameData, 0, destArray, 0, dataBlockSize-1);*/
        
        // uncompress JPEG data
        BufferedImage rgbImage;
        try
        {
            InputStream imageStream = new ByteArrayInputStream(frameData);                               
            ImageInputStream input = ImageIO.createImageInputStream(imageStream); 
            Iterator<ImageReader> readers = ImageIO.getImageReadersByMIMEType("image/jpeg");
            ImageReader reader = readers.next();
            reader.setInput(input);
            rgbImage = reader.read(0);
            videoWindow.getContentPane().getGraphics().drawImage(rgbImage, 0, 0, null);
        }
        catch (IOException e1)
        {
            throw new RuntimeException(e1);
        }
        
        frameCount++;
        
        synchronized (this) { this.notify(); }
    }
    
    
    @After
    public void cleanup()
    {
        if (videoWindow != null)
            videoWindow.dispose();
        
        if (driver != null)
            driver.stop();
    }
}
