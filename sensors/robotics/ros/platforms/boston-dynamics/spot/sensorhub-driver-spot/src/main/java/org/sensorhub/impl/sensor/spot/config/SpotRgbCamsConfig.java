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
 * * Configuration settings for the Boston Dynamics SPOT driver rgb cameras exposed via the OpenSensorHub Admin panel.
 *  * <p>
 *  * Configuration settings take the form of
 *  * <code>
 *  * DisplayInfo(desc="Description of configuration field to show in UI")
 *  * public Type configOption;
 *  * </code>
 *  * <p>
 *  * Containing an annotation describing the setting and if applicable its range of values
 *  * as well as a public access variable of the given Type
 *
 * @author Nick Garay
 * @since Sept. 25, 2023
 */
public class SpotRgbCamsConfig {

    @DisplayInfo.Required
    @DisplayInfo(label = "Sensor Resolution", desc = "Specify the resolution of the sensor frame")
    public SpotFrameResConfig frameConfig = new SpotFrameResConfig();

    @DisplayInfo.Required
    @DisplayInfo(label = "Front Left Camera", desc = "Camera sensor feed")
    public SpotFrameConfig frontLeftCamera = new SpotFrameConfig("/spot/camera/frontleft/image");

    @DisplayInfo.Required
    @DisplayInfo(label = "Front Right Camera", desc = "Camera sensor feed")
    public SpotFrameConfig frontRightCamera = new SpotFrameConfig("/spot/camera/frontright/image");

    @DisplayInfo.Required
    @DisplayInfo(label = "Left Camera", desc = "Camera sensor feed")
    public SpotFrameConfig leftCamera = new SpotFrameConfig("/spot/camera/left/image");

    @DisplayInfo.Required
    @DisplayInfo(label = "Right Camera", desc = "Camera sensor feed")
    public SpotFrameConfig rightCamera = new SpotFrameConfig("/spot/camera/right/image");

    @DisplayInfo.Required
    @DisplayInfo(label = "Back Camera", desc = "Camera sensor feed")
    public SpotFrameConfig backCamera = new SpotFrameConfig("/spot/camera/back/image");
}
