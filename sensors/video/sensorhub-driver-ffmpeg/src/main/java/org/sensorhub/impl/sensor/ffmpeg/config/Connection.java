/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.ffmpeg.config;


import org.sensorhub.api.config.DisplayInfo;

/**
 * Configuration settings for the FFMPEG driver exposed via the OpenSensorHub Admin panel.
 * Specifically, establish connections for video streams that are compatible with FFMPEG
 *
 * @author Drew Botts
 * @since Feb 2023
 */
public class Connection {

    @DisplayInfo(label = "File Path", desc = "VIDEO file to be streamed")
    @DisplayInfo.FieldType(value = DisplayInfo.FieldType.Type.FILESYSTEM_PATH)
    public String transportStreamPath;
    
    @DisplayInfo(label = "FPS", desc = "Number of frames per second to enforce during playback of a file."
        + " 0 means the stream will be played as fast as possible.")
    public int fps = 0;
    
    @DisplayInfo(desc = "Continuously loop video playback (only available when reading from file).")
    public boolean loop = false;

    @DisplayInfo(label = "Connection String", desc = "Connection string that the driver will pass to ffmpeg to connect to the MISB STANAG 4609 MPEG-TS stream. This value is ignored if an input file path is also set in the configuration. See https://www.ffmpeg.org/ffmpeg-protocols.html#Protocols for details of allowed values.")
    public String connectionString;

    @DisplayInfo(label = "MJPEG", desc = "Select if video codec format is MJPEG. Otherwise driver will use H264.")
    public boolean isMJPEG = false;

    @DisplayInfo(label = "Ignore Data Timestamps", desc = "This ignores any data timestamps and defaults to current system time. This is necessary if video stream does not contain any timestamps")
    public boolean ignoreDataTimestamps = false;
}
