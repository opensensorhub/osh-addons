/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License Version
 1.1 (the "License"); you may not use this file except in compliance with
 the License. You may obtain a copy of the License at
 http://www.mozilla.org/MPL/MPL-1.1.html
 
 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.
 
 The Original Code is "SensorHub".
 
 The Initial Developer of the Original Code is Sensia Software LLC.
 <http://www.sensiasoftware.com>. Portions created by the Initial
 Developer are Copyright (C) 2013 the Initial Developer. All Rights Reserved.
 
 Please contact Alexandre Robin <alex.robin@sensiasoftware.com> for more 
 information.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.impl.sensor.fakegps;

import java.io.IOException;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.common.Event;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.fakegps.FakeGpsConfig;
import org.sensorhub.impl.sensor.fakegps.FakeGpsSensor;
import org.vast.cdm.common.AsciiEncoding;
import org.vast.cdm.common.DataComponent;
import org.vast.sweCommon.AsciiDataWriter;
import org.vast.sweCommon.SWECommonUtils;
import org.vast.xml.DOMHelper;
import org.w3c.dom.Element;
import static org.junit.Assert.*;


public class TestFakeGpsDriver implements IEventListener
{
    FakeGpsSensor driver;
    FakeGpsConfig config;
    AsciiDataWriter writer;

        
    @Before
    public void init() throws Exception
    {
        config = new FakeGpsConfig();
        config.id = UUID.randomUUID().toString();
        config.centerLatitude = 34.7300;
        config.centerLongitude = -86.5850;
        config.areaSize = 0.1;
        
        driver = new FakeGpsSensor();
        driver.init(config);
    }
    
    
    @Test
    public void testGetOutputDesc() throws Exception
    {
        for (ISensorDataInterface di: driver.getObservationOutputs().values())
        {
            DataComponent dataMsg = di.getRecordDescription();
            DOMHelper dom = new DOMHelper();
            Element elt = new SWECommonUtils().writeComponent(dom, dataMsg);
            dom.serialize(elt, System.out, true);
        }
    }
    
    
    @Test
    public void testSendMeasurements() throws Exception
    {
        ISensorDataInterface gpsOutput = driver.getObservationOutputs().get("locationOutput");
        
        writer = new AsciiDataWriter();
        writer.setDataEncoding(new AsciiEncoding("\n", ","));
        writer.setDataComponents(gpsOutput.getRecordDescription());
        writer.setOutput(System.out);
        
        gpsOutput.registerListener(this);
        driver.start();
        
        Thread.sleep(3000);
    }
    
    
    @Override
    public void handleEvent(Event e)
    {
        assertTrue(e instanceof SensorDataEvent);
        SensorDataEvent newDataEvent = (SensorDataEvent)e;
        
        try
        {
            System.out.println("\nNew data received from sensor " + newDataEvent.getSensorId());
            writer.write(newDataEvent.getRecords()[0]);
            writer.flush();
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
