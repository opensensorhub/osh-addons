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

package org.sensorhub.impl.sensor.isa;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
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
import org.vast.data.TextEncodingImpl;
import org.vast.sensorML.SMLUtils;
import org.vast.swe.AsciiDataWriter;
import org.vast.swe.SWEStaxBindings;
import org.vast.swe.SWEUtils;
import org.vast.swe.json.SWEJsonStreamWriter;
import org.vast.swe.test.TestSweStaxBindingsV20;
import org.vast.xml.IndentingXMLStreamWriter;
import org.vast.xml.XMLImplFinder;
import org.vast.xml.XMLWriterException;
import static org.junit.Assert.*;


public class TestIsaDriver implements IEventListener
{
    ISADriver driver;
    ISAConfig config;
    AsciiDataWriter writer;
    int sampleCount = 0;

        
    @Before
    public void init() throws Exception
    {
        config = new ISAConfig();
        config.id = UUID.randomUUID().toString();

        driver = new ISADriver();
        driver.init(config);
    }
    

    @Test
    public void testGetOutputDesc() throws Exception
    {
        var sensor = driver.getMembers().values().iterator().next();
        for (IStreamingDataInterface di: sensor.getObservationOutputs().values())
        {
            DataComponent dataMsg = di.getRecordDescription();
            
            System.out.println();
            new SWEUtils(SWEUtils.V2_0).writeComponent(System.out, dataMsg, false, true);
            
            System.out.println();
            System.out.println();
            writeToJson(System.out, dataMsg);
            System.out.println();
        }
    }
    
    
    private void writeToJson(OutputStream os, DataComponent dataMsg) throws XMLWriterException
    {
        try
        {
            SWEStaxBindings sweBindings = new SWEStaxBindings();
            SWEJsonStreamWriter writer = new SWEJsonStreamWriter(os, StandardCharsets.UTF_8);
            sweBindings.writeDataComponent(writer, dataMsg, false);
            writer.flush();            
        }
        catch (XMLStreamException e)
        {
            throw new XMLWriterException("Error while writing " + dataMsg + " to output stream", e);
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
        IStreamingDataInterface output = driver.getObservationOutputs().get("gammaExposure");
        writer = new AsciiDataWriter();
        writer.setDataEncoding(new TextEncodingImpl(",", "\n"));
        writer.setDataComponents(output.getRecordDescription());
        writer.setOutput(System.out);

        output.registerListener(this);
        driver.start();
        
        Thread.sleep(30000);
        
        driver.stop();
        
        System.out.println();
    }
    
    
    @Override
    public void handleEvent(Event e)
    {
        assertTrue(e instanceof DataEvent);
        DataEvent dataEvent = (DataEvent)e;
        
        try
        {
            System.out.print("\nNew data received from sensor " + dataEvent.getProcedureUID());
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
            if (driver != null)
                driver.stop();
        }
        catch (SensorHubException e)
        {
            e.printStackTrace();
        }
    }
}
