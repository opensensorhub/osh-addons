package org.sensorhub.controller.gamepad;

import org.sensorhub.controller.interfaces.IController;
import org.sensorhub.controller.interfaces.IControllerConnection;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Event;

import java.util.ArrayList;

public class GamepadConnection implements IControllerConnection {

    ArrayList<IController> connectedControllers;

    public GamepadConnection(Event event) {
        //System.setProperty("net.java.games.input.useDefaultPlugin", "false");

        Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
        ArrayList<Controller> gamepadControllers = new ArrayList<>();
        for (Controller controller : controllers) {
            if(controller.getType() == Controller.Type.GAMEPAD) {
                gamepadControllers.add(controller);
            }
        }
        if(gamepadControllers.isEmpty()) {
            connectedControllers = null;
            throw new IllegalStateException("No HID Gamepad controllers connected.");
        }
        connectedControllers = new ArrayList<>();
        for (Controller controller : gamepadControllers) {
            Gamepad gamepad = new Gamepad(controller, event, gamepadControllers.indexOf(controller));
            connectedControllers.add(gamepad);
        }
    }

    @Override
    public void disconnect() {

    }

    @Override
    public ArrayList<IController> getConnectedControllers() {
        return connectedControllers;
    }
}
