package org.sensorhub.impl.sensor.station;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class StationDataPoller {
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
	public StationDataRecord pullStationData() {
		StationDataRecord rec = new StationDataRecord();
		String csvData = pollServer();
		String [] lines = csvData.split("\\n");
		//  first line is header- second is latest data record
		String [] vals = lines[1].split(",");
		Station station = new Station();
		station.setName(vals[0]);
		rec.setStation(station);
		rec.setTemperature(Double.parseDouble(vals[6]));
		rec.setDewPoint(Double.parseDouble(vals[7]));
		rec.setRelativeHumidity(Double.parseDouble(vals[8]));
		return rec;
	}

	private String pollServer() {
		URI uri;
		try {
			uri = new URIBuilder()
			.setScheme("http")
			.setHost(server)
			.setPath(path)
			.setParameter("clientId", "awInternal")
			.setParameter("accessKey", "ZombifyMe")
			.setParameter("format", "csv")
			.setParameter("numHours", "2")
			.setParameter("allData", "true")
			.setParameter("stationid", "2477")
			.build();
			HttpGet httpget = new HttpGet(uri);
			System.out.println("executing request " + httpget.getURI());
			CloseableHttpClient httpclient = HttpClients.createDefault();
			CloseableHttpResponse response = httpclient.execute(httpget);
			try {
				BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
				StringBuffer result = new StringBuffer();
				String line;
				while ((line = rd.readLine()) != null) {
					result.append(line + "\n");
				}
				
				return result.toString();
			} finally {
				response.close();
				httpclient.close();
			}
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return null;
	}
	
	public static void main(String[] args) {
		StationDataPoller poller = new StationDataPoller();
		poller.pullStationData();
	}
}
