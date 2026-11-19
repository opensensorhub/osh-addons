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
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.impl.ros.config.RosMasterConfig;

/**
 * Configuration settings for the Boston Dynamics SPOT driver exposed via the OpenSensorHub Admin panel.
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
public class SpotConfig extends SensorConfig {

    /**
     * The unique identifier for the configured sensor (or sensor platform).
     */
    @DisplayInfo.Required
    @DisplayInfo(desc = "Serial number or unique identifier")
    public String serialNumber = "spot-serial-num";

    @DisplayInfo.Required
    @DisplayInfo(label = "ROS", desc = "Configuration parameters for participating in ROS ecosystem")
    public SpotRosMasterConfig rosMaster = new SpotRosMasterConfig();

    @DisplayInfo.Required
    @DisplayInfo(label = "Allow Motion Controls", desc = "Configuration parameters SPOT services, topics, and actions related to commands allowing for motion")
    public SpotAllowMotionConfig spotAllowMotionConfig = new SpotAllowMotionConfig();

    @DisplayInfo.Required
    @DisplayInfo(label = "Cancel Motion Controls", desc = "Configuration parameters SPOT services, topics, and actions related to commanding canceling commands for motion")
    public SpotCancelMotionConfig spotCancelMotionConfig = new SpotCancelMotionConfig();

    @DisplayInfo(label = "Depth Camera Outputs", desc = "Configuration parameters SPOT Depth Camera topics")
    public SpotDepthCamsConfig spotDepthCamsConfig = new SpotDepthCamsConfig();

    @DisplayInfo.Required
    @DisplayInfo(label = "Emergency Stop Controls", desc = "Configuration parameters SPOT services, topics, and actions related to emergency stops control")
    public SpotEStopConfig spotEStopConfig = new SpotEStopConfig();

    @DisplayInfo.Required
    @DisplayInfo(label = "Lease Controls", desc = "Configuration parameters SPOT lease services, topics, and actions related to requesting control")
    public SpotLeaseConfig spotLeaseConfig = new SpotLeaseConfig();

    @DisplayInfo.Required
    @DisplayInfo(label = "Motion Controls", desc = "Configuration parameters SPOT motion services, topics, and actions")
    public SpotMotionConfig spotMotionConfig = new SpotMotionConfig();

    @DisplayInfo.Required
    @DisplayInfo(label = "Body Pose Controls", desc = "Configuration parameters SPOT pose services, topics, and actions")
    public SpotPoseConfig spotPoseConfig = new SpotPoseConfig();

    @DisplayInfo.Required
    @DisplayInfo(label = "Spot Power Controls", desc = "Configuration parameters SPOT services, topics, and actions related to commanding power to motors")
    public SpotPowerConfig spotPowerConfig = new SpotPowerConfig();

    @DisplayInfo.Required
    @DisplayInfo(label = "Camera Outputs", desc = "Configuration parameters SPOT Camera topics")
    public SpotRgbCamsConfig spotRgbCamsConfig = new SpotRgbCamsConfig();

    @DisplayInfo.Required
    @DisplayInfo(label = "Sit Stand Controls", desc = "Configuration parameters SPOT services, topics, and actions related to commanding sit and stand")
    public SpotSitStandConfig spotSitStandConfig = new SpotSitStandConfig();

    @DisplayInfo.Required
    @DisplayInfo(label = "Status Outputs", desc = "Configuration parameters SPOT Status topics")
    public SpotStatusConfig spotStatusConfig = new SpotStatusConfig();

    public static class SpotRosMasterConfig extends RosMasterConfig {

        @DisplayInfo.Required
        @DisplayInfo(label = "Local ROS Master", desc = "Spin up a local ROS Master process for isolated testing and registration")
        public boolean spinRosMaster = false;

        @DisplayInfo.Required
        @DisplayInfo(label = "ROS Master Port", desc = "Port on which the ROS Master listens for registration of components, " +
                "ensure this port matches the port used in Master URI")
        public int rosCorePort = 11311;
    }
}
