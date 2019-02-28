package uk.co.envsys.sensorhub.test.sensor.meteobridge;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.common.Event;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.SensorDataEvent;

import org.vast.data.TextEncodingImpl;
import org.vast.sensorML.SMLUtils;
import org.vast.swe.AsciiDataWriter;
import org.vast.swe.SWEUtils;

import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import uk.co.envsys.sensorhub.sensor.meteobridge.MeteobridgeConfig;
import uk.co.envsys.sensorhub.sensor.meteobridge.MeteobridgeSensor;

public class TestMeteobridgeDriver implements IEventListener {
	static final int METEO_TELNET_PORT = 5557;
	static final boolean DEBUG = false;
	static final double EPSILON = .0001;
	
	private MeteobridgeSensor driver;
    private MeteobridgeConfig config;
    private AsciiDataWriter writer;
    private int sampleCount = 0;
    
    @Before
    public void init() throws Exception {
        config = new MeteobridgeConfig();
        config.id = UUID.randomUUID().toString();
        config.address = "127.0.0.1";
        config.samplingFrequency = 1;
        
        driver = new MeteobridgeSensor();
        driver.init(config);
    }

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
            // Check that default config produces output with correct values
            if(config.th0tempEnabled) assertExists("th0temp", dataMsg);
            if(config.th0humEnabled) assertExists("th0hum", dataMsg);
            if(config.th0dewEnabled) assertExists("th0dew", dataMsg);
            if(config.th0heatindexEnabled) assertExists("th0heatindex", dataMsg);
            if(config.thb0tempEnabled) assertExists("thb0temp", dataMsg);
            if(config.thb0humEnabled) assertExists("thb0hum", dataMsg);
            if(config.thb0dewEnabled) assertExists("thb0dew", dataMsg);
            if(config.thb0pressEnabled) assertExists("thb0press", dataMsg);
            if(config.thb0seapressEnabled) assertExists("thb0seapress", dataMsg);
            if(config.wind0windEnabled) assertExists("wind0wind", dataMsg);
            if(config.wind0avgwindEnabled) assertExists("wind0avgwind", dataMsg);
            if(config.wind0dirEnabled) assertExists("wind0dir", dataMsg);
            if(config.wind0chillEnabled) assertExists("wind0chill", dataMsg);
            if(config.wind0gustEnabled) assertExists("wind0gust", dataMsg);
            if(config.rain0rateEnabled) assertExists("rain0rate", dataMsg);
            if(config.rain0totalEnabled) assertExists("rain0total", dataMsg);
            if(config.uv0indexEnabled) assertExists("uv0index", dataMsg);
            if(config.sol0radEnabled) assertExists("sol0rad", dataMsg);
            
            // ensure that sol0evo is disabled by default, and not included in outputs
            assertTrue(!config.sol0evoEnabled);
            assertTrue(dataMsg.getComponent("sol0evo") == null);
        }
	}
	
	@Test
	public void testDisabledComponentNotInOutputDesc() throws Exception {
		for (ISensorDataInterface di: driver.getObservationOutputs().values()) {
            DataComponent dataMsg = di.getRecordDescription();
            assertTrue(!config.sol0evoEnabled);
            assertTrue(dataMsg.getComponent("sol0evo") == null);
        }
	}
	
    @Test
    public void testGetSensorDesc() throws Exception {
        AbstractProcess smlDesc = driver.getCurrentDescription();
        if(DEBUG) {
        	System.out.println();
        	new SMLUtils(SWEUtils.V2_0).writeProcess(System.out, smlDesc, true);
        }
        assertTrue(smlDesc.getNumOutputs() == 1);
        assertTrue(smlDesc.getDescription() == "Weather station connected to a Meteobridge device");
    }
    
    @Test
    /**
     * Tests the driver pulling data from the meteobridge and sending it to 
     * sensorhub. This is achieved by running an HTTP endpoint to pretend to be a meteobridge
     * which the driver will query.
     * 
     * @throws Exception
     */
    public void testSendMeasurements() throws Exception {
    	// Unimplmented, need sample meteobridge output from livedataxml.cgi
    	// register this as listener
    	// start driver (log/respond to events)
    	ISensorDataInterface meteobridgeOutput = driver.getObservationOutputs().get("weather");
    	
    	writer = new AsciiDataWriter();
        writer.setDataEncoding(new TextEncodingImpl(",", "\n"));
        writer.setDataComponents(meteobridgeOutput.getRecordDescription());
        writer.setOutput(System.out);
        
        ServerSocket listener = new ServerSocket(METEO_TELNET_PORT);
        meteobridgeOutput.registerListener(this);
        driver.start();
        
        // set up a socket for the driver to query, telnet style
        try {
            while(sampleCount < 2) {
                Socket socket = listener.accept();
                try {
                    PrintWriter out =
                        new PrintWriter(socket.getOutputStream(), true);
                    out.println(getFakeXMLData());
                } finally {
                    socket.close();
                }
            }
        }
        finally {
            listener.close();
        }
    }
	
	@Override
	public void handleEvent(Event<?> e) {
		assertTrue(e instanceof SensorDataEvent);
        SensorDataEvent newDataEvent = (SensorDataEvent)e;
        DataBlock latestReading = newDataEvent.getRecords()[0];
        sampleCount++;
        
        assertValueMatch(latestReading, "th0temp", 17.7);
        assertValueMatch(latestReading, "th0hum", 89);
        assertValueMatch(latestReading, "th0dew", 15.9);
        assertValueMatch(latestReading, "uv0index", 0);
        assertValueMatch(latestReading, "sol0rad", 49);
        assertValueMatch(latestReading, "uv0index", 0);
        assertValueMatch(latestReading, "wind0chill", 17.7);
        assertValueMatch(latestReading, "wind0dir", 355);
        assertValueMatch(latestReading, "wind0gust", 0.9);
        assertValueMatch(latestReading, "rain0total", 2240.8);
        assertValueMatch(latestReading, "thb0temp", 22.7);
        assertValueMatch(latestReading, "thb0hum", 66);
        assertValueMatch(latestReading, "thb0dew", 16.0);
        assertValueMatch(latestReading, "thb0press", 1013.6);
        assertValueMatch(latestReading, "thb0seapress", 1019.7);
        
        try {
            //System.out.print("\nNew data received from sensor " + newDataEvent.getSensorId());
            writer.write(newDataEvent.getRecords()[0]);
            writer.flush();
            
            
        } catch (IOException e1) {
            e1.printStackTrace();
        }
                
        synchronized (this) { this.notify(); }
		
	}
	
	@After
    public void cleanup() {
        try {
            driver.stop();
        } catch (SensorHubException e) {
            e.printStackTrace();
        }
    }
	
	private void assertExists(String name, DataComponent dataMsg) {
		DataComponent component = dataMsg.getComponent(name);
    	assertTrue(name + " not found in record description but enabled in config", component != null);
	}
	
	private String getFakeXMLData() {
		String testXML = "<logger>";
		testXML += "\n" + "<TH date=\"20160721144344\" id=\"th0\" temp=\"17.7\" hum=\"89\" dew=\"15.9\" lowbat=\"0\"/>";
		testXML += "\n" + "<UV date=\"20160721144344\" id=\"uv0\" index=\"0.0\" lowbat=\"0\"/>";
		testXML += "\n" + "<SOL date=\"20160721144422\" id=\"sol0\" rad=\"49\" lowbat=\"0\"/>";
		testXML += "\n" + "<WIND date=\"20160721144422\" id=\"wind0\" dir=\"355\" gust=\"0.9\" wind=\"0.0\" chill=\"17.7\" lowbat=\"0\"/>";
		testXML += "\n" + "<RAIN date=\"20160721144422\" id=\"rain0\" rate=\"0.0\" total=\"2240.8\" delta=\"0.0\" lowbat=\"0\"/>";
		testXML += "\n" + "<THB date=\"20160721144348\" id=\"thb0\" temp=\"22.7\" hum=\"66\" dew=\"16.0\" press=\"1013.6\" seapress=\"1019.7\" fc=\"2\" lowbat=\"0\"/>";
		testXML += "\n" + "</logger>";
		return testXML;
	}
	
	
	/**
	 * Gets a value, by field name, out of a DataBlock record
	 * 
	 * @param name The name of the field
	 * @param record The record to extract the value from
	 * @return The value in the field as a Double
	 */
	protected double getValueByName(String name, DataBlock record) {
		DataComponent recordDescription = driver.getObservationOutputs().values().iterator().next().getRecordDescription();
        return record.getDoubleValue(recordDescription.getComponentIndex(name));
	}
	
	
	/**
	 * Assert a double value matches expected from the data record
	 * 
	 * @param record The record containing values
	 * @param fieldName The field name to test
	 * @param expectedValue The value it should be
	 */
	protected void assertValueMatch(DataBlock record, String fieldName, double expectedValue) {
		String message = fieldName + " does not match " + String.valueOf(expectedValue);
		assertTrue(message, Math.abs(getValueByName(fieldName, record)-expectedValue) < EPSILON);
	}

}
