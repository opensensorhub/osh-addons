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

import geometry_msgs.Twist;
import geometry_msgs.TwistWithCovariance;
import geometry_msgs.TwistWithCovarianceStamped;
import geometry_msgs.Vector3;
import net.opengis.swe.v20.DataBlock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ros.RosCore;
import org.ros.internal.message.RawMessage;
import org.ros.message.Time;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.api.event.IEventListener;
import org.sensorhub.impl.sensor.spot.SpotSensor;
import org.sensorhub.impl.sensor.spot.config.SpotConfig;
import org.vast.data.DataBlockMixed;
import org.vast.swe.SWEHelper;
import std_msgs.Header;

import java.io.IOException;

import static org.junit.Assert.*;

public class OdometryOutputTest {

    private SpotSensor driver = null;

    private final RosCore rosCore = RosCore.newPrivate(11311);

    @Before
    public void init() throws Exception {

        rosCore.start();

        SpotConfig config = new SpotConfig();

        config.spotPoseConfig.enabled = false;
        config.spotMotionConfig.enabled = false;
        config.spotLeaseConfig.enabled = false;
        config.spotStatusConfig.odometryStatusOutput.enabled = true;

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
    public void testOdometryOutput() throws Exception {

        driver.start();

        // register listener on data interface
        IStreamingDataInterface di = driver.getStatusOutputs().get("Odometry");
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

            assertTrue(dataBlock.getDoubleValue(0) <= System.currentTimeMillis() / 1000.);
            assertTrue(1.0 <= ((DataBlockMixed) dataBlock).getUnderlyingObject()[1].getDoubleValue(0));
            assertTrue(1.0 <= ((DataBlockMixed) dataBlock).getUnderlyingObject()[1].getDoubleValue(1));
            assertTrue(1.0 <= ((DataBlockMixed) dataBlock).getUnderlyingObject()[1].getDoubleValue(2));

            assertTrue(1.0 <= ((DataBlockMixed) dataBlock).getUnderlyingObject()[2].getDoubleValue(0));
            assertTrue(1.0 <= ((DataBlockMixed) dataBlock).getUnderlyingObject()[2].getDoubleValue(1));
            assertTrue(1.0 <= ((DataBlockMixed) dataBlock).getUnderlyingObject()[2].getDoubleValue(2));

            assertArrayEquals(
                    (double[]) ((DataBlockMixed) dataBlock).getUnderlyingObject()[3].getUnderlyingObject(),
                    new double[]{1, 2, 3, 4, 5, 6, 7, 8, 9}, 0.1);
        };

        di.registerListener(listener);

        ((OdometryOutput) di).onNewMessage(new MockTwistWithCovarianceStamped());
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
            return new MockVector3();
        }

        @Override
        public void setLinear(Vector3 vector3) {

        }

        @Override
        public Vector3 getAngular() {
            return new MockVector3();
        }

        @Override
        public void setAngular(Vector3 vector3) {

        }

        @Override
        public RawMessage toRawMessage() {
            return null;
        }
    }

    static class MockTwistWithCovariance implements TwistWithCovariance {

        @Override
        public Twist getTwist() {
            return new MockTwist();
        }

        @Override
        public void setTwist(Twist twist) {

        }

        @Override
        public double[] getCovariance() {
            return new double[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
        }

        @Override
        public void setCovariance(double[] doubles) {

        }

        @Override
        public RawMessage toRawMessage() {
            return null;
        }
    }

    static class MockHeader implements Header {

        @Override
        public int getSeq() {
            return 1;
        }

        @Override
        public void setSeq(int i) {

        }

        @Override
        public Time getStamp() {
            return Time.fromMillis(System.currentTimeMillis());
        }

        @Override
        public void setStamp(Time time) {

        }

        @Override
        public String getFrameId() {
            return "TestFrameId";
        }

        @Override
        public void setFrameId(String s) {

        }

        @Override
        public RawMessage toRawMessage() {
            return null;
        }
    }

    static class MockTwistWithCovarianceStamped implements TwistWithCovarianceStamped {

        @Override
        public Header getHeader() {
            return new MockHeader();
        }

        @Override
        public void setHeader(Header header) {

        }

        @Override
        public TwistWithCovariance getTwist() {
            return new MockTwistWithCovariance();
        }

        @Override
        public void setTwist(TwistWithCovariance twistWithCovariance) {

        }

        @Override
        public RawMessage toRawMessage() {
            return null;
        }
    }
}
