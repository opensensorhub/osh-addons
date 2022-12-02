package org.sensorhub.impl.sensor.piAware;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.sensorhub.impl.sensor.piAware.AircraftJson.Aircraft;

import com.google.gson.Gson;

public class AircraftReader { // implements Runnable {

	Map<String, Aircraft> aircraftMap = new HashMap<>(); // hexIdent, Aircraft
	Path jsonPath; // only for testing locally
	URL aircraftUrl;
	ReaderTask readerTask; 
	
	public AircraftReader(Path jsonPath) {
		this.jsonPath = jsonPath;
	}

	public AircraftReader(String aircraftUrl) throws MalformedURLException {
		this.aircraftUrl = new URL(aircraftUrl);
	}

	public Aircraft getAircraft(String hexIdent) {
		return aircraftMap.get(hexIdent);
	}

//	volatile boolean running = false;
//	@Override
	class ReaderTask extends TimerTask {
		public void run() {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(aircraftUrl.openStream()))) {
				Gson gson = new Gson();
				AircraftJson aircraftJson = gson.fromJson(reader, AircraftJson.class);
				for(Aircraft aircraft: aircraftJson.aircraft) {
					if(aircraft.flight != null)
						aircraft.flight = aircraft.flight.trim();
					aircraft.hex = aircraft.hex.trim().toUpperCase();
					// Check for existing aircraft without flightID and update if it's there
					Aircraft existing = aircraftMap.get(aircraft.hex);
					if(existing == null) {
						aircraftMap.put(aircraft.hex, aircraft);
					} else {
 						if(existing.flight == null && aircraft.flight != null)
							existing.flight = aircraft.flight;
 						if(existing.category == null && aircraft.category != null)
 							existing.category = aircraft.category;
					}
				}
//				System.err.println(aircraftMap.size() + " planes in map");
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
	}
	Timer timer;
	public void startReaderTask() {
		readerTask = new ReaderTask();
		timer = new Timer();
		timer.scheduleAtFixedRate(readerTask, 0, 1000L);
	}
	
	public void stopReaderTask() {
		readerTask.cancel();
		timer.cancel();
	}
	
	public static void main(String[] args) throws Exception {
		String jsonUrl = "http://192.168.1.126:8080/data/aircraft.json";
		AircraftReader reader = new AircraftReader(jsonUrl);
		reader.startReaderTask();
		System.err.println("started");
		Thread.sleep(60_000L);
//		Aircraft ac = reader.getAircraft("A1AE05");
//		System.err.println(ac.flight);
		reader.stopReaderTask();
		System.err.println("stopped");
	}
	
}
