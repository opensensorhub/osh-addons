/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.uas.outputs;

import org.sensorhub.impl.sensor.uas.UasSensor;
import org.sensorhub.impl.sensor.uas.config.UasConfig;
import net.opengis.swe.v20.DataBlock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.event.IEventListener;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.api.data.DataEvent;
import org.vast.data.DataBlockMixed;

import java.io.File;
import java.net.URL;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class FullTelemetryTest {

    private UasSensor driver = null;

    private final Object syncObject = new Object();

    @Before
    public void init() throws Exception {

        URL resource = getClass().getClassLoader().getResource("sample-stream.ts");
        UasConfig config = new UasConfig();

        assert resource != null;
        config.connection.transportStreamPath = new File(resource.toURI()).getPath();
        config.outputs.enableFullTelemetry = true;

        driver = new UasSensor();
        driver.setConfiguration(config);
        driver.init();
    }

    @After
    public void cleanup() throws Exception {

        driver.stop();
    }

    @Test
    public void testFullTelemetry() throws Exception {

        driver.start();

        // register listener on data interface
        IStreamingDataInterface di = driver.getObservationOutputs().values().iterator().next();

        IEventListener listener = event -> {

            assertTrue(event instanceof DataEvent);
            DataEvent newDataEvent = (DataEvent) event;

            DataBlock timeStamp = ((DataBlockMixed) newDataEvent.getRecords()[0]).getUnderlyingObject()[0];

            assertTrue(timeStamp.getDoubleValue(0) > 0);

            DataBlock dataLinkVersion = ((DataBlockMixed) newDataEvent.getRecords()[0]).getUnderlyingObject()[1];
            assertNotNull(dataLinkVersion.getStringValue(0));

            DataBlock platformDesignation = ((DataBlockMixed) newDataEvent.getRecords()[0]).getUnderlyingObject()[2];
            assertNotNull(platformDesignation.getStringValue(0));

            DataBlock security = ((DataBlockMixed) newDataEvent.getRecords()[0]).getUnderlyingObject()[3];
            assertEquals("UNCLASSIFIED", security.getStringValue(0));
            assertEquals("1", security.getStringValue(1));
            assertEquals("//US", security.getStringValue(2));
            assertNull(security.getStringValue(3));
            assertEquals("//FOUO", security.getStringValue(4));
            assertEquals("US", security.getStringValue(5));
            assertNull(security.getStringValue(6));
            assertNull(security.getStringValue(7));
            assertNull(security.getStringValue(8));
            assertNull(security.getStringValue(9));
            assertNull(security.getStringValue(10));
            assertNull(security.getStringValue(11));
            assertNull(security.getStringValue(12));
            assertNull(security.getStringValue(13));
            assertEquals("5", security.getStringValue(14));
            assertNull(security.getStringValue(15));
            assertNull(security.getStringValue(16));

            DataBlock platformTailNumber = ((DataBlockMixed) newDataEvent.getRecords()[0]).getUnderlyingObject()[4];
            assertNotNull(platformTailNumber.getStringValue(0));

            DataBlock platformLocation = ((DataBlockMixed) newDataEvent.getRecords()[0]).getUnderlyingObject()[5];
            assertTrue(platformLocation.getDoubleValue(0) >= -90 && platformLocation.getDoubleValue(0) <= 90);
            assertTrue(platformLocation.getDoubleValue(1) >= -180 && platformLocation.getDoubleValue(1) <= 180);
            assertTrue(platformLocation.getDoubleValue(2) >= Double.MIN_VALUE && platformLocation.getDoubleValue(2) <= Double.MAX_VALUE);

            DataBlock imageFrame = ((DataBlockMixed) newDataEvent.getRecords()[0]).getUnderlyingObject()[6];
            assertTrue(imageFrame.getDoubleValue(0) > 0);
            assertTrue(imageFrame.getDoubleValue(1) > 0);
            assertTrue(imageFrame.getDoubleValue(2) >= -90 && imageFrame.getDoubleValue(2) <= 90);
            assertTrue(imageFrame.getDoubleValue(3) >= -180 && imageFrame.getDoubleValue(3) <= 180);
            assertTrue(imageFrame.getDoubleValue(4) > 0);
            assertTrue(imageFrame.getDoubleValue(5) >= -90 && imageFrame.getDoubleValue(5) <= 90);
            assertTrue(imageFrame.getDoubleValue(6) >= -180 && imageFrame.getDoubleValue(6) <= 180);
            assertTrue(imageFrame.getDoubleValue(7) >= -90 && imageFrame.getDoubleValue(7) <= 90);
            assertTrue(imageFrame.getDoubleValue(8) >= -180 && imageFrame.getDoubleValue(8) <= 180);
            assertTrue(imageFrame.getDoubleValue(9) >= -90 && imageFrame.getDoubleValue(9) <= 90);
            assertTrue(imageFrame.getDoubleValue(10) >= -180 && imageFrame.getDoubleValue(10) <= 180);
            assertTrue(imageFrame.getDoubleValue(11) >= -90 && imageFrame.getDoubleValue(11) <= 90);
            assertTrue(imageFrame.getDoubleValue(12) >= -180 && imageFrame.getDoubleValue(12) <= 180);

            DataBlock imageSourceSensor = ((DataBlockMixed) newDataEvent.getRecords()[0]).getUnderlyingObject()[7];
            assertNotNull(imageSourceSensor.getStringValue(0));

            DataBlock imageCoordinateSystem = ((DataBlockMixed) newDataEvent.getRecords()[0]).getUnderlyingObject()[8];
            assertNotNull(imageCoordinateSystem.getStringValue(0));

            DataBlock slantRange = ((DataBlockMixed) newDataEvent.getRecords()[0]).getUnderlyingObject()[9];
            assertTrue(slantRange.getDoubleValue(0) > 0);

            DataBlock attitude = ((DataBlockMixed) newDataEvent.getRecords()[0]).getUnderlyingObject()[10];
            assertTrue(attitude.getDoubleValue(0) >= 0 && attitude.getDoubleValue(0) <= 360);
            assertTrue(attitude.getDoubleValue(1) >= -90 && attitude.getDoubleValue(1) <= 90);
            assertTrue(attitude.getDoubleValue(2) >= -180 && attitude.getDoubleValue(2) <= 180);

            DataBlock platformHPR = ((DataBlockMixed) newDataEvent.getRecords()[0]).getUnderlyingObject()[11];
            assertTrue(platformHPR.getDoubleValue(0) >= 0 && platformHPR.getDoubleValue(0) <= 360);
            assertTrue(platformHPR.getDoubleValue(1) >= -90 && platformHPR.getDoubleValue(1) <= 90);
            assertTrue(platformHPR.getDoubleValue(2) >= -180 && platformHPR.getDoubleValue(2) <= 180);

            DataBlock platformGroundSpeed = ((DataBlockMixed) newDataEvent.getRecords()[0]).getUnderlyingObject()[12];
            assertTrue(platformGroundSpeed.getDoubleValue(0) > 0);

            synchronized (syncObject) {

                syncObject.notify();
            }
        };

        di.registerListener(listener);

        // start capture and wait until we receive the first frame
        synchronized (syncObject) {

            syncObject.wait();
        }

        di.unregisterListener(listener);
    }
}
