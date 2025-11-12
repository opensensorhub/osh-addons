/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2025 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.sample.impl.sensor.meshtastic;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MeshtasticSensorTest extends TestBase {
    @Test
    public void testSensor() {
        assertTrue(meshtasticSensor.isStarted());
        assertTrue(meshtasticSensor.isConnected());

        assertEquals(MeshtasticSensor.UID_PREFIX + "123456789", meshtasticSensor.getUniqueIdentifier());
        assertEquals("Sensor Template", meshtasticSensor.getName());
        assertEquals("Description of the sensor", meshtasticSensor.getDescription());
    }
}
