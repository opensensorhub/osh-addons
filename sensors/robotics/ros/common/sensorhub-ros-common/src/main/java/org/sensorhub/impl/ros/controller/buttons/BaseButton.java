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

/**
 * Abstract base class for all buttons, handling button definition and state changes
 * to register actions.
 *
 * @author Nicolas Garay
 * @since Mar 20, 2023
 */
public abstract class BaseButton implements ButtonStateChange {

    /**
     * Button class
     */
    private ButtonClass buttonClass = ButtonClass.UNDEFINED;

    /**
     * Button state
     */
    private ButtonState buttonState = ButtonState.RELEASED;

    /**
     * Name of the button
     */
    private String name = "UNDEFINED";

    /**
     * The index of the button to be used with the {@link ButtonManager}
     */
    protected int joyButtonIdx = -1;

    /**
     * Hidden default constructor
     */
    private BaseButton() {
    }

    /**
     * Constructor
     *
     * @param buttonClass The {@link ButtonClass} of this button
     */
    protected BaseButton(ButtonClass buttonClass) {

        this.buttonClass = buttonClass;
    }

    /**
     * Constructor
     *
     * @param buttonClass The {@link ButtonClass} of this button
     * @param name        a name for the button
     */
    protected BaseButton(ButtonClass buttonClass, String name) {

        this(buttonClass);
        this.name = name;
    }

    /**
     * Constructor
     *
     * @param buttonClass  The {@link ButtonClass} of this button
     * @param name         a name for the button
     * @param joyButtonIdx the index identifying the button to the {@link ButtonManager}
     */
    protected BaseButton(ButtonClass buttonClass, String name, int joyButtonIdx) {

        this(buttonClass, name);
        this.joyButtonIdx = joyButtonIdx;
    }

    /**
     * Gets the button class, as per {@link ButtonClass}
     *
     * @return The registered button class
     */
    public final ButtonClass getButtonClass() {
        return buttonClass;
    }

    /**
     * Gets the name of the button
     *
     * @return the name of the button
     */
    public final String getName() {
        return name;
    }

    /**
     * Gets the buttons current state
     *
     * @return the current state of the button
     */
    public final ButtonState getButtonState() {
        return buttonState;
    }

    /**
     * Sets the button state
     *
     * @param buttonState the new state of the button
     */
    public final void setButtonState(ButtonState buttonState) {

        ButtonState oldButtonState = this.buttonState;
        this.buttonState = buttonState;

        onStateChanged(oldButtonState, buttonState);
    }
}
