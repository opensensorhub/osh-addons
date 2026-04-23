/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.mavsdk.util;

public final class PlatformId {

    public record Platform(String os, String arch) {}

    public static Platform get() {
        String osStringLower = System.getProperty("os.name").toLowerCase();

        String os = osStringLower.contains("mac") ? "macOS"
                : osStringLower.contains("win") ? "Windows"
                : (osStringLower.contains("nux") || osStringLower.contains("nix")) ? "Linux"
                : "UnknownOS";

        String archStringLower = System.getProperty("os.arch").toLowerCase();

        String arch = archStringLower.contains("aarch64") || archStringLower.contains("arm64") ? "ARM64"
                : (archStringLower.contains("x86_64") || archStringLower.contains("amd64")) ? "x86_64"
                : "UnknownArch";

        return new Platform(os, arch);
    }
}
