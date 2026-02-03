package org.sensorhub.controller.interfaces;

import net.java.games.input.Component;

public interface IObserver extends Runnable {

    //ConcurrentHashMap<Component.Identifier, ControllerUpdateListener> controllerListeners = null;
    void addListener(ControllerUpdateListener listener, Component.Identifier component);
    void removeListener(ControllerUpdateListener listener, Component.Identifier component);
    void doStart();
    void doStop();
    boolean isRunning();
}
