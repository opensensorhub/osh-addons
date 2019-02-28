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

import java.util.UUID;
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
import org.sensorhub.test.sensor.videocam.VideoTestHelper;
import org.vast.data.DataValue;
import org.vast.sensorML.SMLUtils;
import org.vast.swe.SWEUtils;
import static org.junit.Assert.*;


public class TestV4LCameraDriver implements IEventListener
{
    final static int MAX_FRAMES = 100;
    V4LCameraDriver driver;
    V4LCameraConfig config;
    int actualWidth, actualHeight;
    int frameCount;
    VideoTestHelper videoTestHelper = new VideoTestHelper();
    
    
    @Before
    public void init() throws Exception
    {
        config = new V4LCameraConfig();
        config.deviceName = "/dev/video0";
        config.id = UUID.randomUUID().toString();
        config.autoStart = true;
        
        driver = new V4LCameraDriver();
        driver.setConfiguration(config);
        driver.requestInit(false);
        driver.requestStart();
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
    
    
    @Test
    public void testCaptureAtDefaultRes() throws Exception
    {
        // register listener on data interface
        ISensorDataInterface di = driver.getObservationOutputs().values().iterator().next();
        assertTrue("No video output", di != null);
        di.registerListener(this);
        videoTestHelper.initWindow(di);
        startCapture();
        
        // start capture and wait until we receive the first frame
        synchronized (this)
        {
            while (frameCount < MAX_FRAMES)
                this.wait();
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
        
        System.out.format("New frame received @ %d\n", e.getTimeStamp());
        System.out.println("Image is " + actualWidth + "x" + actualHeight);
        
        videoTestHelper.renderFrameJPEG(newDataEvent.getRecords()[0]);
        frameCount++;
        
        synchronized (this) { this.notify(); }
    }
    
    
    @After
    public void cleanup()
    {
        videoTestHelper.dispose();
        
        if (driver != null)
            driver.stop();
    }
}
