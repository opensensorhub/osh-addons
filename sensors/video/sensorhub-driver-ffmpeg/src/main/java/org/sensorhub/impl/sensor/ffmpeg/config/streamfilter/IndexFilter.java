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

import java.util.HashSet;
import java.util.Set;

public class IndexFilter extends StreamFilter {

    @DisplayInfo(label = "Stream Indices", desc = "List of stream indices to include. Zero-indexed. Comma separated values. Specify a range using two indices separated with a \"-\".")
    public String indices = "";

    private Set<Integer> indicesSet = new HashSet<>();

    @Override
    public boolean filter(StreamContext stream) {
        return indicesSet.contains(stream.getStreamId());
    }

    @Override
    public void resetState() {
        indicesSet = new HashSet<>();

        if (indices != null && !indices.isEmpty()) {
            indicesSet = parseIndices(indices);
        }
    }

    private static Set<Integer> parseIndices(String indices) {
        var indexArray = indices.split(",");
        Set<Integer> indexSet = new HashSet<>();

        for (String value : indexArray) {
            var range = value.split("-");
            if (range.length == 1) {    // Single index
                indexSet.add(Integer.parseInt(range[0]));
            } else {                    // Range of indices separated by a dash
                for (int i = Integer.parseInt(range[0]); i <= Integer.parseInt(range[1]); i++) {
                    indexSet.add(i);
                }
            }
        }

        return indexSet;
    }
}
