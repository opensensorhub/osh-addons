package org.sensorhub.impl.sensor.piAware;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.sensorhub.impl.sensor.piAware.AircraftJson.Aircraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

public class AircraftReader { 

	Map<String, Aircraft> aircraftMap = new HashMap<>(); // hexIdent, Aircraft
	Path jsonPath; // only for testing locally
	URL aircraftUrl;
	Timer readerTimer;
	ReaderTask readerTask; 
	static final long READER_TIMER_PERIOD = 30_000L;
	Logger logger;
	
	public AircraftReader(Path jsonPath) {
		this.jsonPath = jsonPath;
		logger = LoggerFactory.getLogger(AircraftReader.class);
	}

	public AircraftReader(String aircraftUrl) throws MalformedURLException {
		this.aircraftUrl = new URL(aircraftUrl);
		logger = LoggerFactory.getLogger(AircraftReader.class);
	}

	public Aircraft getAircraft(String hexIdent) {
		return aircraftMap.get(hexIdent);
	}

	int taskCount = 1; 
//	@Override
	class ReaderTask extends TimerTask {
		public void run() {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(aircraftUrl.openStream()))) {
				logger.debug("ReaderTask opening. TaskCount = {}", taskCount++);
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
				logger.debug("{} planes in aircraftMap", aircraftMap.size());
			} catch (Throwable t) {
				logger.error("", t);
			}
		}
	}

	public void startReaderTask() {
		readerTask = new ReaderTask();
		readerTimer = new Timer();
		readerTimer.scheduleAtFixedRate(readerTask, 0, READER_TIMER_PERIOD);
	}
	
	public void stopReaderTask() {
		readerTask.cancel();
		readerTimer.cancel();
	}
	
	public static void main(String[] args) throws Exception {
		String jsonUrl = "http://192.168.1.101:8080/data/aircraft.json";
		AircraftReader reader = new AircraftReader(jsonUrl);
		reader.startReaderTask();
		System.err.println("started");
		Thread.sleep(600_000L);
		reader.stopReaderTask();
		System.err.println("stopped");
	}
	
}
