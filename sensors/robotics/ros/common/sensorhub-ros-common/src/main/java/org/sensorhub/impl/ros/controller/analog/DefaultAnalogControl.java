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
import net.opengis.swe.v20.DataComponent;
import org.apache.commons.lang.WordUtils;
import org.vast.swe.SWEHelper;

/**
 * Default implementation of analog control abstraction.  Implements
 * {@link BaseAnalogControl#onDirectionChanged(Direction, Direction)}
 * to handle updating the analog control's state with the {@link AnalogControlManager}
 *
 * @author Nicolas Garay
 * @since Mar 20, 2023
 */
public class DefaultAnalogControl extends BaseAnalogControl {

    /**
     * The analog control manager managing this control's state data
     */
    private final AnalogControlManager analogControlManager;

    /**
     * SWE Common Data Component describing the analog control
     */
    protected final DataComponent component;

    /**
     * Constructor
     *
     * @param analogControlManager   Instance of {@link AnalogControlManager} used with this control
     * @param placement              The relative placement of the control
     * @param joyAnalogHorizontalIdx The index id of the horizontal motion of the analog controller
     * @param joyAnalogVerticalIdx   The index id of the vertical motion of the analog controller
     */
    protected DefaultAnalogControl(AnalogControlManager analogControlManager, ControlPlacement placement, int joyAnalogHorizontalIdx, int joyAnalogVerticalIdx) {

        super(placement, joyAnalogHorizontalIdx, joyAnalogVerticalIdx);

        this.analogControlManager = analogControlManager;

        SWEHelper factory = new SWEHelper();

        component = factory.createCategory()
                .name(placement.name().toLowerCase() + "AnalogControl")
                .label(WordUtils.capitalize(placement.name().toLowerCase()) + " Analog Control")
                .definition(SWEHelper.getPropertyUri(WordUtils.capitalize(placement.name().toLowerCase()) + "AnalogControl"))
                .description("On a game controller, this is the " + placement.name().toLowerCase() + " handed analog stick")
                .addAllowedValues(
                        Direction.NEUTRAL.name(),
                        Direction.UP.name(),
                        Direction.UPPER_RIGHT.name(),
                        Direction.RIGHT.name(),
                        Direction.LOWER_RIGHT.name(),
                        Direction.DOWN.name(),
                        Direction.LOWER_LEFT.name(),
                        Direction.LEFT.name(),
                        Direction.UPPER_LEFT.name()
                )
                .value(Direction.NEUTRAL.name())
                .build();
    }

    /**
     * Returns the SWE Data Component describing this control
     *
     * @return the SWE Data Component describing this control
     */
    public final DataComponent getSweDataComponent() {

        return component;
    }

    /**
     * Handles changes in control direction by updating the state of the control with
     * the {@link DefaultAnalogControl#analogControlManager}.  Based on new direction
     * the state of the control is updated using the appropriate index identifying
     * the control to the {@link DefaultAnalogControl#analogControlManager}.
     *
     * @param oldDirection The previous direction
     * @param newDirection The new direction
     * @throws UnhandledStateChangeException if the change is unhandled
     */
    @Override
    public void onDirectionChanged(Direction oldDirection, Direction newDirection) throws UnhandledStateChangeException {

        switch (newDirection) {

            case NEUTRAL:
                analogControlManager.setAxisValue(joyAnalogHorizontalIdx, 0.0f);
                analogControlManager.setAxisValue(joyAnalogVerticalIdx, 0.0f);
                break;
            case UP:
                analogControlManager.setAxisValue(joyAnalogHorizontalIdx, 0.0f);
                analogControlManager.setAxisValue(joyAnalogVerticalIdx, 1.0f);
                break;
            case UPPER_RIGHT:
                analogControlManager.setAxisValue(joyAnalogHorizontalIdx, -1.0f);
                analogControlManager.setAxisValue(joyAnalogVerticalIdx, 1.0f);
                break;
            case RIGHT:
                analogControlManager.setAxisValue(joyAnalogHorizontalIdx, -1.0f);
                analogControlManager.setAxisValue(joyAnalogVerticalIdx, 0.0f);
                break;
            case LOWER_RIGHT:
                analogControlManager.setAxisValue(joyAnalogHorizontalIdx, -1.0f);
                analogControlManager.setAxisValue(joyAnalogVerticalIdx, -1.0f);
                break;
            case DOWN:
                analogControlManager.setAxisValue(joyAnalogHorizontalIdx, 0.0f);
                analogControlManager.setAxisValue(joyAnalogVerticalIdx, -1.0f);
                break;
            case LOWER_LEFT:
                analogControlManager.setAxisValue(joyAnalogHorizontalIdx, 1.0f);
                analogControlManager.setAxisValue(joyAnalogVerticalIdx, -1.0f);
                break;
            case LEFT:
                analogControlManager.setAxisValue(joyAnalogHorizontalIdx, 1.0f);
                analogControlManager.setAxisValue(joyAnalogVerticalIdx, 0.0f);
                break;
            case UPPER_LEFT:
                analogControlManager.setAxisValue(joyAnalogHorizontalIdx, 1.0f);
                analogControlManager.setAxisValue(joyAnalogVerticalIdx, 1.0f);
                break;
            default:
                super.onDirectionChanged(oldDirection, newDirection);
                break;
        }
    }
}
