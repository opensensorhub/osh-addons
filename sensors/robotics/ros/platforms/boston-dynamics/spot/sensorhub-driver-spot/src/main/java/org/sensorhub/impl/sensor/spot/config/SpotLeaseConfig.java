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
 * Configuration settings for the Boston Dynamics SPOT driver related to holding a lease
 * and maintaining control of the platform exposed via the OpenSensorHub Admin panel.
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
public class SpotLeaseConfig {

    @DisplayInfo.Required
    @DisplayInfo(label = "Lease Claim Service", desc = "A body lease gives the holder the ability to command the spot to make actions in the world")
    public String leaseClaimService = "/spot/claim";

    @DisplayInfo.Required
    @DisplayInfo(label = "Lease Release Service", desc = "Release the lease")
    public String leaseReleaseService = "/spot/release";

    @DisplayInfo.Required
    @DisplayInfo(label = "Enable", desc = "Enables the control streams to the platform")
    public boolean enabled = true;
}
