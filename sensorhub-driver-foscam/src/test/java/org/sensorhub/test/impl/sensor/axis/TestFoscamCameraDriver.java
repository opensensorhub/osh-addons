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
import org.sensorhub.impl.sensor.foscam.FoscamConfig;
import org.sensorhub.impl.sensor.foscam.FoscamDriver;
import org.sensorhub.impl.sensor.foscam.FoscamVideoOutput;
import org.sensorhub.impl.sensor.rtpcam.RTPCameraConfig;
import org.sensorhub.impl.sensor.rtpcam.RTPCameraDriver;
import org.sensorhub.test.sensor.videocam.VideoTestHelper;
import org.vast.data.DataChoiceImpl;
import org.vast.sensorML.SMLUtils;
import org.vast.swe.AsciiDataWriter;
import org.vast.swe.SWEUtils;
import org.vast.util.DateTimeFormat;

import static org.junit.Assert.*;


/**
 * <p>
 * Implementation of sensor interface for generic Axis Cameras using IP
 * protocol
 * </p>
 * 
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 */
public class TestFoscamCameraDriver implements IEventListener
{
	FoscamDriver driver;
	FoscamConfig config;
	int sampleCount = 0;
    
    @Before
    public void init() throws Exception
    {
        config = new FoscamConfig();
        config.id = UUID.randomUUID().toString();
        config.cameraID = "foscam:001";
        config.video.backupFile = "/home/butler/foscam/test-foscam.h264";
        config.rtsp.remoteHost = "192.168.0.34";
        config.rtsp.user = "osh";
        config.rtsp.password = "osh";
        config.rtsp.remotePort = 88;
        config.rtsp.videoPath = "/videoMain";        
        config.rtsp.localUdpPort = 5600;

        driver = new FoscamDriver();
        driver.init(config);      
    }
    
    
    @Test
    public void testGetOutputDesc() throws Exception
    {
        for (ISensorDataInterface di: driver.getObservationOutputs().values())
        {
            System.out.println();
            DataComponent dataMsg = di.getRecordDescription();
            new SWEUtils(SWEUtils.V2_0).writeComponent(System.out, dataMsg, false, true);
        }
    }
    
    @Test
    public void testGetSensorDesc() throws Exception
    {
        System.out.println();
        AbstractProcess smlDesc = driver.getCurrentDescription();
        new SMLUtils(SWEUtils.V2_0).writeProcess(System.out, smlDesc, true);
    }
    
    
//    @Test
//    public void testPresetPTZCommand() throws Exception
//    {   
//        // get ptz control interface
//        ISensorControlInterface ci = driver.getCommandInputs().get("ptzControl");
//        DataComponent commandDesc = ci.getCommandDescription().copy();
//        DataBlock commandData;
//        
//        // test preset
//        ((DataChoiceImpl)commandDesc).setSelectedItem("preset");
//        commandData = commandDesc.createDataBlock();
//        commandData.setStringValue(1, "Reset");
//        ci.execCommand(commandData);
//        driver.stop();
//    }
    
    
    @Test
    public void testVideoOutput() throws Exception
    {        
        ISensorDataInterface camOutput = driver.getObservationOutputs().get("video");
        camOutput.registerListener(this);
        
        driver.start();
        
        synchronized (this) 
        {
            while (sampleCount < 20000)
                wait();
        }
        
        System.out.println();
    }
    

    
    @Test
    public void testRelPTZCommand() throws Exception
    {   
        // get ptz control interface
        ISensorControlInterface ci = driver.getCommandInputs().get("ptzControl");
        DataComponent commandDesc = ci.getCommandDescription().copy();
        DataBlock commandData;
        
        // test relative movement
        ((DataChoiceImpl)commandDesc).setSelectedItem("relMove");
        commandData = commandDesc.createDataBlock();
        commandData.setStringValue(1, "Right");
        ci.execCommand(commandData);
        driver.stop();
    }
    
    
    @Override
    public void handleEvent(Event<?> e)
    {
        assertTrue(e instanceof SensorDataEvent);
        SensorDataEvent newDataEvent = (SensorDataEvent)e;
        
        double timeStamp = newDataEvent.getRecords()[0].getDoubleValue(0);
        System.out.println("Frame received on " + new DateTimeFormat().formatIso(timeStamp, 0));
        sampleCount++;
        
        synchronized (this) { this.notify(); }
    }
    
    @After
    public void cleanup()
    {
        driver.stop();
    }
}
