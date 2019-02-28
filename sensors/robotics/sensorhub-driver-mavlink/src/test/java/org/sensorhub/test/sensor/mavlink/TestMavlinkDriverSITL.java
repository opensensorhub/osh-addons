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
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataComponent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.common.Event;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.ISensorControlInterface;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.comm.UDPCommProviderConfig;
import org.sensorhub.impl.comm.UDPConfig;
import org.sensorhub.impl.sensor.mavlink.MavlinkConfig;
import org.sensorhub.impl.sensor.mavlink.MavlinkConfig.CmdTypes;
import org.sensorhub.impl.sensor.mavlink.MavlinkConfig.MsgTypes;
import org.sensorhub.impl.sensor.mavlink.MavlinkDriver;
import org.vast.data.TextEncodingImpl;
import org.vast.sensorML.SMLUtils;
import org.vast.swe.AsciiDataWriter;
import org.vast.swe.SWEUtils;
import static org.junit.Assert.*;


public class TestMavlinkDriverSITL implements IEventListener
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
        config.activeCommands = EnumSet.of(
                CmdTypes.TAKEOFF,
                CmdTypes.GOTO_LLA,
                CmdTypes.GOTO_ENU,
                CmdTypes.VELOCITY,
                CmdTypes.HEADING,
                CmdTypes.LOITER,
                CmdTypes.ORBIT,
                CmdTypes.RTL,
                CmdTypes.LAND);

        // TCP conf to connect directly to SITL
        /*TCPCommProviderConfig tcpConf = new TCPCommProviderConfig();
        tcpConf.protocol.remoteHost = "localhost";
        tcpConf.protocol.remotePort = 5760;
        config.commSettings = tcpConf;*/
        
        // UDP conf to connect through MAVProxy
        /*UDPCommProviderConfig udpConf = new UDPCommProviderConfig();
        udpConf.protocol.remoteHost = "localhost";
        udpConf.protocol.remotePort = UDPConfig.PORT_AUTO;
        udpConf.protocol.localPort = 14551;
        config.commSettings = udpConf;*/
        
        // UDP conf to connect to SOLO
        UDPCommProviderConfig udpConf = new UDPCommProviderConfig();
        udpConf.protocol.remoteHost = "10.1.1.10";
        udpConf.protocol.remotePort = 14560;
        udpConf.protocol.localPort = 14550;
        config.commSettings = udpConf;
        
        driver = new MavlinkDriver();
        driver.init(config);
        driver.start();
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
    public void testTakeOffCommand() throws Exception
    {
        ISensorControlInterface navControl = driver.getCommandInputs().get("navCommands");
        DataChoice cmdChoice = (DataChoice)navControl.getCommandDescription().copy();
        cmdChoice.assignNewDataBlock();
        cmdChoice.setSelectedItem(CmdTypes.TAKEOFF.name());
        cmdChoice.getSelectedItem().getData().setFloatValue(20.0f);
        navControl.execCommand(cmdChoice.getData());
    }
    
    
    @Test
    public void testLoiterCommand() throws Exception
    {
        testTakeOffCommand();
        Thread.sleep(5000);
        
        ISensorControlInterface navControl = driver.getCommandInputs().get("navCommands");
        DataChoice cmdChoice = (DataChoice)navControl.getCommandDescription().copy();
        cmdChoice.assignNewDataBlock();
        cmdChoice.setSelectedItem(CmdTypes.LOITER.name());
        cmdChoice.getSelectedItem().getData().setFloatValue(0, -35.37f); // lat
        cmdChoice.getSelectedItem().getData().setFloatValue(1, 149.161f); // lon
        cmdChoice.getSelectedItem().getData().setFloatValue(2, 20.0f); // alt
        navControl.execCommand(cmdChoice.getData());
    }
    
    
    @Test
    public void testGotoLLACommand() throws Exception
    {
        //testTakeOffCommand();
        //Thread.sleep(5000);
        
        ISensorControlInterface navControl = driver.getCommandInputs().get("navCommands");
        DataChoice cmdChoice = (DataChoice)navControl.getCommandDescription().copy();
        cmdChoice.assignNewDataBlock();
        cmdChoice.setSelectedItem(CmdTypes.GOTO_LLA.name());
        cmdChoice.getSelectedItem().getData().setFloatValue(0, -35.3697f); // lat
        cmdChoice.getSelectedItem().getData().setFloatValue(1, 149.161f); // lon
        cmdChoice.getSelectedItem().getData().setFloatValue(2, 50.0f); // alt
        cmdChoice.getSelectedItem().getData().setFloatValue(3, 90.0f); // yaw
        navControl.execCommand(cmdChoice.getData());
    }
    
    
    @Test
    public void testGotoENUCommand() throws Exception
    {
        //testTakeOffCommand();
        //Thread.sleep(5000);
        
        ISensorControlInterface navControl = driver.getCommandInputs().get("navCommands");
        DataChoice cmdChoice = (DataChoice)navControl.getCommandDescription().copy();
        cmdChoice.assignNewDataBlock();
        cmdChoice.setSelectedItem(CmdTypes.GOTO_ENU.name());
        cmdChoice.getSelectedItem().getData().setFloatValue(0, 0.0f); // x
        cmdChoice.getSelectedItem().getData().setFloatValue(1, 0.0f); // y
        cmdChoice.getSelectedItem().getData().setFloatValue(2, 20.0f); // z
        cmdChoice.getSelectedItem().getData().setFloatValue(3, 90.0f); // yaw
        navControl.execCommand(cmdChoice.getData());
    }
    
    
    @Test
    public void testVelocityCommand() throws Exception
    {
        //testTakeOffCommand();
        //Thread.sleep(5000);
        
        for (int i=0; i<30; i++)
        {
            ISensorControlInterface navControl = driver.getCommandInputs().get("navCommands");
            DataChoice cmdChoice = (DataChoice)navControl.getCommandDescription().copy();
            cmdChoice.assignNewDataBlock();
            cmdChoice.setSelectedItem(CmdTypes.VELOCITY.name());
            cmdChoice.getSelectedItem().getData().setFloatValue(0, 0.0f); // vx
            cmdChoice.getSelectedItem().getData().setFloatValue(1, 5.0f); // vy
            cmdChoice.getSelectedItem().getData().setFloatValue(2, 0.0f); // vz
            navControl.execCommand(cmdChoice.getData());
            Thread.sleep(500);
        }
    }
    
    
    @Test
    public void testHeadingCommand() throws Exception
    {
        //testTakeOffCommand();
        //Thread.sleep(5000);
        
        float yaw = 0;
        for (int i=0; i<30; i++)
        {
            ISensorControlInterface navControl = driver.getCommandInputs().get("navCommands");
            DataChoice cmdChoice = (DataChoice)navControl.getCommandDescription().copy();
            cmdChoice.assignNewDataBlock();
            cmdChoice.setSelectedItem(CmdTypes.HEADING.name());
            cmdChoice.getSelectedItem().getData().setFloatValue(0, yaw); // yaw
            cmdChoice.getSelectedItem().getData().setFloatValue(1, 10.0f); // yaw rate
            navControl.execCommand(cmdChoice.getData());
            Thread.sleep(500);
            yaw += 10;
        }
    }
    
    
    @Test
    public void testRtlCommand() throws Exception
    {
        ISensorControlInterface navControl = driver.getCommandInputs().get("navCommands");
        DataChoice cmdChoice = (DataChoice)navControl.getCommandDescription().copy();
        cmdChoice.assignNewDataBlock();
        cmdChoice.setSelectedItem(CmdTypes.RTL.name());
        cmdChoice.getSelectedItem().getData().setBooleanValue(true);
        navControl.execCommand(cmdChoice.getData());
    }
    
    
    @Test
    public void testLandCommand() throws Exception
    {
        ISensorControlInterface navControl = driver.getCommandInputs().get("navCommands");
        DataChoice cmdChoice = (DataChoice)navControl.getCommandDescription().copy();
        cmdChoice.assignNewDataBlock();
        cmdChoice.setSelectedItem(CmdTypes.LAND.name());
        cmdChoice.getSelectedItem().getData().setFloatValue(0, -35.37f); // lat
        cmdChoice.getSelectedItem().getData().setFloatValue(1, 149.161f); // lon
        navControl.execCommand(cmdChoice.getData());
    }
    
    
    @Test
    public void testReceiveMessages() throws Exception
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
