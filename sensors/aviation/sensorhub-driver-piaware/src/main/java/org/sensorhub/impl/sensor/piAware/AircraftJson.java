package org.sensorhub.impl.sensor.piAware;

public class AircraftJson {
	Double now;
	Integer messages;
	Aircraft[] aircraft;

	class Aircraft {
		String hex;		
		String flight; // This corresponds to callSign- not to be confused with flightId in the SBS format.    
		String category;
		long lastMessage;
	}

}
