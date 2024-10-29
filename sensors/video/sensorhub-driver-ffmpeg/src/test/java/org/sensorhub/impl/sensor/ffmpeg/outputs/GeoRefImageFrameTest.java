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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class GeoRefImageFrameTest {

    private FFMPEGSensor driver = null;

    private final Object syncObject = new Object();

    @Before
    public void init() throws Exception {

        URL resource = FFMPEGSensor.class.getResource("sample-stream.ts");
        FFMPEGConfig config = new FFMPEGConfig();

        assert resource != null;
        config.connection.transportStreamPath = new File(resource.toURI()).getPath();
        config.outputs.enableGeoRefImageFrame = true;

        driver = new FFMPEGSensor();
        driver.setConfiguration(config);
        driver.init();
    }

    @After
    public void cleanup() throws Exception {

        driver.stop();
    }

    @Test
    public void testGeoRefImageFrame() throws Exception {

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

            assertTrue(dataBlock.getDoubleValue(1) >= -90 && dataBlock.getDoubleValue(1) <= 90);
            assertTrue(dataBlock.getDoubleValue(2) >= -180 && dataBlock.getDoubleValue(2) <= 180);
            assertTrue(dataBlock.getDoubleValue(3) > 0);

            assertTrue(dataBlock.getDoubleValue(4) >= -90 && dataBlock.getDoubleValue(4) <= 90);
            assertTrue(dataBlock.getDoubleValue(5) >= -180 && dataBlock.getDoubleValue(5) <= 180);

            assertTrue(dataBlock.getDoubleValue(6) >= -90 && dataBlock.getDoubleValue(6) <= 90);
            assertTrue(dataBlock.getDoubleValue(7) >= -180 && dataBlock.getDoubleValue(7) <= 180);

            assertTrue(dataBlock.getDoubleValue(8) >= -90 && dataBlock.getDoubleValue(8) <= 90);
            assertTrue(dataBlock.getDoubleValue(9) >= -180 && dataBlock.getDoubleValue(9) <= 180);

            assertTrue(dataBlock.getDoubleValue(10) >= -90 && dataBlock.getDoubleValue(10) <= 90);
            assertTrue(dataBlock.getDoubleValue(11) >= -180 && dataBlock.getDoubleValue(11) <= 180);

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
