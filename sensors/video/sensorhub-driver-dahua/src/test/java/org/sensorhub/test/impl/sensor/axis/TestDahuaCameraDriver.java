/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Sensia Software LLC. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.impl.sensor.axis;

import java.awt.image.BufferedImage;
import java.util.UUID;
import javax.swing.JFrame;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.common.Event;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.sensor.ISensorControlInterface;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.security.ClientAuth;
import org.sensorhub.impl.sensor.dahua.DahuaCameraConfig;
import org.sensorhub.impl.sensor.dahua.DahuaCameraDriver;
import org.sensorhub.impl.sensor.dahua.DahuaPtzOutput;
import org.sensorhub.impl.sensor.dahua.DahuaVideoOutput;
import org.sensorhub.test.sensor.videocam.VideoTestHelper;
import org.vast.data.DataChoiceImpl;
import org.vast.sensorML.SMLUtils;
import org.vast.swe.SWEUtils;
import static org.junit.Assert.*;


/**
 * <p>
 * Implementation of sensor interface for generic Axis Cameras using IP
 * protocol
 * </p>
 * 
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 */
public class TestDahuaCameraDriver implements IEventListener
{
    final static int MAX_FRAMES = 600;
	DahuaCameraDriver driver;
    DahuaCameraConfig config;
    int actualWidth, actualHeight;
    int dataBlockSize;
    JFrame videoWindow;
    BufferedImage img;
    int frameCount;
    VideoTestHelper videoTestHelper = new VideoTestHelper();
    
    
    static
    {
        ClientAuth.createInstance("osh-keystore.dat");
    }
    
    
    @Before
    public void init() throws Exception
    {
        config = new DahuaCameraConfig();
        config.id = UUID.randomUUID().toString();
        config.connection.connectTimeout = 10000;
        
        config.http.remoteHost = "192.168.0.201";
        config.http.user = "admin";
        config.http.password = "op3nsaysam3";
        
        //config.net.remoteHost = "192.168.0.202";
        //config.net.user = "admin";
        //config.net.password = "admin";
        
        driver = new DahuaCameraDriver();
        driver.init(config);
        driver.start();
        
        assertTrue("Camera is not connected", driver.isConnected());        
    }
    
    
    @Test
    public void testGetOutputDesc() throws Exception
    {
        for (ISensorDataInterface di: driver.getObservationOutputs().values())
        {
            DataComponent dataMsg = di.getRecordDescription();
            new SWEUtils(SWEUtils.V2_0).writeComponent(System.out, dataMsg, false, true);
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
    
    
    /*private void initWindow() throws Exception
    {
    	// prepare frame and buffered image
    	ISensorDataInterface di = driver.getObservationOutputs().get("videoOutput");
        int height = di.getRecordDescription().getComponent(1).getComponentCount();
        int width = di.getRecordDescription().getComponent(1).getComponent(0).getComponentCount();
        videoWindow = new JFrame("Video");
        videoWindow.setSize(width, height);
        videoWindow.setVisible(true);
        img = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
    }*/
    
    
    @Test
    public void testVideoCapture() throws Exception
    {
        // register listener on data interface
        ISensorDataInterface di = driver.getObservationOutputs().get("video");
        assertTrue("No video output", di != null);
        di.registerListener(this);    	
    	//videoTestHelper.initWindow(di);
    	
        // start capture and wait until we receive the first frame
        synchronized (this)
        {
            while (frameCount < MAX_FRAMES)
            	this.wait();
            driver.stop();
        }
    }
    
    
    @Test
    public void testPTZSettingsOutput() throws Exception
    {
        // register listener on data interface
        ISensorDataInterface di = driver.getObservationOutputs().get("ptzOutput");
        assertTrue("No ptz output", di != null);
        di.registerListener(this);
        
        // start capture and wait until we receive the first record
        synchronized (this)
        {
            this.wait();
            driver.stop();
        }
    }
    
    
    @Test
    public void testSendPTZCommand() throws Exception
    {
        // register listeners
    	ISensorDataInterface di = driver.getObservationOutputs().get("ptzOutput");
    	assertTrue("No ptz output", di != null);
        di.registerListener(this);
        ISensorDataInterface di2 = driver.getObservationOutputs().get("video");
        assertTrue("No video output", di2 != null);
        di2.registerListener(this);
        //videoTestHelper.initWindow(di2);
        
        // get ptz control interface
        ISensorControlInterface ci = driver.getCommandInputs().get("ptzControl");
        DataComponent commandDesc = ci.getCommandDescription().copy();
        
        // start capture and send commands
        synchronized (this)
        {
        	float pan = 190.0f;
        	float tilt = 0.0f;
        	int zoom = 0;
        	DataBlock commandData;
        	
        	// NOTE: uncomment one section at a time to test different parameters
        	
        	// test absolute pan
//        	while (frameCount < MAX_FRAMES)  //
//        	{
//        		if (frameCount % 30 == 0)
//        		{
//        			((DataChoiceImpl)commandDesc).setSelectedItem("pan");
//        			commandData = commandDesc.createDataBlock();
//        			pan += 5.;
//        			if (pan > 180.)
//        				pan -= 360;
//        			commandData.setFloatValue(1, pan);
//        			ci.execCommand(commandData);
//        		}                               
//        		this.wait();
//        	}
        	
        	// test absolute tilt
//        	while (frameCount < MAX_FRAMES)
//        	{
//        		if (frameCount % 30 == 0)
//        		{
//        			((DataChoiceImpl)commandDesc).setSelectedItem("tilt");
//        			commandData = commandDesc.createDataBlock();       			
//        			tilt -= 2.;
//        			if (tilt > 180.)
//        				tilt -= 180.;
//        			commandData.setFloatValue(1, tilt);
//        			ci.execCommand(commandData);
//        		}                               
//        		this.wait();
//        	}
        	
        	
        	// test absolute zoom
//        	while (frameCount < MAX_FRAMES)
//        	{
//        		if (frameCount % 30 == 0)
//        		{
//        			((DataChoiceImpl)commandDesc).setSelectedItem("zoom");
//        			commandData = commandDesc.createDataBlock();       			
//        			zoom += 100;
//        			commandData.setFloatValue(1, zoom);
//        			ci.execCommand(commandData);
//        		}                                
//        		this.wait();
//        	}
        	
        	// test relative pan
        	while (frameCount < MAX_FRAMES)  //
        	{
        		if (frameCount % 30 == 0)
        		{
        			((DataChoiceImpl)commandDesc).setSelectedItem("rpan");
        			commandData = commandDesc.createDataBlock();
        			commandData.setFloatValue(1, 5.0f);
        			ci.execCommand(commandData);
        		}                               
        		this.wait();
        	}
       	
        	// test relative tilt
//        	while (frameCount < MAX_FRAMES)  //
//        	{
//        		if (frameCount % 30 == 0)
//        		{
//        			((DataChoiceImpl)commandDesc).setSelectedItem("rtilt");
//        			commandData = commandDesc.createDataBlock();
//        			commandData.setFloatValue(1, 5.0f);
//        			ci.execCommand(commandData);
//        		}                               
//        		this.wait();
//        	}

        	// test relative zoom
//        	while (frameCount < MAX_FRAMES)  //
//        	{
//        		if (frameCount % 30 == 0)
//        		{
//        			((DataChoiceImpl)commandDesc).setSelectedItem("rzoom");
//        			commandData = commandDesc.createDataBlock();
//        			commandData.setFloatValue(1, 10.0f);
//        			ci.execCommand(commandData);
//        		}                               
//        		this.wait();
//        	}

        	driver.stop();
        }
    }
    
    
    @Override
    public void handleEvent(Event<?> e)
    {
        assertTrue(e instanceof SensorDataEvent);
        SensorDataEvent newDataEvent = (SensorDataEvent)e;
        
        if (newDataEvent.getSource().getClass().equals(DahuaVideoOutput.class))
        {
	        System.out.println("Received Frame, Timestamp=" + newDataEvent.getRecords()[0].getDoubleValue(0));
	        videoTestHelper.renderFrameH264(newDataEvent.getRecords()[0]);            
            frameCount++;
        }
        else if (newDataEvent.getSource().getClass().equals(DahuaPtzOutput.class))
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
        videoTestHelper.dispose();
        driver.stop();
    }
}
