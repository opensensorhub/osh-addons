/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.rtmpcam.config;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.impl.sensor.rtmpcam.connection.RtmpConnectionContext;

public class ConnectionConfig {

    /*
    @DisplayInfo.Required
    @DisplayInfo(label = "Generate Random Stream Key", desc = "Enable to generate and append a random hex string to the path. " +
            "Recommended for security. Only enable on first init, otherwise path will include multiple keys. ")
    public boolean generateRandomStreamKey = true;

     */

    @DisplayInfo(label = "Username")
    public String username = "";

    @DisplayInfo(label = "Password")
    public String password = "";

    @DisplayInfo.Required
    @DisplayInfo(label = "Port", desc = "Port listening for an RTMP connection request.")
    @DisplayInfo.ValueRange(min = 1, max = 65535)
    public int port = 1935;

    @DisplayInfo(label = "Path")
    public String path = "";

    @DisplayInfo(label = "Stream Key", desc = "(Optional) Stream key to use for the RTMP connection.")
    public String streamKey = "";

    @DisplayInfo(label = "Generate Random Stream Key", desc = "Overwrite the stream key field with a random string of characters.")
    public boolean generateRandomKey = false;

    /** Key for this config's exact fields — used as the map key on registration. */
    public String compositeKey() {
        return compositeKey(username, password, port, path, streamKey);
    }

    /**
     * Static form used by the router to generate wildcard candidate keys
     * (null fields become empty strings, producing a distinct key per specificity level).
     *
     * Format: "username:password:port:path:streamKey"
     * Example: "alice:secret:1935:live:cam1"
     * Catch-all: "::1935::"
     */
    public static String compositeKey(String username, String password, int port, String path, String streamKey) {
        return  (username != null ? username : "") + ":" +
                (password != null ? password : "") + ":" +
                port + ":" +
                (path != null ? path : "") + ":" +
                (streamKey != null ? streamKey : "");
    }

    public static String compositeKey(RtmpConnectionContext ctx) { return compositeKey(ctx.username(), ctx.password(), ctx.port(), ctx.path(), ctx.streamKey()); }
}