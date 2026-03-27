/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2025 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.mavsdk;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.impl.sensor.ffmpeg.config.Connection;
import org.sensorhub.impl.sensor.mavsdk.config.MissionConfig;
import org.sensorhub.impl.sensor.mavsdk.util.PlatformId;

import java.util.List;

/**
 * Configuration settings for the {@link UnmannedSystem} driver exposed via the OpenSensorHub Admin panel.
 * <p>
 * Public fields are exposed in the Admin panel for configuration by the user.
 * These fields can be annotated with the DisplayInfo annotation to provide additional information to the user
 * or to restrict the values that can be entered.
 * <p>
 * Configuration takes the form of:
 * <pre>{@code
 * @DisplayInfo(label = "Field Label", desc = "A description of the field to be shown in the UI.")
 * public Type configOption = "default value";
 * }</pre>
 */
public class UnmannedConfig extends SensorConfig {

    public UnmannedConfig() {

        //macOS
        SerialPort = "serial:///dev/tty.usbserial-0001:57600";

        PlatformId.Platform platform = PlatformId.get();
        if ( platform.os().equals("Windows")) {
            SerialPort = "serial://COM3:57600";
        }
    }

    // Define the Enum for your options
    public enum ConnectionType {

        WIFI,
        SERIAL,
        SIM,
        EMPTY
    };

// register with your driver
    @DisplayInfo(desc = "Type of Connection to the Drone")
    public ConnectionType connectionType = ConnectionType.WIFI;

    /**
     * The unique identifier for the configured sensor (or sensor platform).
     */
    @DisplayInfo.Required
    @DisplayInfo(desc = "Serial number or unique identifier")
    public String serialNumber = "sensor001";


    @DisplayInfo(label = "Video Connection", desc = "Configuration for video streaming")
    public Connection ffmpegConnection = new Connection();

    public List<MissionConfig> missions;

    public String SDKAddress = "127.0.0.1";

    public int SDKPort = 50051;

    public String SerialPort = "";

    public String UDPListenAddr = "0.0.0.0";

    public int UDPListenPort = 14550;

    public String systemId;

}
