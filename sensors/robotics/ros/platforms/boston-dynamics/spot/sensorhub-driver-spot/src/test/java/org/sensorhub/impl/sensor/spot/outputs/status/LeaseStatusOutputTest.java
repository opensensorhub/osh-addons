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
import spot_msgs.Lease;
import spot_msgs.LeaseArray;
import spot_msgs.LeaseOwner;
import spot_msgs.LeaseResource;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class LeaseStatusOutputTest {

    private SpotSensor driver = null;

    private final RosCore rosCore = RosCore.newPrivate(11311);

    @Before
    public void init() throws Exception {

        rosCore.start();

        SpotConfig config = new SpotConfig();

        config.spotStatusConfig.leaseStatusOutput.enabled = true;

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
    public void testLeaseStatusOutput() throws Exception {

        driver.start();

        // register listener on data interface
        IStreamingDataInterface di = driver.getStatusOutputs().get("LeaseStatus");
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
            int numStates = dataBlock.getIntValue(index++);
            assertEquals(1, numStates);
            for (int idx = 0; idx < numStates; ++idx) {
                assertTrue(dataBlock.getDoubleValue(index++) > 0);
                assertTrue("OpenSensorHubLease".equalsIgnoreCase(dataBlock.getStringValue(index++)));
                assertEquals(10, dataBlock.getIntValue(index++));
                assertEquals(1.0, dataBlock.getDoubleValue(index++), 0.1);
                assertEquals(2.0, dataBlock.getDoubleValue(index++), 0.1);
                assertEquals(3.0, dataBlock.getDoubleValue(index++), 0.1);
                assertEquals(4.0, dataBlock.getDoubleValue(index++), 0.1);
                assertEquals(5.0, dataBlock.getDoubleValue(index++), 0.1);
                assertEquals(6.0, dataBlock.getDoubleValue(index++), 0.1);
                assertEquals(7.0, dataBlock.getDoubleValue(index++), 0.1);
                assertEquals(8.0, dataBlock.getDoubleValue(index++), 0.1);
                assertEquals(9.0, dataBlock.getDoubleValue(index++), 0.1);
                assertEquals(0.0, dataBlock.getDoubleValue(index++), 0.1);
                assertTrue("OpenSensorHubLeaseResource".equalsIgnoreCase(dataBlock.getStringValue(index++)));
                assertTrue("OpenSensorHubClient".equalsIgnoreCase(dataBlock.getStringValue(index++)));
                assertTrue("OpenSensorHubUser".equalsIgnoreCase(dataBlock.getStringValue(index++)));
            }
        };

        di.registerListener(listener);

        ((LeaseStatusOutput) di).onNewMessage(new MockLeaseArray());
    }

    static class MockLeaseArray implements LeaseArray {

        @Override
        public List<LeaseResource> getResources() {
            return Collections.singletonList(new MockLeaseResource());
        }

        @Override
        public void setResources(List<LeaseResource> value) {

        }

        @Override
        public RawMessage toRawMessage() {
            return null;
        }
    }

    private static class MockLeaseResource implements LeaseResource {

        @Override
        public String getResource() {
            return "OpenSensorHubLeaseResource";
        }

        @Override
        public void setResource(String value) {

        }

        @Override
        public Lease getLease() {
            return new MockLease();
        }

        @Override
        public void setLease(Lease value) {

        }

        @Override
        public LeaseOwner getLeaseOwner() {
            return new MockLeaseOwner();
        }

        @Override
        public void setLeaseOwner(LeaseOwner value) {

        }

        @Override
        public RawMessage toRawMessage() {
            return null;
        }
    }

    private static class MockLeaseOwner implements LeaseOwner {

        @Override
        public String getClientName() {
            return "OpenSensorHubClient";
        }

        @Override
        public void setClientName(String value) {

        }

        @Override
        public String getUserName() {
            return "OpenSensorHubUser";
        }

        @Override
        public void setUserName(String value) {

        }

        @Override
        public RawMessage toRawMessage() {
            return null;
        }
    }

    private static class MockLease implements Lease {

        @Override
        public String getResource() {
            return "OpenSensorHubLease";
        }

        @Override
        public void setResource(String value) {

        }

        @Override
        public String getEpoch() {
            return System.currentTimeMillis() + "";
        }

        @Override
        public void setEpoch(String value) {

        }

        @Override
        public int[] getSequence() {
            return new int[] {1,2,3,4,5,6,7,8,9,0};
        }

        @Override
        public void setSequence(int[] value) {

        }

        @Override
        public RawMessage toRawMessage() {
            return null;
        }
    }
}
