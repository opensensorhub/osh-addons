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

import java.util.EnumMap;
import java.util.Map;

/**
 * Analog control manager, manages the state of analog controls based on an axis id
 *
 * @author Nicolas Garay
 * @since Mar 20, 2023
 */
public class AnalogControlManager {

    /**
     * An array to manage the axis data, its size is defined at construction
     * to accommodate various types of analog controls
     */
    private final float[] axisData;

    /**
     * A map of placement to control
     */
    private final Map<ControlPlacement, BaseAnalogControl> controls = new EnumMap<>(ControlPlacement.class);

    /**
     * Constructs the analog control manager
     *
     * @param numAxis  Specifies the number of axis for which the manager will manage state data
     * @param controls A list of controls to be managed
     * @throws IllegalArgumentException if the numAxis parameter is < 1
     */
    public AnalogControlManager(int numAxis, BaseAnalogControl... controls) throws IllegalArgumentException {

        if (numAxis < 1) {

            throw new IllegalArgumentException("numAxis value must be > 0");
        }
        axisData = new float[numAxis];

        registerAnalogControls(controls);
    }

    /**
     * Sets the value for the axis given by the index
     *
     * @param idx   The index of the axis to update
     * @param value The value representing the motion of the axis
     * @throws IndexOutOfBoundsException if the idx < 0 || idx >= axisData.length
     */
    public final void setAxisValue(int idx, float value) throws IndexOutOfBoundsException {

        if (idx < 0 || idx >= axisData.length) {

            throw new IndexOutOfBoundsException("Index " + idx + " outside range of (0, " + axisData.length + "]");

        } else {

            axisData[idx] = value;
        }
    }

    /**
     * Retrieves an analog control by placement
     *
     * @param placement The placement of the control
     * @return The control, if found by placement, or null
     */
    public BaseAnalogControl getControlByPlacement(ControlPlacement placement) {

        return controls.get(placement);
    }

    /**
     * Retrieve the state data for the axis under management.
     *
     * @return float[] of the axis state data
     */
    public final float[] getAxisData() {

        return axisData;
    }

    /**
     * Adds a list of controls to be managed
     *
     * @param controls The list of controls to manage
     */
    public final void registerAnalogControls(BaseAnalogControl... controls) {

        for (BaseAnalogControl control : controls) {

            this.controls.put(control.getPlacement(), control);
        }
    }
}
