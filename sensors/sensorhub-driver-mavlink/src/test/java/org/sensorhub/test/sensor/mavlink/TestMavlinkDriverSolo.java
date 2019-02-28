/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.sensor.mavlink;

import java.io.IOException;
import java.util.EnumSet;
import java.util.UUID;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.DataComponent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.common.Event;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.comm.UDPCommProviderConfig;
import org.sensorhub.impl.sensor.mavlink.MavlinkConfig;
import org.sensorhub.impl.sensor.mavlink.MavlinkConfig.MsgTypes;
import org.sensorhub.impl.sensor.mavlink.MavlinkDriver;
import org.vast.data.TextEncodingImpl;
import org.vast.sensorML.SMLUtils;
import org.vast.swe.AsciiDataWriter;
import org.vast.swe.SWEUtils;
import static org.junit.Assert.*;


public class TestMavlinkDriverSolo implements IEventListener
{
    MavlinkDriver driver;
    MavlinkConfig config;
    AsciiDataWriter writer;
    int sampleCount = 0;

    
    @Before
    public void init() throws Exception
    {
        config = new MavlinkConfig();
        config.id = UUID.randomUUID().toString();
        config.activeMessages = EnumSet.of(
                MsgTypes.GLOBAL_POSITION,
                MsgTypes.ATTITUDE,
                MsgTypes.GIMBAL_REPORT);
        
        UDPCommProviderConfig udpConf = new UDPCommProviderConfig();
        udpConf.protocol.localPort = 14550;
        udpConf.protocol.remoteHost = "10.1.1.10";
        udpConf.protocol.remotePort = 14560;
        config.commSettings = udpConf;
        
        driver = new MavlinkDriver();
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
        System.out.println();
                
        writer = new AsciiDataWriter();
        writer.setDataEncoding(new TextEncodingImpl(",", "\n"));
        writer.setOutput(System.out);
        
        ISensorDataInterface attOutput = driver.getObservationOutputs().get("platformAtt");
        attOutput.registerListener(this);
        
        ISensorDataInterface locOutput = driver.getObservationOutputs().get("platformLoc");
        locOutput.registerListener(this);
        
        ISensorDataInterface gimbalOutput = driver.getObservationOutputs().get("gimbalAtt");
        gimbalOutput.registerListener(this);
        
        driver.start();
        
        synchronized (this) 
        {
            while (sampleCount < 3000)
                wait();
        }
        
        System.out.println();
    }
    
    
    @Override
    public void handleEvent(Event<?> e)
    {
        assertTrue(e instanceof SensorDataEvent);
        SensorDataEvent newDataEvent = (SensorDataEvent)e;
        
        try
        {
            //System.out.print("\nNew data received from sensor " + newDataEvent.getSensorId());
            writer.setDataComponents(newDataEvent.getSource().getRecordDescription());
            writer.reset();
            writer.write(newDataEvent.getRecords()[0]);
            writer.flush();
            
            sampleCount++;
        }
        catch (IOException e1)
        {
            e1.printStackTrace();
        }
                
        synchronized (this) { this.notify(); }
    }
    
    
    @After
    public void cleanup()
    {
        try
        {
            driver.stop();
        }
        catch (SensorHubException e)
        {
            e.printStackTrace();
        }
    }
}
