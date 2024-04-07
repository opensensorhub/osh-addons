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
import spot_msgs.BatteryState;
import spot_msgs.BatteryStateArray;
import std_msgs.Header;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class BatteryStatusOutputTest {

    private SpotSensor driver = null;

    private final RosCore rosCore = RosCore.newPrivate(11311);

    @Before
    public void init() throws Exception {

        rosCore.start();

        SpotConfig config = new SpotConfig();

        config.spotPoseConfig.enabled = false;
        config.spotMotionConfig.enabled = false;
        config.spotLeaseConfig.enabled = false;
        config.spotStatusConfig.batteryStatusOutput.enabled = true;

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
    public void testBatteryStatusOutput() throws Exception {

        driver.start();

        // register listener on data interface
        IStreamingDataInterface di = driver.getStatusOutputs().get("BatteryStatus");
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
            assertEquals(1, dataBlock.getIntValue(1));
            assertEquals("BatteryIdentifier", dataBlock.getStringValue(2));
            assertEquals(50.0, dataBlock.getDoubleValue(3), 0.1);
            assertEquals(5.0, dataBlock.getDoubleValue(4), 0.1);
            assertEquals(1.0, dataBlock.getDoubleValue(5), 0.1);
            assertEquals(5.0, dataBlock.getDoubleValue(6), 0.1);
            assertEquals("MISSING", dataBlock.getStringValue(7));
            assertEquals(4, dataBlock.getIntValue(8));
            assertEquals(32.0, dataBlock.getDoubleValue(9), 0.1);
            assertEquals(33.0, dataBlock.getDoubleValue(10), 0.1);
            assertEquals(34.0, dataBlock.getDoubleValue(11), 0.1);
            assertEquals(35.0, dataBlock.getDoubleValue(12), 0.1);
        };

        di.registerListener(listener);

        ((BatteryStatusOutput) di).onNewMessage(new MockBatteryStateArray());
    }

    static class MockBatteryStateArray implements BatteryStateArray {

        @Override
        public List<BatteryState> getBatteryStates() {
            List<BatteryState> batteryStates = new ArrayList<>();

            batteryStates.add(new MockBatteryState());

            return batteryStates;
        }

        @Override
        public void setBatteryStates(List<BatteryState> value) {

        }

        @Override
        public RawMessage toRawMessage() {
            return null;
        }
    }

    static class MockBatteryState implements BatteryState {

        @Override
        public Header getHeader() {
            return null;
        }

        @Override
        public void setHeader(Header value) {

        }

        @Override
        public String getIdentifier() {
            return "BatteryIdentifier";
        }

        @Override
        public void setIdentifier(String value) {

        }

        @Override
        public double getChargePercentage() {
            return 50;
        }

        @Override
        public void setChargePercentage(double value) {

        }

        @Override
        public Duration getEstimatedRuntime() {
            return Duration.fromMillis(5000);
        }

        @Override
        public void setEstimatedRuntime(Duration value) {

        }

        @Override
        public double getCurrent() {
            return 1.0;
        }

        @Override
        public void setCurrent(double value) {

        }

        @Override
        public double getVoltage() {
            return 5.0;
        }

        @Override
        public void setVoltage(double value) {

        }

        @Override
        public double[] getTemperatures() {
            return new double[] {32, 33, 34, 35};
        }

        @Override
        public void setTemperatures(double[] value) {

        }

        @Override
        public byte getStatus() {
            return 1;
        }

        @Override
        public void setStatus(byte value) {

        }

        @Override
        public RawMessage toRawMessage() {
            return null;
        }
    }
}
