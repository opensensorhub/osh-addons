package org.sensorhub.impl.sensor.piAware;

public class AircraftJson {
	Double now;
	Integer messages;
	Aircraft[] aircraft;

	class Aircraft {
		String hex;
		String flight;
		String category;
		long lastMessage;
	}

}
