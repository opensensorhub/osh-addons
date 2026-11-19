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
 * Base analog control abstraction.  Allows for creation of analog controls to be used
 * and managed by the {@link AnalogControlManager}
 *
 * @author Nicolas Garay
 * @since Mar 20, 2023
 */
public abstract class BaseAnalogControl implements DirectionChange {

    /**
     * Relative position of the analog control
     */
    private ControlPlacement placement = ControlPlacement.UNDEFINED;

    /**
     * Direction of motion of the analog control, {@link Direction#NEUTRAL}
     * denotes it is in its default position.
     */
    private Direction direction = Direction.NEUTRAL;

    /**
     * The index id of the vertical motion of the analog controller
     */
    protected int joyAnalogVerticalIdx = -1;

    /**
     * The index id of the horizontal motion of the analog controller
     */
    protected int joyAnalogHorizontalIdx = -1;

    /**
     * Hidden default constructor
     */
    private BaseAnalogControl() {
    }

    /**
     * Constructor, creates an analog control with given placement and indices for state management of
     * horizontal and vertical motion.
     *
     * @param placement              The relative placement of the control
     * @param joyAnalogHorizontalIdx index for state management of horizontal motion
     * @param joyAnalogVerticalIdx   index for state management of and vertical motion
     */
    protected BaseAnalogControl(ControlPlacement placement, int joyAnalogHorizontalIdx, int joyAnalogVerticalIdx) {

        this.placement = placement;
        this.joyAnalogVerticalIdx = joyAnalogVerticalIdx;
        this.joyAnalogHorizontalIdx = joyAnalogHorizontalIdx;
    }

    /**
     * A string representing the analog control based on its placement
     *
     * @return string representing the analog control based on its placement
     */
    public final String getName() {

        return placement.name();
    }

    /**
     * Placement of the control
     *
     * @return placement of the control
     */
    public final ControlPlacement getPlacement() {

        return placement;
    }

    /**
     * Updates the direction and fires off event containing old and new directions of movement
     *
     * @param direction the new direction of movement
     */
    public final void setDirection(Direction direction) {

        Direction oldDirection = this.direction;
        this.direction = direction;

        onDirectionChanged(oldDirection, direction);
    }

    /**
     * Retrieves the current direction of movement
     *
     * @return the current direction of movement
     */
    public final Direction getDirection() {
        return direction;
    }
}
