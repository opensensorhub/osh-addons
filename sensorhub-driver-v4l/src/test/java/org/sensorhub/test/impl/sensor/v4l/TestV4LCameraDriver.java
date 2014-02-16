/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License Version
 1.1 (the "License"); you may not use this file except in compliance with
 the License. You may obtain a copy of the License at
 http://www.mozilla.org/MPL/MPL-1.1.html
 
 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.
 
 The Original Code is "SensorHub".
 
 The Initial Developer of the Original Code is Sensia Software LLC.
 <http://www.sensiasoftware.com>. Portions created by the Initial
 Developer are Copyright (C) 2013 the Initial Developer. All Rights Reserved.
 
 Please contact Alexandre Robin <alex.robin@sensiasoftware.com> for more 
 information.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.impl.sensor.v4l;

import java.io.File;
import java.util.UUID;
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
import org.vast.cdm.common.DataBlock;
import org.vast.cdm.common.DataComponent;
import org.vast.cdm.common.DataType;
import org.vast.data.DataValue;
import org.vast.sweCommon.SWECommonUtils;
import org.vast.xml.DOMHelper;
import org.w3c.dom.Element;
import static org.junit.Assert.*;


public class TestV4LCameraDriver implements IEventListener
{
    V4LCameraDriver driver;
    V4LCameraConfig config;
    int actualWidth, actualHeight;
    
    
    static
    {
        System.load(new File(new File("."), "/lib/libvideo.so.0").getAbsolutePath());
        System.load(new File(new File("."), "/lib/libv4l4j.so").getAbsolutePath());        
    }    
    
    
    @Before
    public void init() throws Exception
    {
        config = new V4LCameraConfig();
        config.deviceName = "/dev/video0";
        config.id = UUID.randomUUID().toString();
        
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
            DOMHelper dom = new DOMHelper();
            Element elt = new SWECommonUtils().writeComponent(dom, dataMsg);
            dom.serialize(elt, System.out, true);
        }
    }
    
    
    @Test
    public void testGetCommandDesc() throws Exception
    {
        for (ISensorControlInterface ci: driver.getCommandInputs().values())
        {
            DataComponent commandMsg = ci.getCommandDescription();
            DOMHelper dom = new DOMHelper();
            Element elt = new SWECommonUtils().writeComponent(dom, commandMsg);
            dom.serialize(elt, System.out, true);
        }
    }
    
    
    @Test
    public void testCaptureAtDefaultRes() throws Exception
    {
        // register listener on data interface
        ISensorDataInterface di = driver.getObservationOutputs().values().iterator().next();
        di.registerListener(this);
        
        // start capture and wait until we receive the first frame
        synchronized (this)
        {
            startCapture();
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
    public void handleEvent(Event e)
    {
        assertTrue(e instanceof SensorDataEvent);
        SensorDataEvent newDataEvent = (SensorDataEvent)e;
        DataComponent camDataStruct = newDataEvent.getRecordDescription();
        
        actualWidth = camDataStruct.getComponent(0).getComponentCount();
        actualHeight = camDataStruct.getComponentCount();
        
        System.out.println("New data received from sensor " + newDataEvent.getSensorId());
        System.out.println("Image is " + actualWidth + "x" + actualHeight);
        
        synchronized (this) { this.notify(); }
    }
    
    
    @After
    public void cleanup()
    {
        driver.stop();
    }
}
