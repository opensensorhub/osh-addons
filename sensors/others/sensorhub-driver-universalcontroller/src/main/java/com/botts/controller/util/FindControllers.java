package com.botts.controller.util;

import com.botts.controller.gamepad.GamepadConnection;
import com.botts.controller.wii.WiiMoteConnection;
import com.botts.controller.interfaces.IController;
import com.botts.controller.interfaces.IControllerConnection;
import com.botts.controller.models.ControllerType;
import net.java.games.input.Event;

import java.util.*;

public class FindControllers {

    // TODO: Search for wii mote or usb controllers
    private GamepadConnection gamepadConnection;
    private WiiMoteConnection wiiMoteConnection;
    private ArrayList<IController> controllers;
    private Event event;
    private ControllerType[] types;
    private long searchTime = 5000;
    public FindControllers(Event event) {
        controllers = new ArrayList<>();

        try {
            gamepadConnection = new GamepadConnection(event);
            if(gamepadConnection.getConnectedControllers() != null && !gamepadConnection.getConnectedControllers().isEmpty()) {
                controllers.addAll(gamepadConnection.getConnectedControllers());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            wiiMoteConnection = new WiiMoteConnection(searchTime);
            if(wiiMoteConnection.getConnectedControllers() != null && !wiiMoteConnection.getConnectedControllers().isEmpty()) {
                controllers.addAll(wiiMoteConnection.getConnectedControllers());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public FindControllers(long searchTime, Event event, ControllerType... types) {
        controllers = new ArrayList<>();
        this.searchTime = searchTime;
        this.event = event;
        this.types = types;
        for(ControllerType type : this.types) {
            switch(type) {
                case GAMEPAD -> {
                    try {
                        gamepadConnection = new GamepadConnection(this.event);
                        if (gamepadConnection.getConnectedControllers() != null && !gamepadConnection.getConnectedControllers().isEmpty()) {
                            controllers.addAll(gamepadConnection.getConnectedControllers());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                case WIIMOTE -> {
                    try {
                        wiiMoteConnection = new WiiMoteConnection(searchTime);
                        if(wiiMoteConnection.getConnectedControllers() != null && !wiiMoteConnection.getConnectedControllers().isEmpty()) {
                            controllers.addAll(wiiMoteConnection.getConnectedControllers());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                default -> throw new IllegalStateException("No controller connections requested.");
            }
        }
    }

    public IControllerConnection getControllerConnection(ControllerType type) {
        switch (type) {
            case WIIMOTE -> {
                return wiiMoteConnection;
            }
            case GAMEPAD -> {
                return gamepadConnection;
            }
            default -> {
                return null;
            }
        }
    }

    public ArrayList<IController> getControllers() {
        return controllers;
    }

    public ArrayList<IController> getControllers(ControllerType controllerType) {
        ArrayList<IController> filteredControllers = null;
        try {
            filteredControllers = (ArrayList<IController>) controllers.stream().filter((controller) -> Objects.equals(controller.getControllerData().getControllerType(), controllerType));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return filteredControllers;
    }

    public IController getController(String controllerName) {
        return (IController) controllers.stream().filter((controller) -> Objects.equals(controller.getControllerData().getName(), controllerName));
    }

}
