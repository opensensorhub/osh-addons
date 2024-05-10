/*
 * The contents of this file are subject to the Mozilla Public License, v. 2.0.
 *
 * If a copy of the MPL was not distributed with this file, You can obtain
 * one at http://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * See the License for the specific language governing rights and
 * limitations under the License.
 *
 * Copyright (C) 2021-2024 Botts Innovative Research, Inc. All Rights Reserved.
 */
package org.sensorhub.impl.sensor.pibot.drive;

/**
 * Enumeration of the current state values of the DriveSensor, state values correspond to one of:
 * <code>STOP</code>
 * <code>FORWARD</code>
 * <code>FORWARD_TURN_LEFT</code>
 * <code>FORWARD_TURN_RIGHT</code>
 * <code>SPIN_LEFT</code>
 * <code>SPIN_RIGHT</code>
 * <code>REVERSE_TURN_LEFT</code>
 * <code>REVERSE_TURN_RIGHT</code>
 * <code>REVERSE</code>
 * <code>UNKNOWN</code>
 *
 * @author Nick Garay
 * @since Feb. 15, 2021
 */
public enum DriveDirection {

    STOP,
    FORWARD,
    FORWARD_TURN_LEFT,
    FORWARD_TURN_RIGHT,
    SPIN_LEFT,
    SPIN_RIGHT,
    REVERSE_TURN_LEFT,
    REVERSE_TURN_RIGHT,
    REVERSE,
    UNKNOWN;

    public static DriveDirection fromString(String name) {
        for (DriveDirection state : DriveDirection.values()) {
            if (state.name().equalsIgnoreCase(name)) {
                return state;
            }
        }
        return UNKNOWN;
    }
}
