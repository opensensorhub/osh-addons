package org.sensorhub.impl.sensor.mavsdk.processing;

import org.sensorhub.impl.processing.AbstractProcessProvider;

public class PD extends AbstractProcessProvider {

    public PD() {
        addImpl(ConstAltitudeLLA.INFO);
    }

}
