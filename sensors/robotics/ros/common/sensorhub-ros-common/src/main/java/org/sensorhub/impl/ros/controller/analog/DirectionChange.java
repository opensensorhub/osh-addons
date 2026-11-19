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

import org.sensorhub.impl.ros.controller.UnhandledStateChangeException;

/**
 * Interface specification to handle changes in direction of motion related to analog controls.
 *
 * @author Nicolas Garay
 * @since Mar 20, 2023
 */
public interface DirectionChange {

    /**
     * Handle a change in direction of an analog control
     *
     * @param oldDirection The old direction
     * @param newDirection The new direction
     * @throws UnhandledStateChangeException if the direction change is not handled
     */
    default void onDirectionChanged(Direction oldDirection, Direction newDirection) throws UnhandledStateChangeException {

        throw new UnhandledStateChangeException("Old Direction: " + oldDirection.name() + " | New Direction: " + newDirection.name());
    }
}
