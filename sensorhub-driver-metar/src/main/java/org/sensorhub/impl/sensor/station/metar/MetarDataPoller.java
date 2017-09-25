package org.sensorhub.impl.sensor.station.metar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.sensorhub.impl.sensor.station.Station;

public class MetarDataPoller {
	//    //StationName,City,State,ZipCode,MeasurementDateUTC,MeasurementDateLocal,Temperature (degreesF),Dewpoint (degreesF),Relative Humididty (%),Wind Speed (mph),Wind Direction (degrees),
	//Air Pressure (inches HG),Precipitation (inches),Heat Index (degreesF),Wind Chill (degreesF),Heating Degree Days,Cooling Degree Days,Wind Gust (mph),
	//Rainfaill last 3 hours (inches),Rainfaill last 6 hours (inches),Rainfaill last 24 hours (inches),Max Temperature last 24 hours (degreesF),Min Temperature last 24 hours (degreesF),
	//cloud Ceiling (feet),visibility (feet),PresentWeather,SkyConditions
	//	private static final String server = "webservices.anythingweather.com";
	//	private static final String path = "/CurrentObs/GetCurrentObs";
	private final String server; //= "http://192.168.1.91:8080"; // ?clientId=BuildingIQ&accessKey=b42r10a49a474zn5&format=CSV&stationId=3340&startTime=2016-01-01&stopTime=2016-01-02";
	private final String path;  // = "/CurrentObs/GetCurrentObs";

	public MetarDataPoller(String server, String path) {
		this.server = server;
		this.path = path;
	}

	/**
	 * 
	 * @return the last available data record
	 */
	public List<Metar> pollStationData(String stationID) {
		String csvData = pollServer(stationID);
		List<Metar> recs = new ArrayList<>();
		String [] lines = csvData.split("\\n");
		//  first line is header- we can skip it
		for(String l : lines) {
			Metar rec = new Metar();
			String [] vals = (lines[1]+",END").split(",");
			Station station = new Station();
			rec.stationID = vals[0];
//			rec.setTimeStringUtc(vals[4].replace(" ",	"T")+ "Z");
//			DateTime dt = new DateTime(rec.getTimeStringUtc());
//			rec.setTimeUtc(dt.getMillis());
			rec.setTemperatureC(parseDouble(vals[6]));
			rec.setDewPointC(parseDouble(vals[7]));
//			rec.setRelativeHumidity(parseDouble(vals[8]));
			rec.setWindSpeed(parseDouble(vals[9]));
			rec.windDirection = (int)parseDouble(vals[10]);
			rec.pressure  = parseDouble(vals[11]);
			rec.windGust = parseDouble(vals[18]);
//			rec.setCloudCeiling((int)parseDouble(vals[23]));
//			rec.setVisibility((int)parseDouble(vals[24]));
			rec.hourlyPrecipInches = parseDouble(vals[12]);
//			rec.setPresentWeather(vals[25].trim());
//			rec.setSkyConditions(vals[26].trim());
			recs.add(rec);
		}
		return recs;
	}

	private String pollServer(String stationID) {
		String startTime = "2017-06-10";
		String stopTime = "2017-06-15";
		URI uri;
		try {
			uri = new URIBuilder()
					.setScheme("http")
					.setHost(server)
					.setPath(path)
					.setParameter("clientId", "SensorHub")
					.setParameter("accessKey", "mMHAkRZMtQfBmZvH")
					.setParameter("format", "csv")
					.setParameter("startTime", startTime + "")
					.setParameter("stopTime", stopTime + "")
					.setParameter("allData", "true")
					.setParameter("stationName", stationID)
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
		MetarDataPoller poller = new MetarDataPoller("66.37.155.91:8080","/CurrentObs/GetCurrentObs");
		poller.pollStationData("KAUS");
	}
}
