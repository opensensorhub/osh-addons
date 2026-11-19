/*
 *  The contents of this file are subject to the Mozilla Public License, v. 2.0.
 *  If a copy of the MPL was not distributed with this file, You can obtain one
 *  at http://mozilla.org/MPL/2.0/.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 *  for the specific language governing rights and limitations under the License.
 *
 *  Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 */
package org.sensorhub.impl.sensor.imu;

import org.sensorhub.impl.ros.config.RosMasterConfig;
import org.sensorhub.impl.ros.nodes.pubsub.RosSubscriberNode;
import org.sensorhub.impl.ros.output.RosSensorOutput;
import org.sensorhub.impl.ros.utils.RosUtils;
import net.opengis.swe.v20.*;
import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.sensorhub.api.data.DataEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.DataBlockMixed;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;
import sensor_msgs.Imu;

import java.net.URI;

/**
 * Output specification and provider for ...
 *
 * @author Nick Garay
 * @since Feb. 2, 2023
 */
public class ImuOutput extends RosSensorOutput<ImuSensor> {

    private static final String SENSOR_OUTPUT_NAME = "IMU";
    private static final String SENSOR_OUTPUT_LABEL = "IMU";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "IMU Measurements";

    private static final String NODE_NAME_STR = "/SensorHub/imu";

    private static final String TOPIC_STR = "/imu/data";

    private static final Logger logger = LoggerFactory.getLogger(ImuOutput.class);

    private NodeMainExecutor nodeMainExecutor;

    private RosSubscriberNode subscriberNode;

    /**
     * Constructor
     *
     * @param parentImuSensor Sensor driver providing this output
     */
    ImuOutput(ImuSensor parentImuSensor) {

        super(SENSOR_OUTPUT_NAME, parentImuSensor, logger);

        logger.debug("Output created");
    }

    @Override
    protected void defineRecordStructure() {

        // Get an instance of SWE Factory suitable to build components
        GeoPosHelper sweFactory = new GeoPosHelper();

        Time timeStamp = sweFactory.createTime().asSamplingTimeIsoUTC().build();

        Count orientationCovarianceCount = sweFactory.createCount()
                .name("orientationCovarianceCount")
                .label("Orientation Covariance Count")
                .id("orientationCovarianceCount")
                .build();

        Quantity orientationCovarianceData = sweFactory.createQuantity()
                .name("orientationCovarianceData")
                .label("Orientation Covariance Data")
                .definition(SWEHelper.getPropertyUri("orientationCovarianceData"))
                .description("Resulting covariance of orientation quaternion")
                .dataType(DataType.DOUBLE)
                .build();

        DataArray orientationCovarianceDataArray = sweFactory.createArray()
                .name("orientationCovariance")
                .label("Orientation Covariance")
                .definition(SWEHelper.getPropertyUri("orientationCovariance"))
                .description("Computed covariance for orientation quaternion")
                .withVariableSize(orientationCovarianceCount.getId())
                .build();

        orientationCovarianceDataArray.addComponent(orientationCovarianceData.getName(), orientationCovarianceData);

        Count angularVelocityCovarianceCount = sweFactory.createCount()
                .name("angularVelocityCovarianceCount")
                .label("Angular Velocity Covariance Count")
                .id("angularVelocityCovarianceCount")
                .build();

        Quantity angularVelocityCovarianceData = sweFactory.createQuantity()
                .name("angularVelocityCovarianceData")
                .label("Angular Velocity Covariance Data")
                .definition(SWEHelper.getPropertyUri("angularVelocityCovarianceData"))
                .description("Resulting covariance of angular velocity")
                .dataType(DataType.DOUBLE)
                .build();

        DataArray angularVelocityCovarianceDataArray = sweFactory.createArray()
                .name("angularVelocityCovariance")
                .label("Angular Velocity Covariance")
                .definition(SWEHelper.getPropertyUri("angularVelocityCovariance"))
                .description("Computed covariance for angular velocity")
                .withVariableSize(angularVelocityCovarianceCount.getId())
                .build();

        angularVelocityCovarianceDataArray.addComponent(angularVelocityCovarianceData.getName(), angularVelocityCovarianceData);

        Count linearAccelerationCovarianceCount = sweFactory.createCount()
                .name("linearAccelerationCovarianceCount")
                .label("Linear Acceleration Covariance Count")
                .id("linearAccelerationCovarianceCount")
                .build();

        Quantity linearAccelerationCovarianceData = sweFactory.createQuantity()
                .name("linearAccelerationCovarianceData")
                .label("Linear Acceleration Covariance Data")
                .definition(SWEHelper.getPropertyUri("linearAccelerationCovarianceData"))
                .description("Resulting covariance of linear acceleration")
                .dataType(DataType.DOUBLE)
                .build();

        DataArray linearAccelerationCovarianceDataArray = sweFactory.createArray()
                .name("linearAccelerationCovariance")
                .label("Linear Acceleration Covariance")
                .definition(SWEHelper.getPropertyUri("linearAccelerationCovariance"))
                .description("Computed covariance for linear acceleration")
                .withVariableSize(linearAccelerationCovarianceCount.getId())
                .build();

        linearAccelerationCovarianceDataArray.addComponent(linearAccelerationCovarianceData.getName(), linearAccelerationCovarianceData);

        // Create data record description
        dataStruct = sweFactory.createRecord()
                .name(getName())
                .updatable(true)
                .definition(SWEHelper.getPropertyUri(SENSOR_OUTPUT_NAME))
                .label(SENSOR_OUTPUT_LABEL)
                .description(SENSOR_OUTPUT_DESCRIPTION)
                .addField("SampleTime", timeStamp)
                .addField(orientationCovarianceCount.getName(), orientationCovarianceCount)
                .addField(angularVelocityCovarianceCount.getName(), angularVelocityCovarianceCount)
                .addField(linearAccelerationCovarianceCount.getName(), linearAccelerationCovarianceCount)
                .addField("Orientation", sweFactory.createQuatOrientation().build())
                .addField("AngularVelocity", sweFactory.createAngularVelocityVector("rad/s").build())
                .addField("LinearAcceleration", sweFactory.createVelocityVector("m/s"))
                .addField(orientationCovarianceDataArray.getName(), orientationCovarianceDataArray)
                .addField(angularVelocityCovarianceDataArray.getName(), angularVelocityCovarianceDataArray)
                .addField(linearAccelerationCovarianceDataArray.getName(), linearAccelerationCovarianceDataArray)
                .build();

        dataEncoding = sweFactory.newTextEncoding(",", "\n");
    }

    @Override
    public void onNewMessage(Object object) {

        updateSamplingPeriodHistogram();

        Imu imu = (Imu) object;

        DataBlock dataBlock;
        if (latestRecord == null) {

            dataBlock = dataStruct.createDataBlock();

        } else {

            dataBlock = latestRecord.renew();
        }

        // Populate data block
        dataBlock.setDoubleValue(0, System.currentTimeMillis() / 1000.0);

        // Orientation Covariance Count
        dataBlock.setDoubleValue(1, imu.getOrientationCovariance().length);

        // Angular Velocity Covariance Count
        dataBlock.setDoubleValue(2, imu.getAngularVelocityCovariance().length);

        // Linear Acceleration Covariance Count
        dataBlock.setDoubleValue(3, imu.getLinearAccelerationCovariance().length);

        // Orientation
        ((DataBlockMixed) dataBlock).getUnderlyingObject()[4].setDoubleValue(0, imu.getOrientation().getX());
        ((DataBlockMixed) dataBlock).getUnderlyingObject()[4].setDoubleValue(1, imu.getOrientation().getY());
        ((DataBlockMixed) dataBlock).getUnderlyingObject()[4].setDoubleValue(2, imu.getOrientation().getZ());
        ((DataBlockMixed) dataBlock).getUnderlyingObject()[4].setDoubleValue(3, imu.getOrientation().getW());

        // Angular Velocity
        ((DataBlockMixed) dataBlock).getUnderlyingObject()[5].setDoubleValue(0, imu.getAngularVelocity().getX());
        ((DataBlockMixed) dataBlock).getUnderlyingObject()[5].setDoubleValue(1, imu.getAngularVelocity().getY());
        ((DataBlockMixed) dataBlock).getUnderlyingObject()[5].setDoubleValue(2, imu.getAngularVelocity().getZ());

        // Linear Acceleration
        ((DataBlockMixed) dataBlock).getUnderlyingObject()[6].setDoubleValue(0, imu.getLinearAcceleration().getX());
        ((DataBlockMixed) dataBlock).getUnderlyingObject()[6].setDoubleValue(1, imu.getLinearAcceleration().getY());
        ((DataBlockMixed) dataBlock).getUnderlyingObject()[6].setDoubleValue(2, imu.getLinearAcceleration().getZ());

        // Orientation Covariance
        ((DataBlockMixed) dataBlock).getUnderlyingObject()[7].setUnderlyingObject(imu.getOrientationCovariance());

        // Angular Velocity Covariance
        ((DataBlockMixed) dataBlock).getUnderlyingObject()[8].setUnderlyingObject(imu.getAngularVelocityCovariance());

        // Linear Acceleration Covariance
        ((DataBlockMixed) dataBlock).getUnderlyingObject()[9].setUnderlyingObject(imu.getLinearAccelerationCovariance());

        ((DataBlockMixed) dataBlock).updateAtomCount();

        latestRecord = dataBlock;

        latestRecordTime = System.currentTimeMillis();

        eventHandler.publish(new DataEvent(latestRecordTime, ImuOutput.this, dataBlock));
    }

    @Override
    public void doInit() {

        logger.debug("Initializing Output");

        defineRecordStructure();

        nodeMainExecutor = DefaultNodeMainExecutor.newDefault();

        subscriberNode = new RosSubscriberNode(NODE_NAME_STR, TOPIC_STR, Imu._TYPE, this);

        initSamplingTime();

        logger.debug("Initializing Output Complete");
    }

    @Override
    public void doStart() {

        RosMasterConfig config = parentSensor.getConfiguration().rosMaster;

        NodeConfiguration nodeConfiguration = RosUtils.getNodeConfiguration(
                config.localHostIp, NODE_NAME_STR, URI.create(config.uri));

        nodeMainExecutor.execute(subscriberNode, nodeConfiguration);
    }

    @Override
    public void doStop() {

        nodeMainExecutor.shutdownNodeMain(subscriberNode);

        nodeMainExecutor.shutdown();
    }

    @Override
    public boolean isAlive() {

        return !(nodeMainExecutor == null || nodeMainExecutor.getScheduledExecutorService().isShutdown());
    }
}
