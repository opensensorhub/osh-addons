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
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;
import spot_msgs.MobilityParams;

import java.net.URI;

/**
 * Defines the output of the mobility parameters from the platform and publishes the observations thereof
 *
 * @author Nick Garay
 * @since Sept. 25, 2023
 */
public class MobilityParametersOutput extends RosSensorOutput<SpotSensor> {

    /**
     * Reported modes for grated surfaces parameter
     */
    private static final String[] GRATED_SURFACES_MODE = {"UNKNOWN", "OFF", "ON", "AUTO"};

    /**
     * Name of the output
     */
    private static final String SENSOR_OUTPUT_NAME = "MobilityParameters";

    /**
     * Label for the output
     */
    private static final String SENSOR_OUTPUT_LABEL = "Mobility Parameters";

    /**
     * Description of the output
     */
    private static final String SENSOR_OUTPUT_DESCRIPTION = "Describes the current state of the mobility parameters defining the motion behaviour of the robot";

    /**
     * ROS Node name assigned at creation
     */
    private static final String NODE_NAME_STR = "/SensorHub/spot/mobility_parameters";

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
    public MobilityParametersOutput(SpotSensor parentSpotSensor) {

        super(SENSOR_OUTPUT_NAME, parentSpotSensor, LoggerFactory.getLogger(BatteryStatusOutput.class));

        getLogger().debug("Output created");
    }

    @Override
    public void doInit() {

        getLogger().debug("Initializing Output");

        defineRecordStructure();

        nodeMainExecutor = DefaultNodeMainExecutor.newDefault();

        SpotStatusConfig.MobilityParamsStatusOutput config =
                getParentProducer().getConfiguration().spotStatusConfig.mobilityParamsStatusOutput;

        subscriberNode = new RosSubscriberNode(
                NODE_NAME_STR, config.mobilityParamsTopic, MobilityParams._TYPE, this);

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
                .addField("pose", sweFactory.createRecord()
                        .label("Pose")
                        .description("The platforms pose - location and orientation")
                        .definition(SWEHelper.getPropertyUri("pose"))
                        .addField("position", sweFactory.createLocationVectorXYZ("m"))
                        .addField("orientation", sweFactory.createQuatOrientation()))
                .addField("locomotionHint", sweFactory.createCount()
                        .label("Locomotion Hint")
                        .description("Location hint")
                        .definition(SWEHelper.getPropertyUri("location_hint"))
                        .dataType(DataType.INT))
                .addField("obstacleParams", sweFactory.createRecord()
                        .label("Obstacle Params")
                        .description("Parameters for obstacle avoidance")
                        .definition(SWEHelper.getPropertyUri("obstacle_params"))
                        .addField("disableVisionBodyObstacleAvoidance", sweFactory.createBoolean()
                                .label("Vision Body Obstacle Avoidance")
                                .description("Indicates if vision body obstacle avoidance is disabled")
                                .definition(SWEHelper.getPropertyUri("vision_body_obstacle_avoidance")))
                        .addField("disableVisionFootObstacleAvoidance", sweFactory.createBoolean()
                                .label("Vision Foot Obstacle Avoidance")
                                .description("Indicates if vision foot obstacle avoidance is disabled")
                                .definition(SWEHelper.getPropertyUri("vision_foot_obstacle_avoidance")))
                        .addField("obstacleAvoidancePadding", sweFactory.createQuantity()
                                .label("Obstacle Avoidance Padding")
                                .description("Obstacle avoidance padding in meters, the extra keep away distance for obstacle")
                                .definition(SWEHelper.getPropertyUri("obstacle_avoidance_padding"))
                                .uom("m")
                                .dataType(DataType.DOUBLE))
                        .addField("disableVisionNegativeObstacles", sweFactory.createBoolean()
                                .label("Vision Negative Obstacles")
                                .description("Indicates if vision negative obstacles is disabled")
                                .definition(SWEHelper.getPropertyUri("vision_negative_obstacles")))
                        .addField("disableVisionFootConstraintAvoidance", sweFactory.createBoolean()
                                .label("Vision Foot Constraint Avoidance")
                                .description("Indicates if vision foot constraint avoidance is disabled")
                                .definition(SWEHelper.getPropertyUri("vision_foot_constraint_avoidance")))
                        .addField("disableVisionFootObstacleBodyAssist", sweFactory.createBoolean()
                                .label("vision foot obstacle body assist")
                                .description("Indicates if vision foot obstacle body assist is disabled")
                                .definition(SWEHelper.getPropertyUri("vision_foot_obstacle_body_assist"))))
                .addField("stairHint", sweFactory.createBoolean()
                        .label("Stair Hint")
                        .description("Hint if walking on stairs")
                        .definition(SWEHelper.getPropertyUri("stair_hint")))
                .addField("terrainParams", sweFactory.createRecord()
                        .label("Terrain Params")
                        .description("Terrain parameters")
                        .definition(SWEHelper.getPropertyUri("terrain_params"))
                        .addField("gratedSurfacesMode", sweFactory.createCategory()
                                .label("Grated Surfaces Mode")
                                .description("Indicates mode for grated surfaces")
                                .definition(SWEHelper.getPropertyUri("grated_surfaces_mode"))
                                .addAllowedValues(GRATED_SURFACES_MODE))
                        .addField("muHint", sweFactory.createQuantity()
                                .label("Mu Hint")
                                .description("Hint of terrain coefficient of friction.")
                                .definition(SWEHelper.getPropertyUri("mu_estimate"))
                                .dataType(DataType.DOUBLE)))
                .addField("velocityParams", sweFactory.createRecord()
                        .label("Velocity")
                        .description("Parameters describing current velocity of the platform")
                        .definition(SWEHelper.getPropertyUri("velocity_params"))
                        .addField("linear", sweFactory.createVelocityVector("m/s"))
                        .addField("angular", sweFactory.createAngularVelocityVector("rad/s")))
                .addField("swingHeight", sweFactory.createQuantity()
                        .label("Swing Height")
                        .description("Height of swing while in motion")
                        .definition(SWEHelper.getPropertyUri("swing_height"))
                        .uom("m")
                        .dataType(DataType.INT))
                .build();

        dataEncoding = sweFactory.newTextEncoding(",", "\n");
    }

    @Override
    public void onNewMessage(Object object) {

        updateSamplingPeriodHistogram();

        MobilityParams message = (MobilityParams) object;

        DataBlock dataBlock;
        if (latestRecord == null) {

            dataBlock = dataStruct.createDataBlock();

        } else {

            dataBlock = latestRecord.renew();
        }

        dataBlock.setDoubleValue(0, System.currentTimeMillis() / 1000.);
        dataBlock.setDoubleValue(1, message.getBodyControl().getPosition().getX());
        dataBlock.setDoubleValue(2, message.getBodyControl().getPosition().getY());
        dataBlock.setDoubleValue(3, message.getBodyControl().getPosition().getZ());
        dataBlock.setDoubleValue(4, message.getBodyControl().getOrientation().getX());
        dataBlock.setDoubleValue(5, message.getBodyControl().getOrientation().getY());
        dataBlock.setDoubleValue(6, message.getBodyControl().getOrientation().getZ());
        dataBlock.setDoubleValue(7, message.getBodyControl().getOrientation().getW());
        dataBlock.setIntValue(8, message.getLocomotionHint());
        dataBlock.setBooleanValue(9, message.getObstacleParams().getDisableVisionBodyObstacleAvoidance());
        dataBlock.setBooleanValue(10, message.getObstacleParams().getDisableVisionFootObstacleAvoidance());
        dataBlock.setDoubleValue(11, message.getObstacleParams().getObstacleAvoidancePadding());
        dataBlock.setBooleanValue(12, message.getObstacleParams().getDisableVisionNegativeObstacles());
        dataBlock.setBooleanValue(13, message.getObstacleParams().getDisableVisionFootConstraintAvoidance());
        dataBlock.setBooleanValue(14, message.getObstacleParams().getDisableVisionFootObstacleBodyAssist());
        dataBlock.setBooleanValue(15, message.getStairHint());
        dataBlock.setStringValue(16, GRATED_SURFACES_MODE[message.getTerrainParams().getGratedSurfacesMode()]);
        dataBlock.setDoubleValue(17, message.getTerrainParams().getGroundMuHint());
        dataBlock.setDoubleValue(18, message.getVelocityLimit().getLinear().getX());
        dataBlock.setDoubleValue(19, message.getVelocityLimit().getLinear().getY());
        dataBlock.setDoubleValue(20, message.getVelocityLimit().getLinear().getZ());
        dataBlock.setDoubleValue(21, message.getVelocityLimit().getAngular().getX());
        dataBlock.setDoubleValue(22, message.getVelocityLimit().getAngular().getY());
        dataBlock.setDoubleValue(23, message.getVelocityLimit().getAngular().getZ());
        dataBlock.setIntValue(24, message.getSwingHeight());

        latestRecord = dataBlock;

        latestRecordTime = System.currentTimeMillis();

        eventHandler.publish(new DataEvent(latestRecordTime, MobilityParametersOutput.this, dataBlock));
    }
}