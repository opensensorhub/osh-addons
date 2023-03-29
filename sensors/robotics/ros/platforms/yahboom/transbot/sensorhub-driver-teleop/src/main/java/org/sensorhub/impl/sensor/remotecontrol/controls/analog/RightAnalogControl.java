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
package org.sensorhub.impl.sensor.remotecontrol.controls.analog;

import org.sensorhub.impl.ros.controller.analog.AnalogControlManager;
import org.sensorhub.impl.ros.controller.analog.ControlPlacement;
import org.sensorhub.impl.ros.controller.analog.DefaultAnalogControl;

public class RightAnalogControl extends DefaultAnalogControl {

    public RightAnalogControl(AnalogControlManager analogControlDataManager, int horizontalIdx, int verticalIdx) {

        super(analogControlDataManager, ControlPlacement.RIGHT, horizontalIdx, verticalIdx);
    }
}
