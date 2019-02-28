package org.sensorhub.test.impl.sensor.domoticz;


import java.awt.image.BufferedImage;
import java.util.UUID;

import javax.swing.JFrame;

import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.DataBlock;
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
import org.sensorhub.impl.security.ClientAuth;
import org.sensorhub.impl.sensor.domoticz.DomoticzConfig;
import org.sensorhub.impl.sensor.domoticz.DomoticzDriver;
import org.vast.data.DataChoiceImpl;
import org.vast.data.TextEncodingImpl;
import org.vast.sensorML.SMLUtils;
import org.vast.swe.AsciiDataWriter;
import org.vast.swe.SWEUtils;
import org.vast.util.DateTimeFormat;

import static org.junit.Assert.*;


/**
 * <p>
 * Implementation of sensor interface for generic Axis Cameras using IP
 * protocol
 * </p>
 * 
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 */
public class TestDomoticzDriver implements IEventListener
{
	DomoticzDriver driver;
	DomoticzConfig config;
	AsciiDataWriter writer;
	int sampleCount = 0;
    
    @Before
    public void init() throws Exception
    {
        config = new DomoticzConfig();
        config.id = UUID.randomUUID().toString();

        driver = new DomoticzDriver();
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
    public void testPostData() throws Exception
    {
        System.out.println();
        
        ISensorDataInterface dataOutput = driver.getObservationOutputs().get("DomoticzTempData");

        writer = new AsciiDataWriter();
        writer.setDataEncoding(new TextEncodingImpl(",", "\n"));
        writer.setDataComponents(dataOutput.getRecordDescription());
        writer.setOutput(System.out);
        dataOutput.registerListener(this);
        
        driver.start();
        
        synchronized (this) 
        {
            while (sampleCount < 2)
                wait();
        }
        
        System.out.println();
        System.out.println("Done posting data");
    }
    
//    @Test
//    public void testSwitchOrSelectorCommand() throws Exception
//    {  
//        // get control interface
//        ISensorControlInterface ci = driver.getCommandInputs().get("selectorControl");
//        DataComponent commandDesc = ci.getCommandDescription().copy();
//    	DataBlock commandData;
//    	
//    	
//    	String switchID = "8";
//    	String setStatus = "setOn";
//    	//String setStatus = "setOff";
//    	//String setStatus = "toggle";
//    	
//    	
//    	int cnt = 0;
//    	while (cnt<6)
//    	{
//	    	if (cnt%2 == 0)
//	    		setStatus = "setOn";
//	    	else
//	    		setStatus = "setOff";
//	    	
//	    	// test switch
//	    	((DataChoiceImpl)commandDesc).setSelectedItem(setStatus);
//	    	commandData = commandDesc.createDataBlock();
//	    	commandData.setStringValue(1, switchID);
//	    	ci.execCommand(commandData);
//	    	cnt ++;
//	    	Thread.sleep(5000);
//    	}
    	
//    	String setStatus = "setLevel";
//    	String switchID = "8,0";
//    	int cnt = 0;
//    	int level = 0;
//    	while (cnt<6)
//    	{
//	    	// test selector
//	    	((DataChoiceImpl)commandDesc).setSelectedItem(setStatus);
//	    	commandData = commandDesc.createDataBlock();
//	    	commandData.setStringValue(1, switchID);
//	    	ci.execCommand(commandData);
//	    	cnt ++;
//	    	level += 20;
//	    	switchID = "8," + Integer.toString(level);
//	    	Thread.sleep(5000);
//    	}
//    }
    
    
    @Override
    public void handleEvent(Event<?> e)
    {
        assertTrue(e instanceof SensorDataEvent);
        SensorDataEvent newDataEvent = (SensorDataEvent)e;
        
        double timeStamp = newDataEvent.getRecords()[0].getDoubleValue(0);
        System.out.println("Frame received on " + new DateTimeFormat().formatIso(timeStamp, 0));
        sampleCount++;
        
        synchronized (this) { this.notify(); }
    }
    
    @After
    public void cleanup() throws SensorHubException
    {
        driver.stop();
    }
}
