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
 * Abstraction of an analog button, typically these buttons are part of an analog control
 * that allow for actions to be executed based on pressing down on the analog control relative
 * to the base controller.  This is not to be confused with moving the analog control in the
 * down direction.
 *
 * @author Nicolas Garay
 * @since Mar 20, 2023
 */
public abstract class AnalogButton extends BaseButton {

    /**
     * Constructor
     */
    protected AnalogButton() {

        super(ButtonClass.ANALOG);
    }

    /**
     * Constructor
     *
     * @param name a name for the button
     */
    protected AnalogButton(String name) {

        super(ButtonClass.ANALOG, name);
    }

    /**
     * Constructor
     * @param name a name for the button
     *
     * @param joyButtonIdx the index identifying the button to the {@link ButtonManager}
     */
    protected AnalogButton(String name, int joyButtonIdx) {

        super(ButtonClass.ANALOG, name, joyButtonIdx);
    }
}
