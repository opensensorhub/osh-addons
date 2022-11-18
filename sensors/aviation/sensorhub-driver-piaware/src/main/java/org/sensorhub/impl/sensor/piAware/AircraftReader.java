package org.sensorhub.impl.sensor.piAware;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.sensorhub.impl.sensor.piAware.AircraftReader.AircraftJson.Aircraft;

import com.google.gson.Gson;

public class AircraftReader {

	Map<String, String> aircrafts = new HashMap<>();
	
	class AircraftJson {
		Double now;
		Integer messages;
		Aircraft [] aircraft;
		
		class Aircraft {
			String hex;
			String flight;
		}
		
		public Aircraft [] getAircraft() {
			return aircraft;
		}
	}
	
	public static Aircraft[] getAircraft(Reader reader) throws Exception {
//		try (Reader reader = Files.newBufferedReader(Paths.get("src/main/resources/aircraft.json"))) {
//		try (Reader reader = Files.newBufferedReader(path)) {
			Gson gson = new Gson();
			AircraftJson aircraft = gson.fromJson(reader, AircraftJson.class);
			return aircraft.getAircraft();
//		} catch (Exception ex) {
//			throw(ex);
//		}
	}
	
	public static void main(String[] args) throws Exception {
		String jsonUrl = "http://192.168.1.126:8080/data/aircraft.json";
		URL url = new URL(jsonUrl);
		BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
		Aircraft [] aircrafts = getAircraft(reader);
//		StringBuilder sb = new StringBuilder();
//		while(true) {
//			String s = is.readLine();
//			if(s == null || s.trim().length() == 0)  
//				break;
//			sb.append(s);
//		}
//		Aircraft [] aircrafts = getAircraft(Paths.get("src/main/resources/aircraft.json"));
		int cnt = 0;
		for(Aircraft a: aircrafts)
			if(a.flight != null) {
				System.out.println(a.hex + " : " + a.flight);
				cnt++;
			}
		System.out.println(cnt + " flights with tail #");
	}
}
