/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2025 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.mavsdk.outputs;

import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.impl.sensor.ffmpeg.FFMPEGSensor;
import org.sensorhub.impl.sensor.ffmpeg.config.Connection;
import org.sensorhub.impl.sensor.ffmpeg.config.FFMPEGConfig;

import java.util.ArrayList;

public class UnmannedVideoOutput extends FFMPEGSensor
{
    public UnmannedVideoOutput(Connection config ) {
        super();
        this.config = new FFMPEGConfig();
        this.config.connection = config;
    }

    public IStreamingDataInterface getVideoDataInterface() {
        return this.videoOutput;
    }

    public IStreamingDataInterface getAudioDataInterface() {
        return this.audioOutput;
    }
}
