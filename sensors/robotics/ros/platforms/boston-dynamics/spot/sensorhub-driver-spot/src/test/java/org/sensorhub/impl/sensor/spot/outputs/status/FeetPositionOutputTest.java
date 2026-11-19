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

import geometry_msgs.Point;
import geometry_msgs.Vector3;
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
import spot_msgs.FootState;
import spot_msgs.FootStateArray;
import spot_msgs.TerrainState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class FeetPositionOutputTest {

    private SpotSensor driver = null;

    private final RosCore rosCore = RosCore.newPrivate(11311);

    @Before
    public void init() throws Exception {

        rosCore.start();

        SpotConfig config = new SpotConfig();

        config.spotStatusConfig.feetPositionStatusOutput.enabled = true;

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
    public void testFeetPositionOutput() throws Exception {

        driver.start();

        // register listener on data interface
        IStreamingDataInterface di = driver.getStatusOutputs().get("FeetPosition");
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
                assertTrue("CONTACT_UNKNOWN".equalsIgnoreCase(dataBlock.getStringValue(index++)));
                assertEquals(1.0, dataBlock.getDoubleValue(index++), 0.1);
                assertEquals(2.0, dataBlock.getDoubleValue(index++), 0.1);
                assertEquals(3.0, dataBlock.getDoubleValue(index++), 0.1);
                assertTrue("MockFrameName".equalsIgnoreCase(dataBlock.getStringValue(index++)));
                assertEquals(0.0, dataBlock.getDoubleValue(index++), 0.1);
                assertEquals(0.0, dataBlock.getDoubleValue(index++), 0.1);
                assertEquals(0.0, dataBlock.getDoubleValue(index++), 0.1);
                assertEquals(1.0, dataBlock.getDoubleValue(index++), 0.1);
                assertEquals(2.0, dataBlock.getDoubleValue(index++), 0.1);
                assertEquals(3.0, dataBlock.getDoubleValue(index++), 0.1);
                assertEquals(1.0, dataBlock.getDoubleValue(index++), 0.1);
                assertEquals(2.0, dataBlock.getDoubleValue(index++), 0.1);
                assertEquals(3.0, dataBlock.getDoubleValue(index++), 0.1);
                assertEquals(1.0, dataBlock.getDoubleValue(index++), 0.1);
                assertEquals(2.0, dataBlock.getDoubleValue(index++), 0.1);
                assertEquals(3.0, dataBlock.getDoubleValue(index++), 0.1);
            }
        };

        di.registerListener(listener);

        ((FeetPositionOutput) di).onNewMessage(new MockFootStateArray());
    }

    static class MockFootStateArray implements FootStateArray {

        @Override
        public List<FootState> getStates() {
            ArrayList<FootState> footStates = new ArrayList<>();
            footStates.add(new MockFootState());
            return footStates;
        }

        @Override
        public void setStates(List<FootState> value) {

        }

        @Override
        public RawMessage toRawMessage() {
            return null;
        }
    }

    static class MockFootState implements FootState {

        @Override
        public Point getFootPositionRtBody() {
            return new MockPoint();
        }

        @Override
        public void setFootPositionRtBody(Point value) {

        }

        @Override
        public byte getContact() {
            return 0;
        }

        @Override
        public void setContact(byte value) {

        }

        @Override
        public TerrainState getTerrain() {
            return new MockTerrainState();
        }

        @Override
        public void setTerrain(TerrainState value) {

        }

        @Override
        public RawMessage toRawMessage() {
            return null;
        }
    }

    static class MockPoint implements Point {

        @Override
        public double getX() {
            return 1.0;
        }

        @Override
        public void setX(double v) {

        }

        @Override
        public double getY() {
            return 2.0;
        }

        @Override
        public void setY(double v) {

        }

        @Override
        public double getZ() {
            return 3.0;
        }

        @Override
        public void setZ(double v) {

        }

        @Override
        public RawMessage toRawMessage() {
            return null;
        }
    }

    static class MockTerrainState implements TerrainState {

        @Override
        public float getGroundMuEst() {
            return 0;
        }

        @Override
        public void setGroundMuEst(float value) {

        }

        @Override
        public String getFrameName() {
            return "MockFrameName";
        }

        @Override
        public void setFrameName(String value) {

        }

        @Override
        public Vector3 getFootSlipDistanceRtFrame() {
            return new MockVector3();
        }

        @Override
        public void setFootSlipDistanceRtFrame(Vector3 value) {

        }

        @Override
        public Vector3 getFootSlipVelocityRtFrame() {
            return new MockVector3();
        }

        @Override
        public void setFootSlipVelocityRtFrame(Vector3 value) {

        }

        @Override
        public Vector3 getGroundContactNormalRtFrame() {
            return new MockVector3();
        }

        @Override
        public void setGroundContactNormalRtFrame(Vector3 value) {

        }

        @Override
        public float getVisualSurfaceGroundPenetrationMean() {
            return 0;
        }

        @Override
        public void setVisualSurfaceGroundPenetrationMean(float value) {

        }

        @Override
        public float getVisualSurfaceGroundPenetrationStd() {
            return 0;
        }

        @Override
        public void setVisualSurfaceGroundPenetrationStd(float value) {

        }

        @Override
        public RawMessage toRawMessage() {
            return null;
        }
    }

    static class MockVector3 implements Vector3 {

        @Override
        public double getX() {
            return 1;
        }

        @Override
        public void setX(double v) {

        }

        @Override
        public double getY() {
            return 2;
        }

        @Override
        public void setY(double v) {

        }

        @Override
        public double getZ() {
            return 3;
        }

        @Override
        public void setZ(double v) {

        }

        @Override
        public RawMessage toRawMessage() {
            return null;
        }
    }
}
