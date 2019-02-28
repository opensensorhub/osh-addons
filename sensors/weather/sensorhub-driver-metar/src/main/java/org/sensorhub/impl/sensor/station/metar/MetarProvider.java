package org.sensorhub.impl.sensor.station.metar;

import java.util.List;

public interface MetarProvider
{
	public List<Metar> getMetars();
}
