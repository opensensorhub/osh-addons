/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.rtmp.config;

public enum HostType {
    UNSPECIFIED("0.0.0.0"),
    LOCALHOST("localhost"),
    DOCKER_INTERNAL("host.docker.internal")/*,
    OVERRIDE("")*/;

    public final String host;

    HostType(String host) {
        this.host = host;
    }
}
