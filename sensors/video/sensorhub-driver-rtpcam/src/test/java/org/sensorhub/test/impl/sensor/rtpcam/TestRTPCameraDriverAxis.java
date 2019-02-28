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

import java.util.UUID;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.DataComponent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.common.Event;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.rtpcam.RTPCameraConfig;
import org.sensorhub.impl.sensor.rtpcam.RTPCameraDriver;
import org.vast.sensorML.SMLUtils;
import org.vast.swe.AsciiDataWriter;
import org.vast.swe.SWEUtils;
import org.vast.util.DateTimeFormat;
import static org.junit.Assert.*;


public class TestRTPCameraDriverAxis implements IEventListener
{
    RTPCameraDriver driver;
    RTPCameraConfig config;
    AsciiDataWriter writer;
    int sampleCount = 0;

    
    @Before
    public void init() throws Exception
    {
        config = new RTPCameraConfig();
        config.id = UUID.randomUUID().toString();
        config.cameraID = "axis:001";
        config.video.backupFile = "/home/alex/test-axis.h264";
        config.rtsp.remoteHost = "192.168.0.24";
        //config.rtsp.user = "admin";
        //config.rtsp.password = "op3nsaysam3";
        config.rtsp.remotePort = 554;
        config.rtsp.videoPath = "/axis-media/media.amp?videocodec=h264";        
        config.rtsp.localUdpPort = 20000;
        
        driver = new RTPCameraDriver();
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
    
    
    @Test
    public void testSendMeasurements() throws Exception
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
