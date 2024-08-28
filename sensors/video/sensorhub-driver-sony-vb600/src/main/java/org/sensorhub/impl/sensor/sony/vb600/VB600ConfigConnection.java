/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.sony.vb600;

import org.sensorhub.api.config.DisplayInfo;

/**
 * Connection settings for the Sony SNC VB600 camera exposed via the OpenSensorHub Admin panel.
 */
public class VB600ConfigConnection {
    /**
     * The IP address of the camera.
     */
    @DisplayInfo.Required
    @DisplayInfo(label = "IP Address", desc = "IP address of the camera.")
    public String ipAddress;



    /**
     * Username used to connect to the camera.
     */
    @DisplayInfo(label = "Username", desc = "Username for the camera.")
    public String username;

    /**
     * Password used to connect to the camera.
     */
    @DisplayInfo.FieldType(DisplayInfo.FieldType.Type.PASSWORD)
    @DisplayInfo(label = "Password", desc = "Password for the camera.")
    public String password;

    @DisplayInfo(label = "Has Username and Password", desc = "If the camera has a username and password to access")
    public boolean hasUsernameAndPassword;

    }

