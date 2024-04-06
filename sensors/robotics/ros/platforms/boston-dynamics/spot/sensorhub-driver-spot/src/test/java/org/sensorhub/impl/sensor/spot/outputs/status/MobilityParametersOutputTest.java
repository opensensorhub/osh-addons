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

import geometry_msgs.*;
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
import spot_msgs.MobilityParams;
import spot_msgs.ObstacleParams;
import spot_msgs.TerrainParams;

import java.io.IOException;

import static org.junit.Assert.*;

public class MobilityParametersOutputTest {

    private SpotSensor driver = null;

    private final RosCore rosCore = RosCore.newPrivate(11311);

    @Before
    public void init() throws Exception {

        rosCore.start();

        SpotConfig config = new SpotConfig();

        config.spotPoseConfig.enabled = false;
        config.spotMotionConfig.enabled = false;
        config.spotLeaseConfig.enabled = false;
        config.spotStatusConfig.mobilityParamsStatusOutput.enabled = true;

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
    public void testMobilityParamsOutput() throws Exception {

        driver.start();

        // register listener on data interface
        IStreamingDataInterface di = driver.getStatusOutputs().get("MobilityParameters");
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
            assertEquals(1, dataBlock.getDoubleValue(1), 0.1);
            assertEquals(1, dataBlock.getDoubleValue(2), 0.1);
            assertEquals(1, dataBlock.getDoubleValue(3), 0.1);
            assertEquals(1, dataBlock.getDoubleValue(4), 0.1);
            assertEquals(1, dataBlock.getDoubleValue(5), 0.1);
            assertEquals(1, dataBlock.getDoubleValue(6), 0.1);
            assertEquals(.5, dataBlock.getDoubleValue(7), 0.1);
            assertEquals(1, dataBlock.getIntValue(8));
            assertTrue(dataBlock.getBooleanValue(9));
            assertTrue(dataBlock.getBooleanValue(10));
            assertEquals(.5, dataBlock.getDoubleValue(11), 0.1);
            assertTrue(dataBlock.getBooleanValue(12));
            assertTrue(dataBlock.getBooleanValue(13));
            assertTrue(dataBlock.getBooleanValue(14));
            assertTrue(dataBlock.getBooleanValue(15));
            assertEquals("OFF", dataBlock.getStringValue(16));
            assertEquals(2.5, dataBlock.getDoubleValue(17), .1);
            assertEquals(1, dataBlock.getDoubleValue(18), .1);
            assertEquals(1, dataBlock.getDoubleValue(19), .1);
            assertEquals(1, dataBlock.getDoubleValue(20), .1);
            assertEquals(1, dataBlock.getDoubleValue(21), .1);
            assertEquals(1, dataBlock.getDoubleValue(22), .1);
            assertEquals(1, dataBlock.getDoubleValue(23), .1);
            assertEquals(5, dataBlock.getIntValue(24));
        };

        di.registerListener(listener);

        ((MobilityParametersOutput) di).onNewMessage(new MobilityParametersOutputTest.MockMobilityParams());
    }

    static class MockPoint implements Point {

        @Override
        public double getX() {
            return 1;
        }

        @Override
        public void setX(double v) {

        }

        @Override
        public double getY() {
            return 1;
        }

        @Override
        public void setY(double v) {

        }

        @Override
        public double getZ() {
            return 1;
        }

        @Override
        public void setZ(double v) {

        }

        @Override
        public RawMessage toRawMessage() {
            return null;
        }
    }

    static class MockQuaternion implements Quaternion {

        @Override
        public double getX() {
            return 1;
        }

        @Override
        public void setX(double v) {

        }

        @Override
        public double getY() {
            return 1;
        }

        @Override
        public void setY(double v) {

        }

        @Override
        public double getZ() {
            return 1;
        }

        @Override
        public void setZ(double v) {

        }

        @Override
        public double getW() {
            return .5;
        }

        @Override
        public void setW(double v) {

        }

        @Override
        public RawMessage toRawMessage() {
            return null;
        }
    }

    static class MockPose implements Pose {

        @Override
        public Point getPosition() {
            return new MockPoint();
        }

        @Override
        public void setPosition(Point point) {

        }

        @Override
        public Quaternion getOrientation() {
            return new MockQuaternion();
        }

        @Override
        public void setOrientation(Quaternion quaternion) {

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
            return 1;
        }

        @Override
        public void setY(double v) {

        }

        @Override
        public double getZ() {
            return 1;
        }

        @Override
        public void setZ(double v) {

        }

        @Override
        public RawMessage toRawMessage() {
            return null;
        }
    }

    static class MockTwist implements Twist {

        @Override
        public Vector3 getLinear() {
            return new MobilityParametersOutputTest.MockVector3();
        }

        @Override
        public void setLinear(Vector3 vector3) {

        }

        @Override
        public Vector3 getAngular() {
            return new MobilityParametersOutputTest.MockVector3();
        }

        @Override
        public void setAngular(Vector3 vector3) {

        }

        @Override
        public RawMessage toRawMessage() {
            return null;
        }
    }

    static class MockObstacleParams implements ObstacleParams {

        @Override
        public boolean getDisableVisionFootObstacleAvoidance() {
            return true;
        }

        @Override
        public void setDisableVisionFootObstacleAvoidance(boolean value) {

        }

        @Override
        public boolean getDisableVisionFootConstraintAvoidance() {
            return true;
        }

        @Override
        public void setDisableVisionFootConstraintAvoidance(boolean value) {

        }

        @Override
        public boolean getDisableVisionBodyObstacleAvoidance() {
            return true;
        }

        @Override
        public void setDisableVisionBodyObstacleAvoidance(boolean value) {

        }

        @Override
        public float getObstacleAvoidancePadding() {
            return 0.5f;
        }

        @Override
        public void setObstacleAvoidancePadding(float value) {

        }

        @Override
        public boolean getDisableVisionFootObstacleBodyAssist() {
            return true;
        }

        @Override
        public void setDisableVisionFootObstacleBodyAssist(boolean value) {

        }

        @Override
        public boolean getDisableVisionNegativeObstacles() {
            return true;
        }

        @Override
        public void setDisableVisionNegativeObstacles(boolean value) {

        }

        @Override
        public RawMessage toRawMessage() {
            return null;
        }
    }

    static class MockTerrainParams implements TerrainParams {

        @Override
        public float getGroundMuHint() {
            return 2.5f;
        }

        @Override
        public void setGroundMuHint(float value) {

        }

        @Override
        public byte getGratedSurfacesMode() {
            return (byte)0x01;
        }

        @Override
        public void setGratedSurfacesMode(byte value) {

        }

        @Override
        public RawMessage toRawMessage() {
            return null;
        }
    }

    static class MockMobilityParams implements MobilityParams {

        @Override
        public Pose getBodyControl() {
            return new MockPose();
        }

        @Override
        public void setBodyControl(Pose value) {

        }

        @Override
        public int getLocomotionHint() {
            return 1;
        }

        @Override
        public void setLocomotionHint(int value) {

        }

        @Override
        public byte getSwingHeight() {
            return (byte)5;
        }

        @Override
        public void setSwingHeight(byte value) {

        }

        @Override
        public boolean getStairHint() {
            return true;
        }

        @Override
        public void setStairHint(boolean value) {

        }

        @Override
        public Twist getVelocityLimit() {
            return new MockTwist();
        }

        @Override
        public void setVelocityLimit(Twist value) {

        }

        @Override
        public ObstacleParams getObstacleParams() {
            return new MockObstacleParams();
        }

        @Override
        public void setObstacleParams(ObstacleParams value) {

        }

        @Override
        public TerrainParams getTerrainParams() {
            return new MockTerrainParams();
        }

        @Override
        public void setTerrainParams(TerrainParams value) {

        }

        @Override
        public RawMessage toRawMessage() {
            return null;
        }
    }
}
