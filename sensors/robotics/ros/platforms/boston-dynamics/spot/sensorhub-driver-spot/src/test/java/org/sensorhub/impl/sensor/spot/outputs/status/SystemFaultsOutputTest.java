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
import spot_msgs.SystemFault;
import spot_msgs.SystemFaultState;
import std_msgs.Header;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class SystemFaultsOutputTest {

    private SpotSensor driver = null;

    private final RosCore rosCore = RosCore.newPrivate(11311);

    @Before
    public void init() throws Exception {

        rosCore.start();

        SpotConfig config = new SpotConfig();

        config.spotStatusConfig.systemFaultsStatusOutput.enabled = true;

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
    public void testSystemFaultsOutput() throws Exception {

        driver.start();

        // register listener on data interface
        IStreamingDataInterface di = driver.getStatusOutputs().get("SystemFaults");
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
            int numFaultStates = dataBlock.getIntValue(index++);
            int numHistoricalFaultStates = dataBlock.getIntValue(index++);

            assertEquals(1, numFaultStates);
            assertEquals(1, numHistoricalFaultStates);

            for (int idx = 0; idx < numFaultStates; ++idx) {
                assertTrue("MockSystemFault".equalsIgnoreCase(dataBlock.getStringValue(index++)));
                assertEquals(5.0, dataBlock.getDoubleValue(index++), 0.1);
                assertEquals(0, dataBlock.getIntValue(index++));
                assertTrue("123456789".equalsIgnoreCase(dataBlock.getStringValue(index++)));
                assertTrue("MockErrorMessage".equalsIgnoreCase(dataBlock.getStringValue(index++)));
                assertTrue("SEVERITY_UNKNOWN".equalsIgnoreCase(dataBlock.getStringValue(index++)));
                assertEquals(2, dataBlock.getIntValue(index++));
                assertTrue("Attribute A".equalsIgnoreCase(dataBlock.getStringValue(index++)));
                assertTrue("Attribute B".equalsIgnoreCase(dataBlock.getStringValue(index++)));
            }

            for (int idx = 0; idx < numHistoricalFaultStates; ++idx) {
                assertTrue("MockSystemFault".equalsIgnoreCase(dataBlock.getStringValue(index++)));
                assertEquals(5.0, dataBlock.getDoubleValue(index++), 0.1);
                assertEquals(0, dataBlock.getIntValue(index++));
                assertTrue("123456789".equalsIgnoreCase(dataBlock.getStringValue(index++)));
                assertTrue("MockErrorMessage".equalsIgnoreCase(dataBlock.getStringValue(index++)));
                assertTrue("SEVERITY_UNKNOWN".equalsIgnoreCase(dataBlock.getStringValue(index++)));
                assertEquals(2, dataBlock.getIntValue(index++));
                assertTrue("Attribute A".equalsIgnoreCase(dataBlock.getStringValue(index++)));
                assertTrue("Attribute B".equalsIgnoreCase(dataBlock.getStringValue(index++)));
            }
        };

        di.registerListener(listener);

        ((SystemFaultsOutput) di).onNewMessage(new MockSystemStateFaults());
    }

    static class MockSystemStateFaults implements SystemFaultState{

        @Override
        public List<SystemFault> getFaults() {
            ArrayList<SystemFault> systemFaults = new ArrayList<>();
            systemFaults.add(new MockSystemFault());
            return systemFaults;
        }

        @Override
        public void setFaults(List<SystemFault> value) {

        }

        @Override
        public List<SystemFault> getHistoricalFaults() {
            ArrayList<SystemFault> systemFaults = new ArrayList<>();
            systemFaults.add(new MockSystemFault());
            return systemFaults;
        }

        @Override
        public void setHistoricalFaults(List<SystemFault> value) {

        }

        @Override
        public RawMessage toRawMessage() {
            return null;
        }
    }

    static class MockSystemFault implements SystemFault {

        @Override
        public Header getHeader() {
            return null;
        }

        @Override
        public void setHeader(Header value) {

        }

        @Override
        public String getName() {
            return "MockSystemFault";
        }

        @Override
        public void setName(String value) {

        }

        @Override
        public Duration getDuration() {
            return new Duration(5.0);
        }

        @Override
        public void setDuration(Duration value) {

        }

        @Override
        public int getCode() {
            return 0;
        }

        @Override
        public void setCode(int value) {

        }

        @Override
        public long getUid() {
            return 123456789L;
        }

        @Override
        public void setUid(long value) {

        }

        @Override
        public String getErrorMessage() {
            return "MockErrorMessage";
        }

        @Override
        public void setErrorMessage(String value) {

        }

        @Override
        public List<String> getAttributes() {
            ArrayList<String> attributes = new ArrayList<>();
            attributes.add("Attribute A");
            attributes.add("Attribute B");
            return attributes;
        }

        @Override
        public void setAttributes(List<String> value) {

        }

        @Override
        public byte getSeverity() {
            return 0;
        }

        @Override
        public void setSeverity(byte value) {

        }

        @Override
        public RawMessage toRawMessage() {
            return null;
        }
    }
}
