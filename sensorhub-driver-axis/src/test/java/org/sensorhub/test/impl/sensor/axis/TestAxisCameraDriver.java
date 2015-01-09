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

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.UUID;

import javax.swing.JFrame;

import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataChoice;
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
import org.vast.data.DataChoiceImpl;
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
    final static int MAX_FRAMES = 3000;
	AxisCameraDriver driver;
    AxisCameraConfig config;
    int actualWidth, actualHeight;
    int dataBlockSize;
    JFrame videoWindow;
    BufferedImage img;
    int frameCount;
    
    
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
    
    
    private void initWindow() throws Exception
    {
    	// prepare frame and buffered image
    	ISensorDataInterface di = driver.getObservationOutputs().get("videoOutput");
        int height = di.getRecordDescription().getComponent(1).getComponentCount();
        int width = di.getRecordDescription().getComponent(1).getComponent(0).getComponentCount();
        videoWindow = new JFrame("Video");
        videoWindow.setSize(width, height);
        videoWindow.setVisible(true);
        img = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
    }
    
    
    @Test
    public void testVideoCapture() throws Exception
    {
    	initWindow();
    	
    	// register listener on data interface
        ISensorDataInterface di = driver.getObservationOutputs().get("videoOutput");
    	di.registerListener(this);
        
        // start capture and wait until we receive the first frame
        synchronized (this)
        {
            while (frameCount < MAX_FRAMES)
            	this.wait();
            driver.stop();
        }
        
        assertEquals("Wrong image width", 704, actualWidth);
        assertEquals("Wrong image height", 480, actualHeight);
        assertEquals("Wrong data size", 704*480*3 + 1, dataBlockSize); // size of datablock is image+timestamp
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
        
        
    }
    
    
    @Test
    public void testSendPTZCommand() throws Exception
    {
    	initWindow();
    	
    	// register listeners
    	ISensorDataInterface di = driver.getObservationOutputs().get("ptzOutput");
        di.registerListener(this);
        ISensorDataInterface di2 = driver.getObservationOutputs().get("videoOutput");
        di2.registerListener(this);
        
        // get ptz control interface
        ISensorControlInterface ci = driver.getCommandInputs().get("ptzControl");
        DataComponent commandDesc = ci.getCommandDescription().copy();
        
        // start capture and send commands
        synchronized (this)
        {
        	float pan = 0.0f;
        	DataBlock commandData;
        	
        	while (frameCount < MAX_FRAMES)
        	{
        		if (frameCount % 30 == 0)
        		{
        			((DataChoiceImpl)commandDesc).setSelectedItem("pan");
        			commandData = commandDesc.createDataBlock();
        			pan += 5.;
        			if (pan > 180.)
        				pan -= 360;
        			commandData.setFloatValue(1, pan);
        			ci.execCommand(commandData);
        		}                
                
        		this.wait();
        	}
        	
            driver.stop();
        }
    }
    
    
    @Override
    public void handleEvent(Event e)
    {
        assertTrue(e instanceof SensorDataEvent);
        SensorDataEvent newDataEvent = (SensorDataEvent)e;
        
        if (newDataEvent.getSource().getClass().equals(AxisVideoOutput.class))
        {
	        DataComponent camDataStruct = newDataEvent.getRecordDescription().copy();
	        camDataStruct.setData(newDataEvent.getRecords()[0]);
	        
	        actualHeight = camDataStruct.getComponent(1).getComponentCount();
	        actualWidth = camDataStruct.getComponent(1).getComponent(0).getComponentCount();
	        dataBlockSize = newDataEvent.getRecords()[0].getAtomCount();
	        		
	        //System.out.println("New data received from sensor " + newDataEvent.getSensorId());
	        //System.out.println("Image is " + actualWidth + "x" + actualHeight);
	        
	        byte[] srcArray = (byte[])camDataStruct.getComponent(1).getData().getUnderlyingObject();
	        byte[] destArray = ((DataBufferByte)img.getRaster().getDataBuffer()).getData();
	        System.arraycopy(srcArray, 0, destArray, 0, dataBlockSize-1);
            videoWindow.getContentPane().getGraphics().drawImage(img, 0, 0, null);
            
            frameCount++;
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
