package org.sensorhub.impl.sensor.flightAware;

public interface FlightObjectListener
{
	public void processMessage(FlightObject obj);
//	public void processMessage(FlightObject obj, String message);
}
