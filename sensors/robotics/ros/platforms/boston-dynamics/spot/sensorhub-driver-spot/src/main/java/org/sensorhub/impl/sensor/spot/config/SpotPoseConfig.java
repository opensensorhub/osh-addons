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
 * Configuration settings for the Boston Dynamics SPOT driver pose exposed via the OpenSensorHub Admin panel.
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
public class SpotPoseConfig {

    @DisplayInfo.Required
    @DisplayInfo(label = "Body Pose Service", desc = "The static body pose changes the body position only when the robot is stationary")
    public String staticBodyPoseService = "/spot/posed_stand";

//    @DisplayInfo.Required
//    @DisplayInfo(label = "Body Pose Action", desc = "The body pose changes the body position through an action server")
//    public String bodyPoseAction = "/spot/body_pose";

    @DisplayInfo.Required
    @DisplayInfo(label = "In Motion or Idle Body Pose Topic", desc = "Move the robot by specifying a pose either while robot is stationary or in motion")
    public String inMotionOrIdleBodyPoseTopic = "/spot/in_motion_or_idle_body_pose";

//    @DisplayInfo.Required
//    @DisplayInfo(label = "Motion or Idle Body Pose Action", desc = "")
//    public String inMotionBodyPoseAction = "/spot/motion_or_idle_body_pose";

    @DisplayInfo.Required
    @DisplayInfo(label = "Go to Pose Topic", desc = "Move the robot by specifying a pose")
    public String goToPoseTopic = "/spot/go_to_pose";

//    @DisplayInfo.Required
//    @DisplayInfo(label = "Trajectory Action", desc = "Move the robot by specifying a pose, gives a little more control than the Go To Pose Topic, and will also give you information about success or failure")
//    public String trajectoryAction = "/spot/trajectory";

    @DisplayInfo.Required
    @DisplayInfo(label = "Enable", desc = "Enables the control streams to the platform")
    public boolean enabled = true;
}
