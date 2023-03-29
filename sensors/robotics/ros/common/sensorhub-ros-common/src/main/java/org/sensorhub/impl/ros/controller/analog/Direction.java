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

package org.sensorhub.impl.ros.controller.analog;

/**
 * Enumeration of the allowed directions of movement for an analog control
 *
 * @author Nicolas Garay
 * @since Mar 20, 2023
 */
public enum Direction {

    NEUTRAL,
    UP,
    UPPER_RIGHT,
    RIGHT,
    LOWER_RIGHT,
    DOWN,
    LOWER_LEFT,
    LEFT,
    UPPER_LEFT;

    public static Direction fromString(String name) {
        for (Direction direction : Direction.values()) {
            if (direction.name().equalsIgnoreCase(name)) {
                return direction;
            }
        }
        return NEUTRAL;
    }
}
