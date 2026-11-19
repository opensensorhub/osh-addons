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

/**
 * Interface specification to handle changes in button state.
 *
 * @author Nicolas Garay
 * @since Mar 20, 2023
 */
public interface ButtonStateChange {

    /**
     * Handle a change in state of a button
     *
     * @param oldState The old state
     * @param newState The new state
     * @throws UnhandledStateChangeException if the state change is not handled
     */
    default void onStateChanged(ButtonState oldState, ButtonState newState) throws UnhandledStateChangeException {

        throw new UnhandledStateChangeException("Old State: " + oldState.name() + " | New State: " + newState.name());
    }
}
