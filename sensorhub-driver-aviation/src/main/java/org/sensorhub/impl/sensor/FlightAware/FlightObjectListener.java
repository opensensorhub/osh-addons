package org.sensorhub.impl.sensor.FlightAware;

public interface FlightObjectListener
{
	public void processMessage(FlightObject obj);
//	public void processMessage(FlightObject obj, String message);
}
