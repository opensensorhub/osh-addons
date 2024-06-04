/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/
package com.sample.impl.sensor.universalcontroller;

import com.alexalmanza.models.ControllerType;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.SensorConfig;

import java.util.ArrayList;

/**
 * Configuration settings for the {@link UniversalControllerSensor} driver exposed via the OpenSensorHub Admin panel.
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
public class UniversalControllerConfig extends SensorConfig {

    /**
     * The unique identifier for the configured sensor (or sensor platform).
     */
    @DisplayInfo.Required
    @DisplayInfo(desc = "Serial number or unique identifier")
    public String serialNumber = "universalcontroller";

    /**
     * The index of the primary controller.
     */
    @DisplayInfo.Required
    @DisplayInfo(desc = "Index of primary controller")
    public int primaryControllerIndex = 0;

    @DisplayInfo.Required
    @DisplayInfo(desc = "Primary controller switching configuration")
    public ControllerLayerConfig controllerLayerConfig = new ControllerLayerConfig();

    @DisplayInfo.Required
    @DisplayInfo(desc = "Number of control streams for controller processes", label = "Number of Control Streams")
    public int numControlStreams = 0;

    @DisplayInfo.Required
    @DisplayInfo(desc = "Index of primary control stream")
    public int primaryControlStreamIndex = 0;

    @DisplayInfo.Required
    @DisplayInfo(desc = "Polling rate in milliseconds")
    public int pollingRate = 250;

    @DisplayInfo.Required
    @DisplayInfo(desc = "Controllers to search for")
    public ArrayList<ControllerType> controllerTypes = new ArrayList<>();

    @DisplayInfo.Required
    @DisplayInfo(desc = "Time in seconds to search for controllers")
    public int controllerSearchTime = 15;
}
