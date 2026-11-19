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
import spot_msgs.EStopState;
import spot_msgs.EStopStateArray;
import std_msgs.Header;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class EStopStatusOutputTest {

    private SpotSensor driver = null;

    private final RosCore rosCore = RosCore.newPrivate(11311);

    @Before
    public void init() throws Exception {

        rosCore.start();

        SpotConfig config = new SpotConfig();

        config.spotStatusConfig.eStopStatusOutput.enabled = true;

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
    public void testEStopStatusOutput() throws Exception {

        driver.start();

        // register listener on data interface
        IStreamingDataInterface di = driver.getStatusOutputs().get("EStopStatus");
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
                assertTrue("MockName".equalsIgnoreCase(dataBlock.getStringValue(index++)));
                assertTrue("Mock state description".equalsIgnoreCase(dataBlock.getStringValue(index++)));
                assertTrue("TYPE_HARDWARE".equalsIgnoreCase(dataBlock.getStringValue(index++)));
                assertTrue("STATE_ESTOPPED".equalsIgnoreCase(dataBlock.getStringValue(index++)));
            }
        };

        di.registerListener(listener);

        ((EStopStatusOutput) di).onNewMessage(new MockEStopStateArray());
    }

    static class MockEStopStateArray implements EStopStateArray {

        @Override
        public List<EStopState> getEstopStates() {
            ArrayList<EStopState> eStopStates = new ArrayList<>();
            eStopStates.add(new MockEstopState());
            return eStopStates;
        }

        @Override
        public void setEstopStates(List<EStopState> value) {

        }

        @Override
        public RawMessage toRawMessage() {
            return null;
        }
    }

    static class MockEstopState implements EStopState {

        @Override
        public Header getHeader() {
            return null;
        }

        @Override
        public void setHeader(Header value) {

        }

        @Override
        public String getName() {
            return "MockName";
        }

        @Override
        public void setName(String value) {

        }

        @Override
        public byte getType() {
            return 1;
        }

        @Override
        public void setType(byte value) {

        }

        @Override
        public byte getState() {
            return 1;
        }

        @Override
        public void setState(byte value) {

        }

        @Override
        public String getStateDescription() {
            return "Mock state description";
        }

        @Override
        public void setStateDescription(String value) {

        }

        @Override
        public RawMessage toRawMessage() {
            return null;
        }
    }
}
