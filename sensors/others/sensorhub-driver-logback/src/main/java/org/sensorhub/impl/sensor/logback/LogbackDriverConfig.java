/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research, LLC. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.logback;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.SensorConfig;

import java.util.UUID;

public class LogbackDriverConfig extends SensorConfig {

    public enum LogbackLevel {
        TRACE,
        DEBUG,
        INFO,
        WARN,
        ERROR,
    }

    @DisplayInfo(label = "Logback level", desc = "Level at which logs are captured. " +
            "Priority: (TRACE -> DEBUG -> INFO -> WARN -> ERROR)")
    public LogbackLevel level;

    @DisplayInfo(label = "Unique Identifier", desc = "Unique identifier to identify logs from this node")
    public String uniqueId;

    public LogbackDriverConfig() {
        this.uniqueId = UUID.randomUUID().toString();
        this.level = LogbackLevel.INFO;
    }

}
