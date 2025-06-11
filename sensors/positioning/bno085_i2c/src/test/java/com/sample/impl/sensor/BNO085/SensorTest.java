/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2025 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.sample.impl.sensor.BNO085;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SensorTest extends TestBase {
    @Test
    public void testSensor() {
        assertTrue(sensor.isStarted());
        assertTrue(sensor.isConnected());

        assertEquals(Bno085Sensor.UID_PREFIX + "123456789", sensor.getUniqueIdentifier());
        assertEquals("Sensor Template", sensor.getName());
        assertEquals("Description of the sensor", sensor.getDescription());
    }
}
