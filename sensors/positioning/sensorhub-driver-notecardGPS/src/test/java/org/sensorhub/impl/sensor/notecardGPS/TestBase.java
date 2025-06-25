/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2025 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.BNO085;

import org.sensorhub.impl.sensor.BNO085.config.Bno085Config;
import org.junit.After;
import org.junit.Before;

/**
 * Base class for unit tests which initializes the sensor before each test and cleans up after.
 */
public class TestBase {
    notecardGPSSensor sensor;
//    BNO085Output output;

    @Before
    public void init() throws Exception {
        notecardGPSConfig config = new notecardGPSConfig();
        config.serialNumber = "123456789";
        config.name = "Sensor Template";
        config.description = "Description of the sensor";
        sensor = new notecardGPSSensor();
        sensor.init(config);
        sensor.start();
//        output = sensor.output;
    }

    @After
    public void cleanup() throws Exception {
        if (null != sensor) {
            sensor.stop();
        }
    }
}
