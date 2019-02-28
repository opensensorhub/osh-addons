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

package org.sensorhub.test.impl.sensor.vaisala;

import java.io.IOException;
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
import org.sensorhub.impl.comm.rxtx.RxtxSerialCommProviderConfig;
import org.sensorhub.impl.sensor.vaisala.VaisalaWeatherConfig;
import org.sensorhub.impl.sensor.vaisala.VaisalaWeatherSensor;
import org.vast.data.TextEncodingImpl;
import org.vast.sensorML.SMLUtils;
import org.vast.swe.AsciiDataWriter;
import org.vast.swe.SWEUtils;

import static org.junit.Assert.*;


public class TestVaisalaWeatherDriver implements IEventListener
{
    VaisalaWeatherSensor driver;
    VaisalaWeatherConfig config;
    AsciiDataWriter writer;
    int sampleCount = 0;

        
    @Before
    public void init() throws Exception
    {
        config = new VaisalaWeatherConfig();
        config.id = UUID.randomUUID().toString();
        
        RxtxSerialCommProviderConfig serialConf = new RxtxSerialCommProviderConfig();
        //serialConf.portName = "/dev/tty.usbserial";
        serialConf.protocol.portName = "/dev/rfcomm0";
        serialConf.protocol.baudRate = 9600;
        //serialConf.receiveThreshold = 1;
        //serialConf.protocol.receiveTimeout = 1000;
        config.commSettings = serialConf;
        
        driver = new VaisalaWeatherSensor();
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
        ISensorDataInterface gpsOutput = driver.getObservationOutputs().get("weather");
        //ISensorDataInterface gpsOutput = driver.getObservationOutputs().get("wind");
        //ISensorDataInterface gpsOutput = driver.getObservationOutputs().get("ptu");
        //ISensorDataInterface gpsOutput = driver.getObservationOutputs().get("precipitation");
        //ISensorDataInterface gpsOutput = driver.getObservationOutputs().get("supervisor");
        
        writer = new AsciiDataWriter();
        writer.setDataEncoding(new TextEncodingImpl(",", "\n"));
        writer.setDataComponents(gpsOutput.getRecordDescription());
        writer.setOutput(System.out);
        
        gpsOutput.registerListener(this);
        driver.start();
        
        synchronized (this) 
        {
            while (sampleCount < 2)
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
