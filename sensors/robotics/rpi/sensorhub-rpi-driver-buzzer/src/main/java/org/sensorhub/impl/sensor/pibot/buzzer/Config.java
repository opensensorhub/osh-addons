/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2020-2024 Nicolas Garay. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.pibot.buzzer;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.SensorConfig;

/**
 * Configuration settings for the [NAME] driver exposed via the OpenSensorHub Admin panel.
 *
 * Configuration settings take the form of
 * <code>
 *     DisplayInfo(desc="Description of configuration field to show in UI")
 *     public Type configOption;
 * </code>
 *
 * Containing an annotation describing the setting and if applicable its range of values
 * as well as a public access variable of the given Type
 *
 * @author Nick Garay
 * @since Jan. 24, 2021
 */
public class Config extends SensorConfig {

    /**
     * The unique identifier for the configured UAS sensor platform.
     */
    @DisplayInfo.Required
    @DisplayInfo(desc="Serial number or unique identifier for UAS sensor platform")
    public String serialNumber = "uas001";

    @DisplayInfo.Required
    @DisplayInfo(desc="MISB STANAG 4609 MPEG-TS data to be streamed, can be a path to a file or an ipAddress:port")
    public String transportStreamPath;
}
