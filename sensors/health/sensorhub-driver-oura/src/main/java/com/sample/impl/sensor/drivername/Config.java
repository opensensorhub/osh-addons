/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/
package com.sample.impl.sensor.drivername;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.SensorConfig;

import java.util.Date;

/**
 * Configuration settings for the {@link Sensor} driver exposed via the OpenSensorHub Admin panel.
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
 * @author your_name
 * @since date
 */
public class Config extends SensorConfig {

    /**
     * The unique identifier for the configured sensor (or sensor platform).
     */
//    @DisplayInfo.Required
//    @DisplayInfo(desc = "Serial number or unique identifier")
//    public String serialNumber = "sensor001";

//    @DisplayInfo.Required
//    @DisplayInfo(desc = "User Oura Cloud Token")
//    public String bearerToken;

//    Mike: KOZ6PJWLHIFJLUHXYYT5FW5Z7D62N7VQ
//    Lee:  KEAZBXNBUZUMICTHQCAYK6T7FT6FOTYI

    @DisplayInfo.Required
    @DisplayInfo(desc="Minimum time stamp of requested data")
    public Date startTime;

    @DisplayInfo.Required
    @DisplayInfo(desc="Maximum time stamp of requested data")
    public Date endTime;
}