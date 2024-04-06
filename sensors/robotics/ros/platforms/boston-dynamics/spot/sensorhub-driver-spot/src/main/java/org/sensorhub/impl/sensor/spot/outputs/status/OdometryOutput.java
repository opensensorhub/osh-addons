/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 - 2024 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.spot.outputs.status;

import geometry_msgs.TwistWithCovarianceStamped;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataType;
import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.ros.config.RosMasterConfig;
import org.sensorhub.impl.ros.nodes.pubsub.RosSubscriberNode;
import org.sensorhub.impl.ros.output.RosSensorOutput;
import org.sensorhub.impl.ros.utils.RosUtils;
import org.sensorhub.impl.sensor.spot.SpotSensor;
import org.sensorhub.impl.sensor.spot.config.SpotStatusConfig;
import org.slf4j.LoggerFactory;
import org.vast.data.DataBlockMixed;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

import java.net.URI;

/**
 * Defines the output of the odometry from the platform and publishes the observations thereof
 *
 * @author Nick Garay
 * @since Sept. 25, 2023
 */
public class OdometryOutput extends RosSensorOutput<SpotSensor> {

    /**
     * Conversion factor
     */
    private static final double NANOS_PER_SEC = 1000000000.;

    /**
     * Name of the output
     */
    private static final String SENSOR_OUTPUT_NAME = "Odometry";

    /**
     * Label for the output
     */
    private static final String SENSOR_OUTPUT_LABEL = "Odometry";

    /**
     * Description of the output
     */
    private static final String SENSOR_OUTPUT_DESCRIPTION = "The estimated odometry of the platform";

    /**
     * ROS Node name assigned at creation
     */
    private static final String NODE_NAME_STR = "/SensorHub/spot/odometry";

    /**
     * The ROS executor process that manages the lifecycle of ROS nodes and services
     */
    private NodeMainExecutor nodeMainExecutor;

    /**
     * ROS Node used to listen for messages on a specific topic
     */
    private RosSubscriberNode subscriberNode;

    /**
     * Constructor
     *
     * @param parentSpotSensor Sensor driver providing this output
     */
    public OdometryOutput(SpotSensor parentSpotSensor) {

        super(SENSOR_OUTPUT_NAME, parentSpotSensor, LoggerFactory.getLogger(BatteryStatusOutput.class));

        getLogger().debug("Output created");
    }

    @Override
    public void doInit() {

        getLogger().debug("Initializing Output");

        defineRecordStructure();

        nodeMainExecutor = DefaultNodeMainExecutor.newDefault();

        SpotStatusConfig.OdometryStatusOutput config =
                getParentProducer().getConfiguration().spotStatusConfig.odometryStatusOutput;

        subscriberNode = new RosSubscriberNode(
                NODE_NAME_STR, config.odometryTopic, TwistWithCovarianceStamped._TYPE, this);

        initSamplingTime();

        getLogger().debug("Initializing Output Complete");
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

    @Override
    protected void defineRecordStructure() {

        // Get an instance of SWE Factory suitable to build components
        GeoPosHelper sweFactory = new GeoPosHelper();

        // Create data record description
        dataStruct = sweFactory.createRecord()
                .name(getName())
                .updatable(true)
                .definition(SWEHelper.getPropertyUri(SENSOR_OUTPUT_NAME))
                .label(SENSOR_OUTPUT_LABEL)
                .description(SENSOR_OUTPUT_DESCRIPTION)
                .addField("SampleTime", sweFactory.createTime().asSamplingTimeIsoUTC().build())
                .addField("LinearVelocity", sweFactory.createVelocityVector("m/s"))
                .addField("AngularVelocity", sweFactory.createAngularVelocityVector("rad/s"))
                .addField("positionCovariance", sweFactory.createArray()
                        .name("positionCovariance")
                        .label("Position Covariance")
                        .definition(SWEHelper.getPropertyUri("PositionCovariance"))
                        .description("Position Covariance [m^2] defined relative to a tangential plane " +
                                "through the reported position. The components are East, North, UP (ENU), " +
                                "in row-major order")
                        .withElement("covariance", sweFactory.createQuantity()
                                .label("Covariance")
                                .definition(SWEHelper.getPropertyUri("Covariance"))
                                .description("Observed covariance")
                                .uom("m^2")
                                .dataType(DataType.DOUBLE))
                        .withFixedSize(9))
                .build();

        dataEncoding = sweFactory.newTextEncoding(",", "\n");
    }

    @Override
    public void onNewMessage(Object object) {

        updateSamplingPeriodHistogram();

        TwistWithCovarianceStamped message = (TwistWithCovarianceStamped) object;

        DataBlock dataBlock;
        if (latestRecord == null) {

            dataBlock = dataStruct.createDataBlock();

        } else {

            dataBlock = latestRecord.renew();
        }


        dataBlock.setDoubleValue(0, message.getHeader().getStamp().totalNsecs() / NANOS_PER_SEC);
        ((DataBlockMixed) dataBlock).getUnderlyingObject()[1].setDoubleValue(0, message.getTwist().getTwist().getLinear().getX());
        ((DataBlockMixed) dataBlock).getUnderlyingObject()[1].setDoubleValue(1, message.getTwist().getTwist().getLinear().getY());
        ((DataBlockMixed) dataBlock).getUnderlyingObject()[1].setDoubleValue(2, message.getTwist().getTwist().getLinear().getZ());
        ((DataBlockMixed) dataBlock).getUnderlyingObject()[2].setDoubleValue(0, message.getTwist().getTwist().getAngular().getX());
        ((DataBlockMixed) dataBlock).getUnderlyingObject()[2].setDoubleValue(1, message.getTwist().getTwist().getAngular().getY());
        ((DataBlockMixed) dataBlock).getUnderlyingObject()[2].setDoubleValue(2, message.getTwist().getTwist().getAngular().getZ());
        ((DataBlockMixed) dataBlock).getUnderlyingObject()[3].setUnderlyingObject(message.getTwist().getCovariance());

        latestRecord = dataBlock;

        latestRecordTime = System.currentTimeMillis();

        eventHandler.publish(new DataEvent(latestRecordTime, OdometryOutput.this, dataBlock));
    }
}