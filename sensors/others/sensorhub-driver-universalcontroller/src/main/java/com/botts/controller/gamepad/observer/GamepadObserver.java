package com.botts.controller.gamepad.observer;

import com.botts.controller.gamepad.Gamepad;
import com.botts.controller.interfaces.IObserver;
import com.botts.controller.interfaces.ControllerUpdateListener;
import net.java.games.input.*;
import net.java.games.input.Component.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class to listen for controller input events registered as ControllerUpdateListener objects
 */
public class GamepadObserver implements IObserver {

    /**
     * The list of event listeners registered to this observer
     */
    private Map<Identifier, ControllerUpdateListener> gamepadListeners;

    /**
     * Event object for underlying plugin to populate
     */
    private Event event;

    /**
     * The controller to which the observer is applied
     */
    private Controller gamepad;

    private Gamepad parent;

    /**
     * Lock for thread safety
     */
    private static final Object lock = new Object();

    /**
     * Worker thread for observing events
     */
    private Thread worker;

    private String threadName = "GamepadObserverThread:";

    private boolean running = false;

    /**
     * Singleton constructor
     */
    public GamepadObserver(Gamepad parent, Controller gamepad, Event event) {
        this.event = event;
        this.parent = parent;
        this.gamepad = gamepad;

        threadName += parent.getControllerData().getName();

        gamepadListeners = new ConcurrentHashMap<>();
    }

    /**
     * Starts the worker thread to listen for events
     */
    @Override
    public void doStart() {
        try {
            if(event == null) {
                throw new IllegalStateException("Event must be initialized and set using setEvent()");
            }
            if(worker != null && worker.isAlive()) {
                throw new IllegalStateException("Observer is already listening for events");
            }
            running = true;
            worker = new Thread(this, this.threadName);
            worker.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Stop observer's worker thread
     */
    @Override
    public void doStop() {
        running = false;
        System.out.println("Observer stopped manually");
    }

    /**
     * Check if observer's worker thread is running
     */
    @Override
    public boolean isRunning() {
        running = worker.isAlive();
        return running;
    }

    /**
     * Adds an event listener to the observer's list of event listeners
     *
     * @param listener ControllerUpdateListener object whose callback method onChange() will be executed on notification of change in the controller component identified by the component identifier
     * @param component Controller component identifier to specify which component update notification needs to update which callback function
     */
    @Override
    public void addListener(ControllerUpdateListener listener, Identifier component) {
        gamepadListeners.put(component, listener);
    }

    /**
     * Removes an event listener to the observer's list of event listeners
     *
     * @param listener ControllerUpdateListener object whose callback method onChange() will be executed on notification of change in the controller component identified by the component identifier
     * @param component Controller component identifier to specify which component update notification needs to update which callback function
     */
    @Override
    public void removeListener(ControllerUpdateListener listener, Identifier component) {
        gamepadListeners.remove(component, listener);
    }

    /**
     * Thread worker used for listening for controller events
     */
    @Override
    public void run() {
        while(running) {
            if(event != null && gamepad != null) {
                synchronized (this) {
                    EventQueue queue = gamepad.getEventQueue();
                    gamepad.poll();

                    for (int i = 0; i < gamepad.getComponents().length; i++) {
                        parent.getControllerData().getOutputs().get(i).setValue(gamepad.getComponents()[i].getPollData());
                    }

                    if(queue.getNextEvent(event)) {
                        Component eventComponent = event.getComponent();
                        for(Map.Entry<Identifier, ControllerUpdateListener> entry : gamepadListeners.entrySet()) {
                            if(eventComponent.getIdentifier() == entry.getKey()) {
                                entry.getValue().onChange(String.valueOf(entry.getKey()), eventComponent.getPollData());
                            }
                        }
                    }
                }
            }
        }
    }
}
