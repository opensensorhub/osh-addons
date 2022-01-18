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

package org.sensorhub.test.impl.sensor.simuav;

import java.io.IOException;
import java.util.UUID;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataComponent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.impl.sensor.simuav.SimUavConfig;
import org.sensorhub.impl.sensor.simuav.SimUavDriver;
import org.sensorhub.api.command.CommandData;
import org.sensorhub.api.command.CommandStatusEvent;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.api.data.DataEvent;
import org.vast.data.TextEncodingImpl;
import org.vast.sensorML.SMLUtils;
import org.vast.swe.AsciiDataWriter;
import org.vast.swe.SWEUtils;
import static org.junit.Assert.*;


public class TestSimUavDriver
{
    SimUavDriver driver;
    SimUavConfig config;

    
    @Before
    public void init() throws Exception
    {
        config = new SimUavConfig();
        config.id = UUID.randomUUID().toString();
        
        driver = new SimUavDriver();
        driver.init(config);
    }
    
    
    @Test
    public void testGetOutputDesc() throws Exception
    {
        for (IStreamingDataInterface di: driver.getObservationOutputs().values())
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
    public void testSendCommands() throws Exception
    {
        System.out.println();
        
        // register listener and writer on pos output
        var gpsOutput = driver.getObservationOutputs().get("platform_pos");
        var posWriter = new AsciiDataWriter();
        posWriter.setDataEncoding(new TextEncodingImpl(",", "\n"));
        posWriter.setDataComponents(gpsOutput.getRecordDescription());
        posWriter.setOutput(System.out);
        gpsOutput.registerListener(e -> {
            assertTrue(e instanceof DataEvent);
            var dataEvent = (DataEvent)e;
            
            try
            {
                System.out.println("\nNew position data received:");
                posWriter.write(dataEvent.getRecords()[0]);
                posWriter.flush();
            }
            catch (IOException e1)
            {
                e1.printStackTrace();
            }
        });
        
        // register listener on vehicle command status
        var vehicleControl = driver.getCommandInputs().get("vehicle_control");
        vehicleControl.registerListener(e -> {
            assertTrue(e instanceof CommandStatusEvent);
            var statusEvent = (CommandStatusEvent)e;
            System.out.println("\nNew status report:");
            System.out.println(statusEvent.getStatus());
        });
        
        driver.start();
        
        // queue vehicle commands
        var cmdRecord = (DataChoice)vehicleControl.getCommandDescription().copy();
        DataBlock cmd;
        
        // takeoff
        cmdRecord.setSelectedItem("AUTO_TAKEOFF");
        cmd = cmdRecord.createDataBlock();
        cmd.setDoubleValue(1, 5);
        vehicleControl.submitCommand(new CommandData.Builder()
            .withId(1)
            .withCommandStream(1)
            .withParams(cmd)
            .build());
        
        // goto waypoint
        cmdRecord.setSelectedItem("WAYPOINT");
        cmd = cmdRecord.createDataBlock();
        cmd.setDoubleValue(1, 0.001);
        cmd.setDoubleValue(2, 0.0);
        cmd.setDoubleValue(3, 30.0);
        cmd.setDoubleValue(4, 2.5);
        vehicleControl.submitCommand(new CommandData.Builder()
            .withId(3)
            .withCommandStream(1)
            .withParams(cmd)
            .build());
        
        Thread.sleep(100000);
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
