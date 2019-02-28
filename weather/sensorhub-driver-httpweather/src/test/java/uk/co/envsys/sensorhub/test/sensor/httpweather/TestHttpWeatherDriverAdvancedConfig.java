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
import org.vast.swe.AsciiDataWriter;
import org.vast.swe.SWEUtils;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import uk.co.envsys.sensorhub.sensor.httpweather.HttpWeatherConfig;
import uk.co.envsys.sensorhub.sensor.httpweather.HttpWeatherSensor;

public class TestHttpWeatherDriverAdvancedConfig extends HttpWeatherTestUtils implements IEventListener {
	static final boolean DEBUG = false;		// whether to print records
	
	/**
	 * Initialise the test class. Sets up the sensor config and instantiates a sensor with it.
	 * Called before each test is run. This method enables exposure of all options through HTTP endpoint
	 * 
	 * @throws Exception if error encountered
	 */
    @Before
    public void init() throws Exception {
    	config = new HttpWeatherConfig();
        config.id = UUID.randomUUID().toString();
        // Enable all
        config.exposeAvgWindSpeed = config.exposeDewPoint = config.exposeHeatIndex = config.exposeOutHum =
        		config.exposeRain = config.exposeSeaPressure = config.exposeSolar = config.exposeStationPressure =
        		config.exposeSunrise = config.exposeSunset = config.exposeUV = config.exposeWindChill = 
        		config.exposeWindDir = config.exposeWindSpeed = true;
        
        driver = new HttpWeatherSensor();
        driver.init(config);
    }
	
	 /**
     * Tests whether the output description matches that specified in the config
     * @throws Exception
     */
    @Test
	public void testOutputDescMatchesConfig() throws Exception {
		// Just print the descriptions for now
		for (ISensorDataInterface di: driver.getObservationOutputs().values()) {
            DataComponent dataMsg = di.getRecordDescription();
            if(DEBUG) {
            	System.out.println();
            	new SWEUtils(SWEUtils.V2_0).writeComponent(System.out, dataMsg, false, true);
            	System.out.println();
            }
            assertComponentExists("intemp", dataMsg);
            assertComponentExists("outtemp", dataMsg);
            assertComponentExists("avgwindspeed", dataMsg);
            assertComponentExists("dewpoint", dataMsg);
            assertComponentExists("heatindex", dataMsg);
            assertComponentExists("outhum", dataMsg);
            assertComponentExists("rain", dataMsg);
            assertComponentExists("seapressure", dataMsg);
            assertComponentExists("solarradiation", dataMsg);
            assertComponentExists("pressure", dataMsg);
            assertComponentExists("sunrise", dataMsg);
            assertComponentExists("sunset", dataMsg);
            assertComponentExists("uvindex", dataMsg);
            assertComponentExists("windchill", dataMsg);
            assertComponentExists("winddir", dataMsg);
            assertComponentExists("windspeed", dataMsg);
        }
	}
    
    @Test
    public void testReceiveSendMeasurements() throws Exception {
        ISensorDataInterface weatherOutput = driver.getObservationOutputs().get("httpweather");
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
        
        String testValues = "?";
        testValues += "intemp=" + String.valueOf(IN_TEMP);
        testValues += "&outtemp=" + String.valueOf(OUT_TEMP);
        testValues += "&avgwindspeed=" + String.valueOf(AVG_WIND_SPEED);
        testValues += "&dewpoint=" + String.valueOf(DEW_POINT);
        testValues += "&heatindex=" + String.valueOf(HEAT_INDEX);
        testValues += "&outhum=" + String.valueOf(OUT_HUM);
        testValues += "&rain=" + String.valueOf(RAIN_RATE);
        testValues += "&seapressure=" + String.valueOf(SEA_PRESSURE);
        testValues += "&solarradiation=" + String.valueOf(SOLAR_RADIATION);
        testValues += "&pressure=" + String.valueOf(PRESSURE);
        testValues += "&sunrise=" + SUNRISE;
        testValues += "&sunset=" + SUNSET;
        testValues += "&uvindex=" + String.valueOf(UV_INDEX);
        testValues += "&windchill=" + String.valueOf(WIND_CHILL);
        testValues += "&winddir=" + String.valueOf(WIND_DIR);
        testValues += "&windspeed=" + String.valueOf(WIND_SPEED);
        
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
        if(DEBUG) {
	        try {
	            writer.write(record);
	            writer.flush();
	        } catch (IOException e1) {
	            e1.printStackTrace();
	        }
        }
        
        assertValueMatch(record, "intemp", IN_TEMP);
        assertValueMatch(record, "outtemp", OUT_TEMP);
        assertValueMatch(record, "avgwindspeed", AVG_WIND_SPEED);
        assertValueMatch(record, "dewpoint", DEW_POINT);
        assertValueMatch(record, "heatindex", HEAT_INDEX);
        assertValueMatch(record, "outhum", OUT_HUM);
        assertValueMatch(record, "rain", RAIN_RATE);
        assertValueMatch(record, "seapressure", SEA_PRESSURE);
        assertValueMatch(record, "solarradiation", SOLAR_RADIATION);
        assertValueMatch(record, "pressure", PRESSURE);
        assertValueMatch(record, "sunrise", SUNRISE);
        assertValueMatch(record, "sunset", SUNSET);
        assertValueMatch(record, "uvindex", UV_INDEX);
        assertValueMatch(record, "windchill", WIND_CHILL);
        assertValueMatch(record, "winddir", WIND_DIR);
        assertValueMatch(record, "windspeed", WIND_SPEED);
        sampleCount++;
               
        synchronized (this) { this.notify(); }
	}
}
