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
package org.sensorhub.impl.sensor.pibot.searchlight;

/**
 * Enumeration of the current state values of the SearchlightSensor, state values correspond to one of:
 * <code>OFF</code>
 * <code>WHITE</code>
 * <code>RED</code>
 * <code>MAGENTA</code>
 * <code>BLUE</code>
 * <code>CYAN</code>
 * <code>GREEN</code>
 * <code>YELLOW</code>
 * <code>UNKNOWN</code>
 *
 * @author Nick Garay
 * @since Jan. 24, 2021
 */
public enum SearchlightState {
    OFF,
    WHITE,
    RED,
    MAGENTA,
    BLUE,
    CYAN,
    GREEN,
    YELLOW,
    UNKNOWN;

    public static SearchlightState fromString(String name) {
        for (SearchlightState state : SearchlightState.values()) {
            if (state.name().equalsIgnoreCase(name)) {
                return state;
            }
        }
        return UNKNOWN;
    }
}
