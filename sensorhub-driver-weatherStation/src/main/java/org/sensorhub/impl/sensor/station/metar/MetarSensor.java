package org.sensorhub.impl.sensor.station.metar;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;


public class MetarSensor extends AbstractSensorModule<MetarConfig> //extends StationSensor
{
	MetarOutput metarInterface;

	
	public MetarSensor() {
		metarInterface = new MetarOutput(this);
		addOutput(metarInterface, false);
		metarInterface.init();	
	}

	
	@Override
	protected void updateSensorDescription()
	{
		synchronized (sensorDescription)
		{
			super.updateSensorDescription();
			sensorDescription.setUniqueIdentifier("urn:test:sensors:weather:metar");
			sensorDescription.setDescription("METAR weather station");
		}
	}


	@Override
	public void start() throws SensorHubException
	{
		metarInterface.start();        
	}


	@Override
	public void stop() throws SensorHubException
	{
		metarInterface.stop();
	}


	@Override
	public void cleanup() throws SensorHubException
	{

	}


	@Override
	public boolean isConnected()
	{
		return true;
	}
}
