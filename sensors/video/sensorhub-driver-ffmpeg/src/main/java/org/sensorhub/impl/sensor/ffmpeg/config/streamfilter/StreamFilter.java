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

import org.sensorhub.mpegts.StreamCollection;
import org.sensorhub.mpegts.StreamContext;

import java.util.HashSet;
import java.util.Set;

public abstract class StreamFilter {
    public abstract boolean filter(StreamContext stream);

    public abstract void resetState();

    public Set<StreamContext> getFilteredStreams(StreamCollection streams) {
        resetState();

        Set<StreamContext> contexts = new HashSet<>();
        for (StreamContext context : streams.getStreamContexts()) {
            if (filter(context)) {
                contexts.add(context);
            }
        }
        return contexts;
    }
}
