/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.ffmpeg.outputs;

import org.sensorhub.impl.sensor.ffmpeg.FFMPEGSensor;
import org.sensorhub.impl.sensor.ffmpeg.config.FFMPEGConfig;
import net.opengis.swe.v20.DataBlock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.event.IEventListener;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.api.data.DataEvent;
import org.vast.data.DataBlockMixed;
import org.vast.swe.SWEHelper;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.*;

public class FullTelemetryTest {

    private FFMPEGSensor driver = null;

    private final Object syncObject = new Object();

    @Before
    public void init() throws Exception {

        URL resource = FFMPEGSensor.class.getResource("sample-stream.ts");
        FFMPEGConfig config = new FFMPEGConfig();

        assert resource != null;
        config.connection.transportStreamPath = new File(resource.toURI()).getPath();
        config.outputs.enableFullTelemetry = true;

        driver = new FFMPEGSensor();
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
        var dataWriter = SWEHelper.createDataWriter(di.getRecommendedEncoding());
        dataWriter.setDataComponents(di.getRecordDescription().copy());
        dataWriter.setOutput(System.out);

        IEventListener listener = event -> {

            assertTrue(event instanceof DataEvent);
            DataEvent newDataEvent = (DataEvent) event;

            DataBlock dataBlock = ((DataBlockMixed) newDataEvent.getRecords()[0]);
            try {
                dataWriter.write(dataBlock);
                dataWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
                fail("Error writing data");
            }

            assertTrue(dataBlock.getDoubleValue(0) > 0);

            DataBlock dataLinkVersion = ((DataBlockMixed) newDataEvent.getRecords()[0]).getUnderlyingObject()[1];
            assertNotNull(dataLinkVersion.getStringValue(0));

            DataBlock platformDesignation = ((DataBlockMixed) newDataEvent.getRecords()[0]).getUnderlyingObject()[2];
            assertNotNull(platformDesignation.getStringValue(0));

            DataBlock security = ((DataBlockMixed) newDataEvent.getRecords()[0]).getUnderlyingObject()[3];
            assertEquals("UNCLASSIFIED", security.getStringValue(0));
            assertEquals("1", security.getStringValue(1));
            assertEquals("//US", security.getStringValue(2));

            DataBlock platformTailNumber = ((DataBlockMixed) newDataEvent.getRecords()[0]).getUnderlyingObject()[4];
            //assertNotNull(platformTailNumber.getStringValue(0));

            DataBlock platformLocation = ((DataBlockMixed) newDataEvent.getRecords()[0]).getUnderlyingObject()[5];
            assertTrue(platformLocation.getDoubleValue(0) >= -90 && platformLocation.getDoubleValue(0) <= 90);
            assertTrue(platformLocation.getDoubleValue(1) >= -180 && platformLocation.getDoubleValue(1) <= 180);
            assertTrue(platformLocation.getDoubleValue(2) >= Double.MIN_VALUE && platformLocation.getDoubleValue(2) <= Double.MAX_VALUE);

            DataBlock sensorParams = ((DataBlockMixed) newDataEvent.getRecords()[0]).getUnderlyingObject()[6];
            assertTrue(sensorParams.getDoubleValue(0) > 0);
            assertTrue(sensorParams.getDoubleValue(1) > 0);
            
            DataBlock imageFrame = ((DataBlockMixed) newDataEvent.getRecords()[0]).getUnderlyingObject()[7];
            assertTrue(imageFrame.getDoubleValue(0) >= -90 && imageFrame.getDoubleValue(0) <= 90);
            assertTrue(imageFrame.getDoubleValue(1) >= -180 && imageFrame.getDoubleValue(1) <= 180);
            assertTrue(imageFrame.getDoubleValue(2) > 0);
            assertTrue(imageFrame.getDoubleValue(3) >= -90 && imageFrame.getDoubleValue(3) <= 90);
            assertTrue(imageFrame.getDoubleValue(4) >= -180 && imageFrame.getDoubleValue(4) <= 180);
            assertTrue(imageFrame.getDoubleValue(5) >= -90 && imageFrame.getDoubleValue(5) <= 90);
            assertTrue(imageFrame.getDoubleValue(6) >= -180 && imageFrame.getDoubleValue(6) <= 180);
            assertTrue(imageFrame.getDoubleValue(7) >= -90 && imageFrame.getDoubleValue(7) <= 90);
            assertTrue(imageFrame.getDoubleValue(8) >= -180 && imageFrame.getDoubleValue(8) <= 180);
            assertTrue(imageFrame.getDoubleValue(9) >= -90 && imageFrame.getDoubleValue(9) <= 90);
            assertTrue(imageFrame.getDoubleValue(10) >= -180 && imageFrame.getDoubleValue(10) <= 180);

            DataBlock imageSourceSensor = ((DataBlockMixed) newDataEvent.getRecords()[0]).getUnderlyingObject()[8];
            assertNotNull(imageSourceSensor.getStringValue(0));

            DataBlock imageCoordinateSystem = ((DataBlockMixed) newDataEvent.getRecords()[0]).getUnderlyingObject()[9];
            assertNotNull(imageCoordinateSystem.getStringValue(0));

            DataBlock slantRange = ((DataBlockMixed) newDataEvent.getRecords()[0]).getUnderlyingObject()[10];
            assertTrue(slantRange.getDoubleValue(0) > 0);

            DataBlock attitude = ((DataBlockMixed) newDataEvent.getRecords()[0]).getUnderlyingObject()[11];
            assertTrue(attitude.getDoubleValue(0) >= -360 && attitude.getDoubleValue(0) <= 360);
            assertTrue(attitude.getDoubleValue(1) >= -90 && attitude.getDoubleValue(1) <= 90);
            assertTrue(attitude.getDoubleValue(2) >= -180 && attitude.getDoubleValue(2) <= 180);

            DataBlock platformHPR = ((DataBlockMixed) newDataEvent.getRecords()[0]).getUnderlyingObject()[12];
            assertTrue(platformHPR.getDoubleValue(0) >= 0 && platformHPR.getDoubleValue(0) <= 360);
            assertTrue(platformHPR.getDoubleValue(1) >= -90 && platformHPR.getDoubleValue(1) <= 90);
            assertTrue(platformHPR.getDoubleValue(2) >= -180 && platformHPR.getDoubleValue(2) <= 180);

            DataBlock platformGroundSpeed = ((DataBlockMixed) newDataEvent.getRecords()[0]).getUnderlyingObject()[13];
            assertTrue(platformGroundSpeed.getDoubleValue(0) >= 0);

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
