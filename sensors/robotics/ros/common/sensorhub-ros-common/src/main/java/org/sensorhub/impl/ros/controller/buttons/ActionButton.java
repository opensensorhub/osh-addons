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
 * Abstraction of an {@link ButtonClass#ACTION} buttons
 * These buttons are typically the "A", "B", "C", "X", "Y" and "Z"
 * buttons on game controllers
 *
 * @author Nicolas Garay
 * @since Mar 20, 2023
 */
public abstract class ActionButton extends BaseButton {

    /**
     * Constructor
     */
    protected ActionButton() {

        super(ButtonClass.ACTION);
    }

    /**
     * Constructor
     *
     * @param name a name for the button
     */
    protected ActionButton(String name) {

        super(ButtonClass.ACTION, name);
    }

    /**
     * Constructor
     * @param name a name for the button
     *
     * @param joyButtonIdx the index identifying the button to the {@link ButtonManager}
     */
    protected ActionButton(String name, int joyButtonIdx) {

        super(ButtonClass.ACTION, name, joyButtonIdx);
    }
}
