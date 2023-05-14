/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.uas.config;

import org.sensorhub.api.config.DisplayInfo;

/**
 * Configuration settings for the UAS driver exposed via the OpenSensorHub Admin panel.
 * Specifically, establish connections for source of MISB STANAG 4609 MPEG-TS data
 *
 * @author Nick Garay
 * @since Oct. 6, 2020
 */
public class Connection {

    @DisplayInfo(label = "File Path", desc = "MISB STANAG 4609 MPEG-TS file to be streamed")
    @DisplayInfo.FieldType(value = DisplayInfo.FieldType.Type.FILESYSTEM_PATH)
    public String transportStreamPath;
    
    @DisplayInfo(label = "FPS", desc = "Number of frames per second to enforce during playback of a file."
        + " 0 means the stream will be played as fast as possible.")
    public int fps = 0;
    
    @DisplayInfo(desc = "Continuously loop video playback (only available when reading from file)."
        + "In this mode, timestamps are adjusted to simulate a real-time stream")
    public boolean loop = false;

    @DisplayInfo(label = "Connection String", desc = "Connection string that the driver will pass to ffmpeg to connect to the MISB STANAG 4609 MPEG-TS stream. This value is ignored if an input file path is also set in the configuration. See https://www.ffmpeg.org/ffmpeg-protocols.html#Protocols for details of allowed values.")
    public String connectionString;
}
