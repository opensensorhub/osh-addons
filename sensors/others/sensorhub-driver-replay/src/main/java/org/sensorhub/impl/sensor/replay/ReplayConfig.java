/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.replay;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.SensorConfig;
import java.util.Date;

/**
 * Configuration parameters for replay
 * playback of archive data
 *
 * @author Nicolas Garay
 * @since 7/28/2025
 */
public class ReplayConfig extends SensorConfig {
    @DisplayInfo.Required
    @DisplayInfo.FieldType(DisplayInfo.FieldType.Type.SYSTEM_UID)
    @DisplayInfo(label = "System Unique Id", desc = "The unique id of the system for which archive data is to be transferred")
    public String uniqueId;

    @DisplayInfo(label = "Start Time (ISO UTC)", desc = "The timestamp of the earliest observations from the archived data set to transmit, starting transmission of observations\nSee the corresponding data store for when data is available")
    public Date startTime;

    @DisplayInfo(label = "End Time (ISO UTC)", desc = "The timestamp of the latest observations from the archived data set to transmit, ending transmission of observations\nSee the corresponding data store for when data is available")
    public Date endTime;

    @DisplayInfo(label = "Batch Sizing", desc = "For playback, tuning this output will enable smoother performance, however the higher the value the more memory usage")
    public int batchSize = 100;
}
