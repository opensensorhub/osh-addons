package org.sensorhub.controller.interfaces;

import org.sensorhub.controller.models.ControllerData;

public interface IController {

    boolean isConnected();
    void disconnect();
    IObserver getObserver();
    ControllerData getControllerData();

}
