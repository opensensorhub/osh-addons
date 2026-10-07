/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research, LLC. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.ffmpeg.config.streamfilter;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.mpegts.StreamContext;

public class MaxCountFilter extends StreamFilter {
    @DisplayInfo.Required
    @DisplayInfo(label = "Maximum Video Streams", desc = "Maximum number of video streams to output.")
    public int maxVideoStreams = 1;

    @DisplayInfo.Required
    @DisplayInfo(label = "Maximum Audio Streams", desc = "Maximum number of audio streams to output.")
    public int maxAudioStreams = 1;

    @DisplayInfo.Required
    @DisplayInfo(label = "Maximum Data Streams", desc = "Maximum number of data streams to output.")
    public int maxDataStreams = 0;

    private int videoStreams = 0, audioStreams = 0, dataStreams = 0;

    @Override
    public boolean filter(StreamContext stream) {
        return switch (stream.getStreamType()) {
            case VIDEO -> videoStreams++ < maxVideoStreams;
            case AUDIO -> audioStreams++ < maxAudioStreams;
            case DATA -> dataStreams++ < maxDataStreams;
            default -> false;
        };
    }

    @Override
    public void resetState() {
        videoStreams = 0;
        audioStreams = 0;
        dataStreams = 0;
    }
}
