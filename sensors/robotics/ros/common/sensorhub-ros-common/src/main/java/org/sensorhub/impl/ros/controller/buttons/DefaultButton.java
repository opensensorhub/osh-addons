/*
 *  The contents of this file are subject to the Mozilla Public License, v. 2.0.
 *  If a copy of the MPL was not distributed with this file, You can obtain one
 *  at http://mozilla.org/MPL/2.0/.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 *  for the specific language governing rights and limitations under the License.
 *
 *  Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 */
package org.sensorhub.impl.ros.controller.buttons;

import org.sensorhub.impl.ros.controller.UnhandledStateChangeException;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.impl.ros.controller.analog.AnalogControlManager;
import org.sensorhub.impl.ros.controller.analog.BaseAnalogControl;
import org.sensorhub.impl.ros.controller.analog.DefaultAnalogControl;
import org.sensorhub.impl.ros.controller.analog.Direction;
import org.vast.swe.SWEHelper;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Default implementation of button control abstraction.  Implements
 * {@link BaseButton#onStateChanged(ButtonState, ButtonState)}
 * to handle updating the button's state with the {@link ButtonManager}
 *
 * @author Nicolas Garay
 * @since Mar 20, 2023
 */
public abstract class DefaultButton extends BaseButton {

    /**
     * The button manager that will manage this button
     */
    private final ButtonManager buttonManager;

    /**
     * A scheduler that will be used to ensure button presses that are long
     * revert to release after the timeout time.
     */
    private final ScheduledExecutorService scheduler;

    /**
     * SWE Common Data Component describing the button
     */
    protected final DataComponent component;

    /**
     * Constructor
     *
     * @param buttonManager the button manager used to manage this button
     * @param buttonClass   the class of button
     * @param name          the name to assign to the button
     * @param buttonIdx     the index used to identify the button to the button manager
     */
    protected DefaultButton(final ButtonManager buttonManager, final ButtonClass buttonClass, final String name, int buttonIdx) {
        super(buttonClass, name, buttonIdx);
        this.buttonManager = buttonManager;
        scheduler = new ScheduledThreadPoolExecutor(1);

        SWEHelper factory = new SWEHelper();

        component = factory.createCategory()
                .name(buttonClass.name().toLowerCase() + name)
                .label(name + " Button")
                .definition(SWEHelper.getPropertyUri(name.toLowerCase().replace(" ", "") + "Button"))
                .description("On a game controller, the " + name + " button")
                .addAllowedValues(
                        ButtonState.RELEASED.name(),
                        ButtonState.PRESSED.name(),
                        ButtonState.LONG_PRESS.name()
                )
                .value(ButtonState.RELEASED.name())
                .build();
    }

    /**
     * Returns the SWE Data Component describing this button
     *
     * @return the SWE Data Component describing this button
     */
    public final DataComponent getSweDataComponent() {

        return component;
    }

    /**
     * Handles changes in button state by updating the state of the button with
     * the {@link DefaultButton#buttonManager}.  Based on new button state
     * the state of the button is updated using the appropriate index identifying
     * the control to the {@link DefaultButton#buttonManager}.
     *
     * @param oldState The previous state
     * @param newState The new state
     * @throws UnhandledStateChangeException if the change is unhandled
     */
    @Override
    public void onStateChanged(ButtonState oldState, ButtonState newState) throws UnhandledStateChangeException {

        switch (newState) {

            case PRESSED:
                buttonManager.setButtonValue(joyButtonIdx, 1);
                break;

            case LONG_PRESS:
                buttonManager.setButtonValue(joyButtonIdx, 1);
                scheduler.schedule(() -> setButtonState(ButtonState.RELEASED), 1000, TimeUnit.MILLISECONDS);
                break;

            case RELEASED:
                buttonManager.setButtonValue(joyButtonIdx, 0);
                break;

            default:
                super.onStateChanged(oldState, newState);
                break;
        }
    }
}
