/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.rtmp;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks RTMP listener ports currently reserved by RTMP driver modules.
 * <p>
 * The singleton prevents multiple RTMP driver instances from attempting to listen
 * on the same port at the same time. All public methods are synchronized to
 * provide simple thread-safe access to the port reservation map.
 * </p>
 */
public final class RtmpPortSingleton {
    private static final RtmpPortSingleton instance = new RtmpPortSingleton();

    private final Map<Integer, String> urls = new HashMap<>();

    public static RtmpPortSingleton getInstance() {
        return instance;
    }

    /**
     * Attempts to reserve a port for the specified module.
     * <p>
     * If the port is not already reserved, this method records the module unique
     * identifier and returns {@code null}. If the port is already reserved, this
     * method returns the unique identifier of the module that currently owns it.
     * </p>
     *
     * @param url RTMP listener port to reserve
     * @param moduleUid unique identifier of the module requesting the port
     * @return {@code null} if the reservation succeeded; otherwise the unique
     * identifier of the module currently using the port
     */
    public synchronized String addConnection(int url, String moduleUid) {
        if (instance.urls.containsKey(url)) {
            return urls.get(url);
        } else {
            instance.urls.put(url, moduleUid);
            return null;
        }
    }

    /**
     * Releases a previously reserved RTMP listener port.
     *
     * @param url RTMP listener port to release
     */
    public synchronized void removeConnection(int url) {
        instance.urls.remove(url);
    }
}
