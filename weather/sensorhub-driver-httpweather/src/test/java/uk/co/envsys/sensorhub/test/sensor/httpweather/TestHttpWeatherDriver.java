package uk.co.envsys.sensorhub.test.sensor.httpweather;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.common.Event;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.vast.data.TextEncodingImpl;
import org.vast.sensorML.SMLUtils;
import org.vast.swe.AsciiDataWriter;
import org.vast.swe.SWEUtils;

import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import uk.co.envsys.sensorhub.sensor.httpweather.HttpWeatherConfig;
import uk.co.envsys.sensorhub.sensor.httpweather.HttpWeatherSensor;

public class TestHttpWeatherDriver extends HttpWeatherTestUtils implements IEventListener {
	static final boolean DEBUG = false;		// whether to print records

	/**
	 * Initialise the test class. Sets up the sensor config and instantiates a sensor with it.
	 * Called before each test is run.
	 * 
	 * @throws Exception if error encountered
	 */
    @Before
    public void init() throws Exception {
    	config = new HttpWeatherConfig();
        config.id = UUID.randomUUID().toString();
        
        driver = new HttpWeatherSensor();
        driver.init(config);
    }
    
    /**
     * Tests whether the output description matches that specified in the config
     * @throws Exception
     */
    @Test
	public void testOutputDescMatchesConfig() throws Exception {
		for (ISensorDataInterface di: driver.getObservationOutputs().values()) {
            DataComponent dataMsg = di.getRecordDescription();
            if(DEBUG) {
	            System.out.println();
	            new SWEUtils(SWEUtils.V2_0).writeComponent(System.out, dataMsg, false, true);
	            System.out.println();
            }
            assertComponentExists("intemp", dataMsg);
            assertComponentExists("outtemp", dataMsg);
        }
	}
    
    /**
     * Tests that a component disabled in the config, is not in the output description
     * @throws Exception
     */
    @Test
	public void testDisabledComponentNotInOutputDesc() throws Exception {
		for (ISensorDataInterface di: driver.getObservationOutputs().values()) {
            DataComponent dataMsg = di.getRecordDescription();
            assertTrue(!config.exposeSunrise);
            assertTrue(dataMsg.getComponent("sunrise") == null);
        }
	}
    
    /**
     * Tests that the description of the sensor is as we expect
     * @throws Exception
     */
    @Test
    public void testGetSensorDesc() throws Exception {
    	AbstractProcess smlDesc = driver.getCurrentDescription();
    	if(DEBUG) {
    		System.out.println();        
    		new SMLUtils(SWEUtils.V2_0).writeProcess(System.out, smlDesc, true);
    	}
        assertTrue(smlDesc.getNumOutputs() == 1);
        assertTrue("Description does not match expected", 
        		smlDesc.getDescription() == "Provides HTTP endpoint on which it can receive weather data");
    }
    
    /**
     * Tests the process for receiving measurements from the web end point.
     * Starts the driver (which publishes a listening servlet). Registers ourselves
     * as a listener to the driver, and sends an HTTP GET request to the endpoint.
     * This results in handleEvent() being called, where we check the results.
     *  
     * @throws Exception
     */
    @Test
    public void testReceiveSendMeasurements() throws Exception {
        ISensorDataInterface weatherOutput = driver.getObservationOutputs().get("httpweather");        
        String testValues = "?intemp=" + String.valueOf(IN_TEMP) + "&outtemp=" + String.valueOf(OUT_TEMP);
        if(DEBUG) {
        	System.out.println();
	        writer = new AsciiDataWriter();
	        writer.setDataEncoding(new TextEncodingImpl(",", "\n"));
	        writer.setDataComponents(weatherOutput.getRecordDescription());
	        writer.setOutput(System.out);
	        System.out.println();
        }
        
        weatherOutput.registerListener(this);
        driver.start();
        
        while (sampleCount < 2) {
        	assertTrue(sendMeasurementToHttp(testValues).equals("OK"));
        }
    }
    
    /**
     * The handle even function. This allows us to listen for events from OSH.
     * Checks the output from OSH to ensure it matches what we sent to the HTTP Endpoint.
     */
	@Override
	public void handleEvent(Event<?> e) {
		assertTrue(e instanceof SensorDataEvent);
        SensorDataEvent newDataEvent = (SensorDataEvent)e;
        DataBlock record = newDataEvent.getRecords()[0];
       
        assertValueMatch(record, "intemp", IN_TEMP);
        assertValueMatch(record, "outtemp", OUT_TEMP);
        
        if(DEBUG) {
	        try {
	            writer.write(record);
	            writer.flush();
	        } catch (IOException e1) {
	            e1.printStackTrace();
	        }
        }
        sampleCount++;
        synchronized (this) { this.notify(); }
	}
	
}
