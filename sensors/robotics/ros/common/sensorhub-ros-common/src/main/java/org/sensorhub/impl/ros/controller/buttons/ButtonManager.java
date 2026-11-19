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

import java.util.HashMap;
import java.util.Map;

/**
 * Manages the state of button controls based on an button id
 *
 * @author Nicolas Garay
 * @since Mar 20, 2023
 */
public class ButtonManager {

    /**
     * An array to manage the button data, its size is defined at construction
     * to accommodate various types of controller buttons
     */
    private final int[] buttonData;

    /**
     * Map of button names to button objects
     */
    private final Map<String, BaseButton> buttons = new HashMap<>();

    /**
     * Constructor
     *
     * @param numButtons the number of buttons to manage
     * @param buttons    a list of buttons to manage
     * @throws IllegalArgumentException if the numButtons parameter is < 1
     */
    public ButtonManager(int numButtons, BaseButton... buttons) throws IllegalArgumentException {

        if (numButtons < 1) {

            throw new IllegalArgumentException("numButtons value must be > 0");
        }

        buttonData = new int[numButtons];

        registerButtons(buttons);
    }

    /**
     * Sets the value of a button given by its index
     *
     * @param idx   the index of the button, serving also as its identifier
     * @param value the value of the button
     * @throws IndexOutOfBoundsException if the idx < 0 || idx >= buttonData.length
     */
    protected final void setButtonValue(int idx, int value) throws IndexOutOfBoundsException {

        if (idx < 0 || idx >= buttonData.length) {

            throw new IndexOutOfBoundsException("Index " + idx + " outside range of (0, " + buttonData.length + "]");

        } else {

            buttonData[idx] = value;
        }
    }

    /**
     * Set the state of the button
     *
     * @param name  The name of the button whose state needs to be updated
     * @param state The new state for the button
     * @throws IllegalArgumentException if a button by the given name cannot be found
     */
    public final void setButtonState(String name, ButtonState state) throws IllegalArgumentException {

        if (buttons.containsKey(name)) {

            buttons.get(name).setButtonState(state);

        } else {

            throw new IllegalArgumentException("No button registered with name: " + name);
        }
    }

    /**
     * Retrieves the data for all buttons under management
     *
     * @return the int[] of button data
     */
    public final int[] getButtonData() {

        return buttonData;
    }

    /**
     * Adds a list of buttons to be managed
     *
     * @param buttons The list of buttons to manage
     */
    public void registerButtons(BaseButton... buttons) {

        for (BaseButton button : buttons) {

            this.buttons.put(button.getName(), button);
        }
    }
}
