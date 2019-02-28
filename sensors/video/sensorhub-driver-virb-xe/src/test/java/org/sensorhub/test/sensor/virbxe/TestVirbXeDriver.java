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

package org.sensorhub.test.sensor.virbxe;

import java.io.IOException;
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
import org.sensorhub.impl.security.ClientAuth;
import org.sensorhub.impl.sensor.virbxe.VirbXeConfig;
import org.sensorhub.impl.sensor.virbxe.VirbXeDriver;
import org.sensorhub.impl.sensor.virbxe.VirbXeVideoOutput;
import org.vast.data.TextEncodingImpl;
import org.vast.sensorML.SMLUtils;
import org.vast.swe.AsciiDataWriter;
import org.vast.swe.SWEUtils;
import static org.junit.Assert.*;


public class TestVirbXeDriver implements IEventListener
{
    VirbXeDriver driver;
    VirbXeConfig config;	
    AsciiDataWriter writer;
    int sampleCount = 0;

        
    @Before
    public void init() throws Exception
    {
    	ClientAuth.createInstance(" ");
    	
    	config = new VirbXeConfig();
        config.id = UUID.randomUUID().toString();
        config.http.remoteHost = "192.168.0.22";
                        
        driver = new VirbXeDriver();
        driver.init(config);
    }
    
    
    @Test
    public void testGetOutputDesc() throws Exception
    {
        driver.start();
        
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
        driver.start();
        
        System.out.println();
        AbstractProcess smlDesc = driver.getCurrentDescription();
        new SMLUtils(SWEUtils.V2_0).writeProcess(System.out, smlDesc, true);
    }
    
    
    @Test
    public void testSendNavMeasurements() throws Exception
    {
        System.out.println();
        
        driver.start();
        ISensorDataInterface output = driver.getObservationOutputs().get("navData");
        assertTrue("No nav output", (output != null));
        output.registerListener(this);
        
        writer = new AsciiDataWriter();
        writer.setDataEncoding(new TextEncodingImpl(",", "\n"));
        writer.setDataComponents(output.getRecordDescription());
        writer.setOutput(System.out);
        
        synchronized (this) 
        {
            while (sampleCount < 10)
                wait();
        }
        
        System.out.println();
    }
    
    
    @Test
    public void testSendHealthMeasurements() throws Exception
    {
        System.out.println();
        
        driver.start();
        ISensorDataInterface output = driver.getObservationOutputs().get("healthSensors");
        assertTrue("No healthSensors output", (output != null));
        output.registerListener(this);
        
        writer = new AsciiDataWriter();
        writer.setDataEncoding(new TextEncodingImpl(",", "\n"));
        writer.setDataComponents(output.getRecordDescription());
        writer.setOutput(System.out);
        
        synchronized (this) 
        {
            while (sampleCount < 10)
                wait();
        }
        
        System.out.println();
    }
    
    
    @Test
    public void testSendVideoMeasurements() throws Exception
    {
        System.out.println();
        
        driver.start();
        ISensorDataInterface output = driver.getObservationOutputs().get("video");
        assertTrue("No video output", (output != null));
        output.registerListener(this);
        
        writer = new AsciiDataWriter();
        writer.setDataEncoding(new TextEncodingImpl(",", "\n"));
        writer.setDataComponents(output.getRecordDescription());
        writer.setOutput(System.out);
        
        synchronized (this) 
        {
            while (sampleCount < 10)
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
            if (e.getSource() instanceof VirbXeVideoOutput)
            {
                System.out.println("Received Frame, Timestamp=" + newDataEvent.getRecords()[0].getDoubleValue(0));
                sampleCount++;
            }
            else
            {
                //System.out.print("\nNew data received from sensor " + newDataEvent.getSensorId());
                writer.write(newDataEvent.getRecords()[0]);
                writer.flush();            
                sampleCount++;
            }
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
            Thread.sleep(500); // sleep 500ms before next test or camera doesn't accept next connection
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
