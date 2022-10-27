package org.sensorhub.impl.sensor.piAware;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.sensorhub.impl.sensor.piAware.AircraftReader.AircraftJson.Aircraft;

import com.google.gson.Gson;

@Deprecated // Not being used- confirm and removes
public class AircraftReader {

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
	
	public static Aircraft[] getAircraft(Path path) throws Exception {
//		try (Reader reader = Files.newBufferedReader(Paths.get("src/main/resources/aircraft.json"))) {
		try (Reader reader = Files.newBufferedReader(path)) {
			Gson gson = new Gson();
			AircraftJson aircraft = gson.fromJson(reader, AircraftJson.class);
//			System.out.println(aircraft);
			return aircraft.getAircraft();
		} catch (Exception ex) {
			throw(ex);
		}
	}
	
	public static void main(String[] args) throws Exception {
		Aircraft [] aircrafts = getAircraft(Paths.get("src/main/resources/aircraft.json"));
		for(Aircraft a: aircrafts)
			System.err.println(a.hex + " : " + a.flight);
	}
}
