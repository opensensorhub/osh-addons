/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 - 2024 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.spot.config;

import org.sensorhub.api.config.DisplayInfo;

/**
 * Configuration settings for the Boston Dynamics SPOT driver related to
 * emergency stoppage of the platform exposed via the OpenSensorHub Admin panel.
 *
 * <p>
 * Configuration settings take the form of
 * <code>
 * DisplayInfo(desc="Description of configuration field to show in UI")
 * public Type configOption;
 * </code>
 * <p>
 * Containing an annotation describing the setting and if applicable its range of values
 * as well as a public access variable of the given Type
 *
 * @author Nick Garay
 * @since Sept. 25, 2023
 */
public class SpotEStopConfig {

    @DisplayInfo.Required
    @DisplayInfo(label = "Gentle eStop Service", desc = "Stops all motion of the robot and commands it to sit. This stop does not have to be released")
    public String eStopGentleService = "/spot/estop/gentle";

    @DisplayInfo.Required
    @DisplayInfo(label = "Hard eStop Service", desc = "The hard emergency stop will kill power to the motors and must be released before you can send any commands to the robot. Requires call to eStop Release to allow further control")
    public String eStopHardService = "/spot/estop/hard";

    @DisplayInfo.Required
    @DisplayInfo(label = "eStop Release Service", desc = "Allows further control after a hard e-stop")
    public String eStopReleaseService = "/spot/estop/release";

    @DisplayInfo.Required
    @DisplayInfo(label = "Enable", desc = "Enables the control streams to the platform")
    public boolean enabled = true;
}
