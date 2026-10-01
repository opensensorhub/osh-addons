/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.ffmpeg.config;

import org.sensorhub.api.config.DisplayInfo;

/**
 * Configuration settings for the FFmpeg driver exposed via the OpenSensorHub Admin panel.
 * Specifically, establish connections for video streams that are compatible with FFMPEG
 */
public class Connection {
    /**
     * Connection string that the driver will pass to FFmpeg to connect to the stream.
     */
    @DisplayInfo.Required
    @DisplayInfo(label = "Connection String", desc = "Connection string that the driver will pass to FFmpeg to connect to the stream. See https://www.ffmpeg.org/ffmpeg-protocols.html#Protocols for details of allowed values. May also be a file path.")
    public String connectionString;

    @DisplayInfo(label = "Command Line Arguments", desc = "Optional command line arguments")
    public String commandLineArgs = "-timeout 3000000";

    /**
     * FPS of the video playback, used only when reading from a file.
     */
    @DisplayInfo.ValueRange
    @DisplayInfo(label = "FPS", desc = "Number of frames per second to enforce during playback of a file. 0 means the stream will be played as fast as possible. Only used when reading from file.")
    public int fps;

    /**
     * Continuously loop video playback, used only when reading from a file.
     */
    @DisplayInfo(desc = "Continuously loop video playback. Only used when reading from file.")
    public boolean loop = false;

    @DisplayInfo(label = "Inject Extradata for Streaming", desc = "Injects extradata into the video stream. Set true if this driver is being used to output a live video stream for late-join decoders.")
    public boolean injectExtradata = true;

    @DisplayInfo(label = "Register Devices", desc = "Registers devices for the video stream. Set true if input is not from a network stream or file (e.g. dshow).")
    public boolean registerDevices = false;

    /**
     * Max number of video streams to output.
     */
    @DisplayInfo(label = "Max Video Streams", desc = "Maximum number of video streams to output. Set 0 to disable video output. Set negative to output all available video streams.")
    public int maxVideoStreams = 1;

    /**
     * Max number of audio streams to output.
     */
    @DisplayInfo(label = "Max Audio Streams", desc = "Maximum number of audio streams to output. Set 0 to disable audio output. Set negative to output all available audio streams.")
    public int maxAudioStreams = 1;

    /**
     * Max number of binary data streams to output.
     */
    @DisplayInfo(label = "Max Binary Data Streams", desc = "Maximum number of binary data streams to output. Set 0 to disable binary data output. Set negative to output all available binary data streams.")
    public int maxBinaryDataStreams = 0;


}
