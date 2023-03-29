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
package org.sensorhub.impl.sensor.movebase;

import org.sensorhub.impl.ros.config.RosMasterConfig;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.SensorConfig;

/**
 * Configuration settings for the [NAME] driver exposed via the OpenSensorHub Admin panel.
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
 * @since Feb. 2, 2023
 */
public class MoveBaseConfig extends SensorConfig {

    @DisplayInfo.Required
    @DisplayInfo(label = "ROS", desc = "Configuration parameters for participating in ROS ecosystem")
    public RosConfig rosConfig = new RosConfig();

    public class RosConfig extends RosMasterConfig {

        @DisplayInfo.Required
        @DisplayInfo(label = "X-origin", desc = "The robot's initial position x-coordinate")
        public double xOrigin = 0.0;

        @DisplayInfo.Required
        @DisplayInfo(label = "Y-origin", desc = "The robot's initial position y-coordinate")
        public double yOrigin = 0.0;

        @DisplayInfo.Required
        @DisplayInfo(label = "Frame Id", desc = "Id of the reference frame for the pose messages published")
        public String frameId = "map";
    }
}
