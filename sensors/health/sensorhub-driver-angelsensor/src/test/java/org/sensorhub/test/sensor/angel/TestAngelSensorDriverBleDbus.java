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

package org.sensorhub.test.sensor.angel;

import java.io.IOException;
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
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.comm.ble.dbus.BleDbusCommNetwork;
import org.sensorhub.impl.comm.ble.dbus.BluetoothNetworkConfig;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.sensor.angel.AngelSensorConfig;
import org.sensorhub.impl.sensor.angel.AngelSensor;
import org.vast.data.TextEncodingImpl;
import org.vast.sensorML.SMLUtils;
import org.vast.swe.AsciiDataWriter;
import org.vast.swe.SWEUtils;
import static org.junit.Assert.*;


public class TestAngelSensorDriverBleDbus implements IEventListener
{
    AngelSensor driver;
    AngelSensorConfig config;	
    AsciiDataWriter writer;
    int sampleCount = 0;

        
    @Before
    public void init() throws Exception
    {
        ModuleRegistry reg = SensorHub.getInstance().getModuleRegistry();

        BluetoothNetworkConfig netConf = new BluetoothNetworkConfig();
        netConf.id = "BLE";
        netConf.moduleClass = BleDbusCommNetwork.class.getCanonicalName();
        netConf.deviceName = "hci0";
        netConf.autoStart = true;
        reg.loadModule(netConf);
        
        config = new AngelSensorConfig();
        config.id = "ANGEL";        
        config.networkID = netConf.id;
        //config.btAddress = "00:07:80:79:04:AF";
        config.btAddress = "00:07:80:03:0E:0A";
        config.autoStart = true;
        driver = (AngelSensor)reg.loadModule(config);
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
        ISensorDataInterface output = driver.getObservationOutputs().get("healthData");
        
        writer = new AsciiDataWriter();
        writer.setDataEncoding(new TextEncodingImpl(",", "\n"));
        writer.setDataComponents(output.getRecordDescription());
        writer.setOutput(System.out);
        
        output.registerListener(this);
        driver.start();
        
        synchronized (this) 
        {
            while (sampleCount < 20)
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
