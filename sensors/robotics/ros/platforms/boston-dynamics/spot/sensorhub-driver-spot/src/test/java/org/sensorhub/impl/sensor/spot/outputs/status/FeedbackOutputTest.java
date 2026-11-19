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
import spot_msgs.Feedback;

import java.io.IOException;

import static org.junit.Assert.*;

public class FeedbackOutputTest {

    private SpotSensor driver = null;

    private final RosCore rosCore = RosCore.newPrivate(11311);

    @Before
    public void init() throws Exception {

        rosCore.start();

        SpotConfig config = new SpotConfig();

        config.spotPoseConfig.enabled = false;
        config.spotMotionConfig.enabled = false;
        config.spotLeaseConfig.enabled = false;
        config.spotStatusConfig.feedbackStatusOutput.enabled = true;

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
    public void testFeedbackOutput() throws Exception {

        driver.start();

        // register listener on data interface
        IStreamingDataInterface di = driver.getStatusOutputs().get("Feedback");
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
            assertFalse(dataBlock.getBooleanValue(1));
            assertTrue(dataBlock.getBooleanValue(2));
            assertFalse(dataBlock.getBooleanValue(3));
            assertEquals("SPOT_TEST_123", dataBlock.getStringValue(4));
            assertEquals("ROTWEILER", dataBlock.getStringValue(5));
            assertEquals("1.0_TEST", dataBlock.getStringValue(6));
            assertEquals("Dino", dataBlock.getStringValue(7));
            assertEquals("CSN_123", dataBlock.getStringValue(8));
        };

        di.registerListener(listener);

        ((FeedbackOutput) di).onNewMessage(new FeedbackOutputTest.MockFeedback());
    }

    static class MockFeedback implements Feedback {

        @Override
        public boolean getStanding() {
            return false;
        }

        @Override
        public void setStanding(boolean value) {

        }

        @Override
        public boolean getSitting() {
            return true;
        }

        @Override
        public void setSitting(boolean value) {

        }

        @Override
        public boolean getMoving() {
            return false;
        }

        @Override
        public void setMoving(boolean value) {

        }

        @Override
        public String getSerialNumber() {
            return "SPOT_TEST_123";
        }

        @Override
        public void setSerialNumber(String value) {

        }

        @Override
        public String getSpecies() {
            return "ROTWEILER";
        }

        @Override
        public void setSpecies(String value) {

        }

        @Override
        public String getVersion() {
            return "1.0_TEST";
        }

        @Override
        public void setVersion(String value) {

        }

        @Override
        public String getNickname() {
            return "Dino";
        }

        @Override
        public void setNickname(String value) {

        }

        @Override
        public String getComputerSerialNumber() {
            return "CSN_123";
        }

        @Override
        public void setComputerSerialNumber(String value) {

        }

        @Override
        public RawMessage toRawMessage() {
            return null;
        }
    }
}
