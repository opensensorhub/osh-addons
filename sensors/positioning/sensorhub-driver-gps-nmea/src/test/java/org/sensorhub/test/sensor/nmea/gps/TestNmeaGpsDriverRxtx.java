/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.sensor.nmea.gps;

import java.io.IOException;
import java.util.Arrays;
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
import org.sensorhub.impl.comm.UARTConfig.Parity;
import org.sensorhub.impl.comm.rxtx.RxtxSerialCommProviderConfig;
import org.sensorhub.impl.sensor.nmea.gps.NMEAGpsConfig;
import org.sensorhub.impl.sensor.nmea.gps.NMEAGpsSensor;
import org.vast.data.TextEncodingImpl;
import org.vast.sensorML.SMLUtils;
import org.vast.swe.AsciiDataWriter;
import org.vast.swe.SWEUtils;
import static org.junit.Assert.*;


public class TestNmeaGpsDriverRxtx implements IEventListener
{
    NMEAGpsSensor driver;
    NMEAGpsConfig config;
    AsciiDataWriter writer;
    int sampleCount = 0;

        
    @Before
    public void init() throws Exception
    {
        config = new NMEAGpsConfig();
        config.id = UUID.randomUUID().toString();
        config.activeSentences = Arrays.asList("GGA", "RMC", "GSA");
        
        RxtxSerialCommProviderConfig providerConf = new RxtxSerialCommProviderConfig();
        providerConf.moduleClass = "org.sensorhub.impl.comm.rxtx.RxtxSerialCommProvider";
        providerConf.protocol.portName = "/dev/ttyUSB0";
        providerConf.protocol.baudRate = 9600;
        providerConf.protocol.dataBits = 8;
        providerConf.protocol.stopBits = 1;
        providerConf.protocol.parity = Parity.PARITY_NONE;
        providerConf.protocol.receiveTimeout = 100;
        config.commSettings = providerConf;
        
        driver = new NMEAGpsSensor();
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
        
        ISensorDataInterface locOutput = driver.getObservationOutputs().get("gpsLocation");
        locOutput.registerListener(this);
        
        ISensorDataInterface qualOutput = driver.getObservationOutputs().get("gpsQuality");
        qualOutput.registerListener(this);
        
        driver.start();
        
        synchronized (this) 
        {
            while (sampleCount < 50)
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
            writer.setDataComponents(newDataEvent.getRecordDescription());
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
