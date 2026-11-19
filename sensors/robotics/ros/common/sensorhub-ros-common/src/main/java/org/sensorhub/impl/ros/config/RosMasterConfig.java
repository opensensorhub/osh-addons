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

package org.sensorhub.impl.ros.config;

import org.sensorhub.api.config.DisplayInfo;

/**
 * Base configuration parameters for ROS integration with OpenSensorHub modules
 *
 * @author Nicolas Garay
 * @since Mar 20, 2023
 */
public class RosMasterConfig {

    /**
     * The URI of the ROS Master
     */
    @DisplayInfo.Required
    @DisplayInfo(label = "Master URI", desc = "The URI of the ROS Master")
    public String uri = "http://127.0.0.1:11311";

    /**
     * The IP of the ROS host
     */
    @DisplayInfo.Required
    @DisplayInfo(label = "Local Host Address", desc = "The IP address of the Local host")
    public String localHostIp = "127.0.0.1";
}
