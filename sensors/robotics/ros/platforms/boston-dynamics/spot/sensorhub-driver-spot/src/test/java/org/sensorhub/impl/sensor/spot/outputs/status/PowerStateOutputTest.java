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
import spot_msgs.PowerState;
import std_msgs.Header;

import java.io.IOException;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class PowerStateOutputTest {

    private SpotSensor driver = null;

    private final RosCore rosCore = RosCore.newPrivate(11311);

    @Before
    public void init() throws Exception {

        rosCore.start();

        SpotConfig config = new SpotConfig();

        config.spotPoseConfig.enabled = false;
        config.spotMotionConfig.enabled = false;
        config.spotLeaseConfig.enabled = false;
        config.spotStatusConfig.powerStateStatusOutput.enabled = true;

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
    public void testPowerStateOutput() throws Exception {

        driver.start();

        // register listener on data interface
        IStreamingDataInterface di = driver.getStatusOutputs().get("PowerState");
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
            assertEquals("STATE_UNKNOWN", dataBlock.getStringValue(1));
            assertEquals("STATE_UNKNOWN_SHORE_POWER", dataBlock.getStringValue(2));
            assertTrue(50 <= dataBlock.getDoubleValue(3));
            assertTrue(1.0 <= dataBlock.getDoubleValue(4));
        };

        di.registerListener(listener);

        ((PowerStateOutput) di).onNewMessage(new MockPowerState());
    }

    static class MockPowerState implements PowerState {


        @Override
        public Header getHeader() {
            return null;
        }

        @Override
        public void setHeader(Header value) {

        }

        @Override
        public byte getMotorPowerState() {
            return 0;
        }

        @Override
        public void setMotorPowerState(byte value) {

        }

        @Override
        public byte getShorePowerState() {
            return 0;
        }

        @Override
        public void setShorePowerState(byte value) {

        }

        @Override
        public double getLocomotionChargePercentage() {
            return 50;
        }

        @Override
        public void setLocomotionChargePercentage(double value) {

        }

        @Override
        public Duration getLocomotionEstimatedRuntime() {
            return Duration.fromNano(1000000000);
        }

        @Override
        public void setLocomotionEstimatedRuntime(Duration value) {

        }

        @Override
        public RawMessage toRawMessage() {
            return null;
        }
    }
}
