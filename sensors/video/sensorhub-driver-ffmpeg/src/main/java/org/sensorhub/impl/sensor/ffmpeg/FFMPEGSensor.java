/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.ffmpeg;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.ffmpeg.config.FFMPEGConfig;

/**
 * Sensor driver that can read video data that is compatible with FFMPEG.
 *
 *
 * @author Drew Botts
 * @since Feb. 2023
 */
public class FFMPEGSensor extends FFMPEGSensorBase<FFMPEGConfig> {
    @Override
    protected void doInit() throws SensorHubException {
        super.doInit();

        // We need the background thread here since we start reading the video data immediately in order to determine
        // the video size.
        setupExecutor();

        // Open up the stream so that we can get the video output.
        openStream();

    }
    
    @Override
    protected void doStart() throws SensorHubException {
    	super.doStart();

    	// Start up the background thread if it's not already going. Normally doInit() will have just been called, so
    	// this is redundant (but harmless). But if the user has stopped the sensor and re-started it, then this call
    	// is necessary.
        setupExecutor();

        // Make sure the stream is already open. (If the sensor has been previously started, then stopped, then the
        // stream won't be open.)
        openStream();

        // Some preliminary data was read from the stream in doInit(), but this call makes it start processing all the
        // frames.
        startStream();
    }
}
