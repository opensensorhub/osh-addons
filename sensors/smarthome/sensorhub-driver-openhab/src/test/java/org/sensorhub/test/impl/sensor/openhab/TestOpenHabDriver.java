package org.sensorhub.test.impl.sensor.openhab;


import java.util.UUID;

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
import org.sensorhub.impl.sensor.openhab.OpenHabConfig;
import org.sensorhub.impl.sensor.openhab.OpenHabDriver;
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
public class TestOpenHabDriver implements IEventListener
{
	OpenHabDriver driver;
	OpenHabConfig config;
	AsciiDataWriter writer;
	int sampleCount = 0;
    
    @Before
    public void init() throws Exception
    {
        config = new OpenHabConfig();
        config.id = UUID.randomUUID().toString();

        driver = new OpenHabDriver();
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
        
        ISensorDataInterface dataOutput = driver.getObservationOutputs().get("OpenHABUltravioletData");

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
//    public void testSwitchCommand() throws Exception
//    {  
//        // get control interface
//        ISensorControlInterface ci = driver.getCommandInputs().get("switchControl");
//        DataComponent commandDesc = ci.getCommandDescription().copy();
//    	DataBlock commandData;
//    	
//    	/********** Test setting dimmer on/off *********/
//    	String setStatus = "setOff";
//    	String name = "zwave_device_office_node5_switch_dimmer";
//    	
//    	// test switch
//    	((DataChoiceImpl)commandDesc).setSelectedItem(setStatus);
//    	commandData = commandDesc.createDataBlock();
//    	commandData.setStringValue(1, name);
//    	ci.execCommand(commandData);
//    	Thread.sleep(5000);
//    }
    
//    @Test
//    public void testDimmerCommand() throws Exception
//    {  
//        // get control interface
//        ISensorControlInterface ci = driver.getCommandInputs().get("dimmerControl");
//        DataComponent commandDesc = ci.getCommandDescription().copy();
//    	DataBlock commandData;
//    	
//    	/********** Test setting dimmer level **********/
//    	String setStatus = "setLevel";
//    	String nameANDlevel = "zwave_device_office_node5_switch_dimmer,0";
//    	
//    	int cnt = 0;
//    	int level = 0;
//    	while (cnt<6)
//    	{
//	    	// test dimmer
//	    	((DataChoiceImpl)commandDesc).setSelectedItem(setStatus);
//	    	commandData = commandDesc.createDataBlock();
//	    	commandData.setStringValue(1, nameANDlevel);
//	    	ci.execCommand(commandData);
//	    	cnt ++;
//	    	level += 20;
//	    	nameANDlevel = "zwave_device_office_node5_switch_dimmer," + Integer.toString(level);
//	    	Thread.sleep(5000);
//    	}
//    	
//    	/********** Test setting dimmer on/off *********/
////    	String setStatus = "setOn";
////    	String name = "zwave_device_office_node5_switch_dimmer";
////    	
////    	// test switch
////    	((DataChoiceImpl)commandDesc).setSelectedItem(setStatus);
////    	commandData = commandDesc.createDataBlock();
////    	commandData.setStringValue(1, name);
////    	ci.execCommand(commandData);
////    	Thread.sleep(5000);
//    }
    
    
    @Override
    public void handleEvent(Event<?> e)
    {
        assertTrue(e instanceof SensorDataEvent);
        SensorDataEvent newDataEvent = (SensorDataEvent)e;
        
        double timeStamp = newDataEvent.getRecords()[0].getDoubleValue(1);
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