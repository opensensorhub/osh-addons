package org.sensorhub.test.impl.sensor.axis;

import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.common.Event;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.sensor.ISensorControlInterface;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.axis.AxisCameraConfig;
import org.sensorhub.impl.sensor.axis.AxisCameraDriver;
import org.vast.cdm.common.DataBlock;
import org.vast.cdm.common.DataComponent;
import org.vast.cdm.common.DataType;
import org.vast.data.DataValue;
import org.vast.sweCommon.SWECommonUtils;
import org.vast.xml.DOMHelper;
import org.w3c.dom.Element;
import static org.junit.Assert.*;


public class TestAxisCameraDriver implements IEventListener
{
    AxisCameraDriver driver;
    AxisCameraConfig config;
    int actualWidth, actualHeight; 
    
    
    @Before
    public void init() throws Exception
    {
        config = new AxisCameraConfig();
        config.ipAddress = "root:more4less@192.168.1.50";
        config.id = UUID.randomUUID().toString();
        
        driver = new AxisCameraDriver();
        driver.init(config);
        driver.start();
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
    public void testGetCommandDesc() throws Exception
    {
        for (ISensorControlInterface ci: driver.getCommandInputs().values())
        {
            DataComponent commandMsg = ci.getCommandDescription();
            DOMHelper dom = new DOMHelper();
            Element elt = new SWECommonUtils().writeComponent(dom, commandMsg);
            dom.serialize(elt, System.out, true);
        }
    }
    
    
    @Test
    public void testCaptureAtDefaultRes() throws Exception
    {
        // register listener on data interface
        ISensorDataInterface di = driver.getObservationOutputs().values().iterator().next();
        di.registerListener(this);
        
        // start capture and wait until we receive the first frame
        synchronized (this)
        {
            this.wait();
            driver.stop();
        }
        
        //assertTrue(actualWidth == config.defaultParams.imgWidth);
        //assertTrue(actualHeight == config.defaultParams.imgHeight);
    }
    
    
    /*@Test
    public void testChangeParams() throws Exception
    {
        // register listener on data interface
        ISensorDataInterface di = driver.getObservationOutputs().values().iterator().next();
        di.registerListener(this);
        
        int expectedWidth = config.defaultParams.imgWidth = 320;
        int expectedHeight = config.defaultParams.imgHeight = 240;
        
        // start capture and wait until we receive the first frame
        synchronized (this)
        {
            startCapture();
            this.wait();
        }
        
        assertTrue(actualWidth == expectedWidth);
        assertTrue(actualHeight == expectedHeight);
    }*/
    
    
    /*@Test
    public void testSendCommand() throws Exception
    {
        // register listener on data interface
        ISensorDataInterface di = driver.getObservationOutputs().values().iterator().next();
        di.registerListener(this);
        
        // start capture and wait until we receive the first frame
        synchronized (this)
        {            
            startCapture();
            this.wait();
        }        
        
        int expectedWidth = 160;
        int expectedHeight = 120;
        
        ISensorControlInterface ci = driver.getCommandInputs().values().iterator().next();
        DataBlock commandData = ci.getCommandDescription().createDataBlock();
        int fieldIndex = 0;
        commandData.setStringValue(fieldIndex++, "YUYV");
        if (((DataValue)ci.getCommandDescription().getComponent(1)).getDataType() != DataType.INT)
        {
            commandData.setStringValue(fieldIndex++, expectedWidth+"x"+expectedHeight);
        }
        else
        {
            commandData.setIntValue(fieldIndex++, expectedWidth);
            commandData.setIntValue(fieldIndex++, expectedHeight);
        }
        commandData.setIntValue(fieldIndex++, 10);
        
        // send command to control interface
        ci.execCommand(commandData);
        
        // start capture and wait until we receive the first frame
        // after we changed settings
        synchronized (this)
        {            
            this.wait();
        }
        
        assertTrue(actualWidth == expectedWidth);
        assertTrue(actualHeight == expectedHeight);
    }*/
    
    
    @Override
    public void handleEvent(Event e)
    {
        assertTrue(e instanceof SensorDataEvent);
        SensorDataEvent newDataEvent = (SensorDataEvent)e;
        DataComponent camDataStruct = newDataEvent.getRecordDescription();
        
        actualWidth = camDataStruct.getComponent(1).getComponentCount();
        actualHeight = camDataStruct.getComponentCount();
        
        System.out.println("New data received from sensor " + newDataEvent.getSensorId());
        System.out.println("Image is " + actualWidth + "x" + actualHeight);
        
        synchronized (this) { this.notify(); }
    }
    
    
    @After
    public void cleanup()
    {
        driver.stop();
    }
}
