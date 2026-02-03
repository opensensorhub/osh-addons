package com.botts.controller.interfaces;

import com.botts.controller.models.ControllerData;

public interface IController {

    boolean isConnected();
    void disconnect();
    IObserver getObserver();
    ControllerData getControllerData();

}
