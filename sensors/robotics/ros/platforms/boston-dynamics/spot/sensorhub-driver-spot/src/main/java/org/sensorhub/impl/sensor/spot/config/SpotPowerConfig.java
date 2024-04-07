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
 * power control of the platform exposed via the OpenSensorHub Admin panel.
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
public class SpotPowerConfig {

    @DisplayInfo.Required
    @DisplayInfo(label = "Motor Power Service", desc = "Enables motor power. Needs to be enabled once you have a Lease on the body")
    public String powerOnService = "/spot/power_on";

    @DisplayInfo.Required
    @DisplayInfo(label = "Motor Power Service", desc = "Disables motor power. Needs to be disabled power to motors and shut down")
    public String powerOffService = "/spot/power_off";

    @DisplayInfo.Required
    @DisplayInfo(label = "Enable", desc = "Enables the control streams to the platform")
    public boolean enabled = true;
}
