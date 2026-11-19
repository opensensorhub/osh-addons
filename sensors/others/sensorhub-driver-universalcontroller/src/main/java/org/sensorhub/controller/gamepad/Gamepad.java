package org.sensorhub.controller.gamepad;

import org.sensorhub.controller.gamepad.observer.GamepadObserver;
import org.sensorhub.controller.interfaces.IController;
import org.sensorhub.controller.interfaces.IObserver;
import org.sensorhub.controller.models.ControllerComponent;
import org.sensorhub.controller.models.ControllerData;
import org.sensorhub.controller.models.ControllerType;
import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.Event;

import java.util.ArrayList;

public class Gamepad implements IController {

    private Controller jinputGamepad;
    private GamepadObserver gamepadObserver;
    private ControllerData controllerData;

    public Gamepad(Controller jinputGamepad, Event event, int id) {
        this.jinputGamepad = jinputGamepad;

        Component[] jinputComponents = jinputGamepad.getComponents();
        ArrayList<ControllerComponent> components = new ArrayList<>();

        for (Component component : jinputComponents) {
            components.add(new ControllerComponent(component.getIdentifier().getName(), component.getPollData()));
        }

        controllerData = new ControllerData(jinputGamepad.getName() + ":" + id, ControllerType.GAMEPAD, components);
        gamepadObserver = new GamepadObserver(this, jinputGamepad, event);
        gamepadObserver.doStart();
    }

    @Override
    public boolean isConnected() {
        return jinputGamepad.poll();
    }

    @Override
    public void disconnect() {

    }

    @Override
    public IObserver getObserver() {
        return gamepadObserver;
    }

    @Override
    public ControllerData getControllerData() {
        return controllerData;
    }
}
