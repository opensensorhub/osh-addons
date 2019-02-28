package org.sensorhub.impl.sensor.station;


public interface StationDataPoller {
	public StationDataRecord pullStationData(String stationID);
}
