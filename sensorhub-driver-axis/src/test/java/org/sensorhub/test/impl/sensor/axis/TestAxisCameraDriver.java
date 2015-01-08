/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.impl.sensor.axis;

import java.util.UUID;

import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.DataComponent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.common.Event;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.sensor.ISensorControlInterface;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.axis.AxisCameraConfig;
import org.sensorhub.impl.sensor.axis.AxisCameraDriver;
import org.sensorhub.impl.sensor.axis.AxisSettingsOutput;
import org.sensorhub.impl.sensor.axis.AxisVideoOutput;
import org.vast.sensorML.SMLUtils;
import org.vast.swe.SWECommonUtils;

import static org.junit.Assert.*;

/**
 * <p>
 * Implementation of sensor interface for generic Axis Cameras using IP
 * protocol
 * </p>
 *
 * <p>
 * Copyright (c) 2014
 * </p>
 * 
 * @author Mike Botts <mike.botts@botts-inc.com>
 * @since October 30, 2014
 */


public class TestAxisCameraDriver implements IEventListener
{
    AxisCameraDriver driver;
    AxisCameraConfig config;
    int actualWidth, actualHeight; 
    
    
    @Before
    public void init() throws Exception
    {
        config = new AxisCameraConfig();
        //config.ipAddress = "root:more4less@192.168.1.50";
        //config.ipAddress = "192.168.1.50";
        config.ipAddress = "192.168.1.60";
        config.id = UUID.randomUUID().toString();
        
        driver = new AxisCameraDriver();
        driver.init(config);
        driver.start();
    }
    
    
    @Test
    public void testGetOutputDesc() throws Exception
    {
        for (ISensorDataInterface di: driver.getObservationOutputs().values())
        {
            DataComponent dataMsg = di.getRecordDescription();
            new SWECommonUtils().writeComponent(System.out, dataMsg, false, true);
        }
    }
    
    
    @Test
    public void testGetCommandDesc() throws Exception
    {
        for (ISensorControlInterface ci: driver.getCommandInputs().values())
        {
            DataComponent commandMsg = ci.getCommandDescription();
            new SWECommonUtils().writeComponent(System.out, commandMsg, false, true);
        }
    }
    
    
    @Test
    public void testGetSensorDesc() throws Exception
    {
        AbstractProcess smlDesc = driver.getCurrentSensorDescription();
        new SMLUtils().writeProcess(System.out, smlDesc, true);
    }
    
    
    @Test
    public void testCaptureAtDefaultRes() throws Exception
    {
        // register listener on data interface
        ISensorDataInterface di = driver.getObservationOutputs().get("videoOutput");
        di.registerListener(this);
        
        // start capture and wait until we receive the first frame
        synchronized (this)
        {
            this.wait();
            driver.stop();
        }
        
        //assertTrue(actualWidth == config.defaultParams.imgWidth);
        //assertTrue(actualHeight == config.defaultParams.imgHeight);
    }
    
    
    @Test
    public void testPTZSettingsOutput() throws Exception
    {
        // register listener on data interface
        ISensorDataInterface di = driver.getObservationOutputs().get("ptzOutput");
        di.registerListener(this);
        
        // start capture and wait until we receive the first frame
        synchronized (this)
        {
            this.wait();
            driver.stop();
        }
        
        //assertTrue(actualWidth == config.defaultParams.imgWidth);
        //assertTrue(actualHeight == config.defaultParams.imgHeight);
    }
    
    
    /*@Test
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
    }*/
    
    
    /*@Test
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
    }*/
    
    
    @Override
    public void handleEvent(Event e)
    {
        assertTrue(e instanceof SensorDataEvent);
        SensorDataEvent newDataEvent = (SensorDataEvent)e;
        
        if (newDataEvent.getSource().getClass().equals(AxisVideoOutput.class))
        {
	        DataComponent camDataStruct = newDataEvent.getRecordDescription();
	        
	        actualWidth = camDataStruct.getComponent(1).getComponentCount();
	        actualHeight = camDataStruct.getComponentCount();
	        
	        System.out.println("New data received from sensor " + newDataEvent.getSensorId());
	        System.out.println("Image is " + actualWidth + "x" + actualHeight);
        }
        else if (newDataEvent.getSource().getClass().equals(AxisSettingsOutput.class))
        {
        	DataComponent ptzParams = newDataEvent.getRecordDescription().copy();
        	ptzParams.setData(newDataEvent.getRecords()[0]);
        	System.out.println(ptzParams);
        }
        
        synchronized (this) { this.notify(); }
    }
    
    
    @After
    public void cleanup()
    {
        driver.stop();
    }
}
