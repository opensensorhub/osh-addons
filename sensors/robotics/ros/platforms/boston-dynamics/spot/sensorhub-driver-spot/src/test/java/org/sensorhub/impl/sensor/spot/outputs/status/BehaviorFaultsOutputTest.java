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
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.api.event.IEventListener;
import org.sensorhub.impl.sensor.spot.SpotSensor;
import org.sensorhub.impl.sensor.spot.config.SpotConfig;
import org.vast.swe.SWEHelper;
import spot_msgs.BehaviorFault;
import spot_msgs.BehaviorFaultState;
import std_msgs.Header;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class BehaviorFaultsOutputTest {

    private SpotSensor driver = null;

    private final RosCore rosCore = RosCore.newPrivate(11311);

    @Before
    public void init() throws Exception {

        rosCore.start();

        SpotConfig config = new SpotConfig();

        config.spotPoseConfig.enabled = false;
        config.spotMotionConfig.enabled = false;
        config.spotLeaseConfig.enabled = false;
        config.spotStatusConfig.behaviorFaultsStatusOutput.enabled = true;

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
    public void testBehaviorFaultsOutput() throws Exception {

        driver.start();

        // register listener on data interface
        IStreamingDataInterface di = driver.getStatusOutputs().get("BehaviorFaults");
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

            int index = 0;
            assertTrue(dataBlock.getDoubleValue(index++) > 0);
            int numFaults = dataBlock.getIntValue(index++);
            assertEquals(3, numFaults);
            for (int idx = 0; idx < numFaults; ++idx) {
                assertEquals(1, dataBlock.getIntValue(index++));
                assertTrue("CAUSE_UNKNOWN".equalsIgnoreCase(dataBlock.getStringValue(index++)));
                assertTrue("STATUS_UNKNOWN".equalsIgnoreCase(dataBlock.getStringValue(index++)));
            }
        };

        di.registerListener(listener);

        ((BehaviorFaultsOutput) di).onNewMessage(new MockBehaviorFaultState());
    }

    static class MockBehaviorFaultState implements BehaviorFaultState {

        @Override
        public List<BehaviorFault> getFaults() {

            List<BehaviorFault> faults = new ArrayList<>();

            faults.add(new MockBehaviorFault());
            faults.add(new MockBehaviorFault());
            faults.add(new MockBehaviorFault());

            return faults;
        }

        @Override
        public void setFaults(List<BehaviorFault> value) {

        }

        @Override
        public RawMessage toRawMessage() {
            return null;
        }
    }

    static class MockBehaviorFault implements BehaviorFault {

        @Override
        public Header getHeader() {
            return null;
        }

        @Override
        public void setHeader(Header value) {

        }

        @Override
        public int getBehaviorFaultId() {
            return 1;
        }

        @Override
        public void setBehaviorFaultId(int value) {

        }

        @Override
        public byte getCause() {
            return 0;
        }

        @Override
        public void setCause(byte value) {

        }

        @Override
        public byte getStatus() {
            return 0;
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
