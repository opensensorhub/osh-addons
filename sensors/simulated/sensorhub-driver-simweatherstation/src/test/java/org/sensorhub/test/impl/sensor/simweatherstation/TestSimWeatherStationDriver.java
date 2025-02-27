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

package org.sensorhub.test.impl.sensor.simweatherstation;

import java.io.IOException;
import java.util.UUID;

import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.DataComponent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.event.Event;
import org.sensorhub.api.event.IEventListener;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.simweatherstation.SimWeatherStationConfig;
import org.sensorhub.impl.sensor.simweatherstation.SimWeatherStationSensor;
import org.vast.data.TextEncodingImpl;
import org.vast.sensorML.SMLUtils;
import org.vast.swe.AsciiDataWriter;
import org.vast.swe.SWEUtils;

import static org.junit.Assert.*;


public class TestSimWeatherStationDriver implements IEventListener
{
    SimWeatherStationSensor sensor;
    SimWeatherStationConfig config;
    AsciiDataWriter writer;
    int sampleCount = 0;

        
    @Before
    public void init() throws Exception
    {
        config = new SimWeatherStationConfig();
        config.id = UUID.randomUUID().toString();
        
        sensor = new SimWeatherStationSensor();
        sensor.init(config);
    }
    

    @Test
    public void testGetOutputDesc() throws Exception
    {
        for (IStreamingDataInterface di: sensor.getObservationOutputs().values())
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
        AbstractProcess smlDesc = sensor.getCurrentDescription();
        new SMLUtils(SWEUtils.V2_0).writeProcess(System.out, smlDesc, true);
    }
    

    @Test
    public void testSendMeasurements() throws Exception
    {
        System.out.println();
        IStreamingDataInterface weatherOutput = sensor.getObservationOutputs().get("Sim Weather");
        
        writer = new AsciiDataWriter();
        writer.setDataEncoding(new TextEncodingImpl(",", "\n"));
        writer.setDataComponents(weatherOutput.getRecordDescription());
        writer.setOutput(System.out);

        weatherOutput.registerListener(this);
        sensor.start();
        
        synchronized (this) 
        {
            while (sampleCount < 2)
                wait();
        }
        
        sensor.stop();
        
        System.out.println();
    }
    
    
    @Override
    public void handleEvent(Event e)
    {
        assertTrue(e instanceof DataEvent);
        DataEvent dataEvent = (DataEvent)e;
        
        try
        {
            System.out.print("\nNew data received from sensor " + dataEvent.getSystemUID());
            writer.write(dataEvent.getRecords()[0]);
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
            sensor.stop();
        }
        catch (SensorHubException e)
        {
            e.printStackTrace();
        }
    }
}