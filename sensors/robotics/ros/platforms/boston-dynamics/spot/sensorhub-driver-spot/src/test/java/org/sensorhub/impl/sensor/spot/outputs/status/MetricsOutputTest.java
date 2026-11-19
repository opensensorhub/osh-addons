/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023-2024 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.spot.outputs.status;

import net.opengis.swe.v20.DataBlock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ros.RosCore;
import org.ros.internal.message.RawMessage;
import org.ros.message.Duration;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.api.event.IEventListener;
import org.sensorhub.impl.sensor.spot.SpotSensor;
import org.sensorhub.impl.sensor.spot.config.SpotConfig;
import org.vast.swe.SWEHelper;
import spot_msgs.Metrics;
import std_msgs.Header;

import java.io.IOException;

import static org.junit.Assert.*;

public class MetricsOutputTest {

    private SpotSensor driver = null;

    private final RosCore rosCore = RosCore.newPrivate(11311);

    @Before
    public void init() throws Exception {

        rosCore.start();

        SpotConfig config = new SpotConfig();

        config.spotPoseConfig.enabled = false;
        config.spotMotionConfig.enabled = false;
        config.spotLeaseConfig.enabled = false;
        config.spotStatusConfig.metricsOutput.enabled = true;

        driver = new SpotSensor();
        driver.setConfiguration(config);
        driver.init();
    }

    @After
    public void cleanup() throws Exception {

        driver.stop();

        rosCore.shutdown();
    }

    @Test
    public void testMetricsOutput() throws Exception {

        driver.start();

        // register listener on data interface
        IStreamingDataInterface di = driver.getStatusOutputs().get("Metrics");
        var dataWriter = SWEHelper.createDataWriter(di.getRecommendedEncoding());
        dataWriter.setDataComponents(di.getRecordDescription());
        dataWriter.setOutput(System.out);

        IEventListener listener = event -> {

            assertTrue(event instanceof DataEvent);
            DataEvent newDataEvent = (DataEvent) event;

            DataBlock dataBlock = newDataEvent.getRecords()[0];
            try {
                dataWriter.write(dataBlock);
                dataWriter.flush();
            } catch (IOException e) {
                fail("Error writing data");
            }

            assertTrue(dataBlock.getDoubleValue(0) > 0);
            assertEquals(500.f, dataBlock.getDoubleValue(1), 0.1);
            assertEquals(100, dataBlock.getIntValue(2));
            assertEquals(2.0, dataBlock.getDoubleValue(3), 0.1);
            assertEquals(1.0, dataBlock.getDoubleValue(4), 0.1);
        };

        di.registerListener(listener);

        ((MetricsOutput) di).onNewMessage(new MetricsOutputTest.MockMetrics());
    }

    static class MockMetrics implements Metrics {

        @Override
        public Header getHeader() {
            return null;
        }

        @Override
        public void setHeader(Header value) {

        }

        @Override
        public float getDistance() {
            return 500f;
        }

        @Override
        public void setDistance(float value) {

        }

        @Override
        public int getGaitCycles() {
            return 100;
        }

        @Override
        public void setGaitCycles(int value) {

        }

        @Override
        public Duration getTimeMoving() {
            return Duration.fromNano(1000000000);
        }

        @Override
        public void setTimeMoving(Duration value) {

        }

        @Override
        public Duration getElectricPower() {
            return Duration.fromNano(2000000000);
        }

        @Override
        public void setElectricPower(Duration value) {

        }

        @Override
        public RawMessage toRawMessage() {
            return null;
        }
    }
}
