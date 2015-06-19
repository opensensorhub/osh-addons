package org.sensorhub.impl.sensor.station.metar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.joda.time.DateTime;
import org.sensorhub.impl.sensor.station.Station;
import org.sensorhub.impl.sensor.station.StationDataPoller;


public class MetarDataPoller implements StationDataPoller {
	//    //StationName,City,State,ZipCode,MeasurementDateUTC,MeasurementDateLocal,Temperature (degreesF),Dewpoint (degreesF),Relative Humididty (%),Wind Speed (mph),Wind Direction (degrees),
	//Air Pressure (inches HG),Precipitation (inches),Heat Index (degreesF),Wind Chill (degreesF),Heating Degree Days,Cooling Degree Days,Wind Gust (mph),
	//Rainfaill last 3 hours (inches),Rainfaill last 6 hours (inches),Rainfaill last 24 hours (inches),Max Temperature last 24 hours (degreesF),Min Temperature last 24 hours (degreesF),
	//cloud Ceiling (feet),visibility (feet),PresentWeather,SkyConditions
	private static final String server = "webservices.anythingweather.com";
	private static final String path = "/CurrentObs/GetCurrentObs";

	/**
	 * 
	 * @return the last available data record
	 */
	public MetarDataRecord pullStationData(String stationID) {
		MetarDataRecord rec = new MetarDataRecord();
		String csvData = pollServer(stationID);
		String [] lines = csvData.split("\\n");
		//  first line is header- second is latest data record
		String [] vals = (lines[1]+",END").split(",");
		Station station = new Station();
		station.setName(vals[0]);
		rec.setStation(station);
		rec.setTimeStringUtc(vals[4].replace(" ",	"T")+ "Z");
		DateTime dt = new DateTime(rec.getTimeStringUtc());
		rec.setTimeUtc(dt.getMillis());
		rec.setTemperature(parseDouble(vals[6]));
		rec.setDewPoint(parseDouble(vals[7]));
		rec.setRelativeHumidity(parseDouble(vals[8]));
		rec.setWindSpeed(parseDouble(vals[9]));
		rec.setWindDirection(parseDouble(vals[10]));
		rec.setPressure(parseDouble(vals[11]));
		rec.setWindGust(parseDouble(vals[18]));
		rec.setCloudCeiling((int)parseDouble(vals[23]));
		rec.setVisibility((int)parseDouble(vals[24]));
		rec.setHourlyPrecip(parseDouble(vals[12]));
		rec.setPresentWeather(vals[25].trim());
		rec.setSkyConditions(vals[26].trim());
		return rec;
	}

	private String pollServer(String stationID) {
		URI uri;
		try {
			uri = new URIBuilder()
			.setScheme("http")
			.setHost(server)
			.setPath(path)
			.setParameter("clientId", "SensorHub")
			.setParameter("accessKey", "mMHAkRZMtQfBmZvH")
			.setParameter("format", "csv")
			.setParameter("numHours", "1")
			.setParameter("allData", "true")
			.setParameter("stationid", stationID)
			.build();
			HttpGet httpget = new HttpGet(uri);
			MetarSensor.log.debug("Executing request {}", httpget.getURI());
			CloseableHttpClient httpclient = HttpClients.createDefault();
			CloseableHttpResponse response = httpclient.execute(httpget);
			try {
				BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
				StringBuffer result = new StringBuffer();
				String line;
				while ((line = rd.readLine()) != null) {
					result.append(line + "\n");
				}

				MetarSensor.log.debug("Result:\n{}", result);
				return result.toString();
			} finally {
				response.close();
				httpclient.close();
			}
		} catch (URISyntaxException|IOException e) {
			e.printStackTrace();
		} 
		return null;
	}

	public double parseDouble(String s) {
		try {
			return Double.parseDouble(s);
		} catch (Exception e) {
			return -999.9; // not crazy about this- what is better option from SWE perspecitve?
		}
	}

	public static void main(String[] args) {
	    MetarDataPoller poller = new MetarDataPoller();
		poller.pullStationData("3467");
	}
}
