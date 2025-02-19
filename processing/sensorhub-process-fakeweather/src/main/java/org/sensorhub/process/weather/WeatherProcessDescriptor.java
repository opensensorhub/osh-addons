package org.sensorhub.process.weather;

import org.sensorhub.impl.processing.AbstractProcessProvider;

public class WeatherProcessDescriptor extends AbstractProcessProvider {

    public WeatherProcessDescriptor() {
        addImpl(WeatherProcess.INFO);
    }

}