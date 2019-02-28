package uk.co.envsys.sensorhub.test.sensor.httpweather;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.vast.swe.AsciiDataWriter;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import uk.co.envsys.sensorhub.sensor.httpweather.HttpWeatherConfig;
import uk.co.envsys.sensorhub.sensor.httpweather.HttpWeatherSensor;

public abstract class HttpWeatherTestUtils {
	static final double IN_TEMP = 22.0;
	static final double OUT_TEMP = 16.2;
	static final double AVG_WIND_SPEED = 33.0;
	static final double DEW_POINT = 17.0;
	static final double HEAT_INDEX = 12.0;
	static final double OUT_HUM = 89;
	static final double RAIN_RATE = 0;
	static final double SEA_PRESSURE = 1019.7;
	static final double SOLAR_RADIATION = 49;
	static final double PRESSURE = 1013.6;
	static final double UV_INDEX = 0.0;
	static final double WIND_CHILL = -3.2;
	static final double WIND_SPEED = 22.3;
	static final double WIND_DIR = 187;
	static final String SUNRISE = "06:00";
	static final String SUNSET = "21:00";
	
	protected HttpWeatherConfig config;
	protected HttpWeatherSensor driver;
	protected AsciiDataWriter writer;
	protected int sampleCount = 0;		// total samples collected so far

	/**
	 * Convenience to perform an HTTP GET request, check the status code and return the text
	 * from the response. 
	 * 
	 * @return String of the text response
	 * @throws IOException
	 */
	protected String sendMeasurementToHttp(String values) throws IOException {
		String url = "http://localhost:8080/sensorhub/httpweather/" + config.urlBase + values; 
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		int responseCode = con.getResponseCode();
		assertTrue(responseCode == 200);
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		StringBuffer response = new StringBuffer();
		String inputLine;
		while ((inputLine = in.readLine()) != null) 
			response.append(inputLine);		
		in.close();
		return response.toString();
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
		assertTrue(message, getValueByName(fieldName, record) == expectedValue);
	}
	
	/**
	 * Assert a string value matches expected from the data record
	 * @param record The record containing values
	 * @param fieldName The field name to test
	 * @param expectedValue The value it should be
	 */
	protected void assertValueMatch(DataBlock record, String fieldName, String expectedValue) {
		String message = fieldName + " does not match " + String.valueOf(expectedValue);
		assertTrue(message, getStringValueByName(fieldName, record).equals(expectedValue));
	}
	
	/**
	 * Assert there exists a component with name in parent
	 * 
	 * @param name The name of the sub-component we are asserting exists
	 * @param parent The parent component
	 */
	protected void assertComponentExists(String name, DataComponent parent) {
		assertTrue(name + "does not exist in data output", parent.getComponent(name) != null);
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
	 * Gets a value, by field name, out of a DataBlock record
	 * 
	 * @param name The name of the field
	 * @param record The record to extract the value from
	 * @return The value in the field, returned as a String
	 */
	protected String getStringValueByName(String name, DataBlock record) {
		DataComponent recordDescription = driver.getObservationOutputs().values().iterator().next().getRecordDescription();
        return record.getStringValue(recordDescription.getComponentIndex(name));
	}
	
	
}
