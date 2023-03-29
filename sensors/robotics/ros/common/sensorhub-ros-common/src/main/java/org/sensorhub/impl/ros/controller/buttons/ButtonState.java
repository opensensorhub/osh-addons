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
 * An enumeration of button states
 */
public enum ButtonState {

    PRESSED,
    LONG_PRESS,
    RELEASED;

    public static ButtonState fromString(String name) {
        for (ButtonState state : ButtonState.values()) {
            if (state.name().equalsIgnoreCase(name)) {
                return state;
            }
        }
        return RELEASED;
    }
}
