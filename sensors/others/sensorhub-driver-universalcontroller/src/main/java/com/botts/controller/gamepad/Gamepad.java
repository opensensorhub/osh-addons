package com.botts.controller.gamepad;

import com.botts.controller.gamepad.observer.GamepadObserver;
import com.botts.controller.interfaces.IController;
import com.botts.controller.interfaces.IObserver;
import com.botts.controller.models.ControllerComponent;
import com.botts.controller.models.ControllerData;
import com.botts.controller.models.ControllerType;
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
