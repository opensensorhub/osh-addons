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
import org.vast.data.DataArrayImpl;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;
import spot_msgs.FootState;
import spot_msgs.FootStateArray;
import spot_msgs.TerrainState;

import java.net.URI;
import java.util.List;

/**
 * Defines the output of the feet position from the platform and publishes the observations thereof
 *
 * @author Nick Garay
 * @since Sept. 25, 2023
 */
public class FeetPositionOutput extends RosSensorOutput<SpotSensor> {

    /**
     * Possible contact states for each foot with terrain
     */
    private static final String[] CONTACT_STATES = {"CONTACT_UNKNOWN", "CONTACT_MADE", "CONTACT_LOST"};

    /**
     * Name of the output
     */
    private static final String SENSOR_OUTPUT_NAME = "FeetPosition";

    /**
     * Label for the output
     */
    private static final String SENSOR_OUTPUT_LABEL = "Feet Position";

    /**
     * Description of the output
     */
    private static final String SENSOR_OUTPUT_DESCRIPTION = "The status and position of each foot";

    /**
     * ROS Node name assigned at creation
     */
    private static final String NODE_NAME_STR = "/SensorHub/spot/feet_position";

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
    public FeetPositionOutput(SpotSensor parentSpotSensor) {

        super(SENSOR_OUTPUT_NAME, parentSpotSensor, LoggerFactory.getLogger(BatteryStatusOutput.class));

        getLogger().debug("Output created");
    }

    @Override
    public void doInit() {

        getLogger().debug("Initializing Output");

        defineRecordStructure();

        nodeMainExecutor = DefaultNodeMainExecutor.newDefault();

        SpotStatusConfig.FeetPositionStatusOutput config =
                getParentProducer().getConfiguration().spotStatusConfig.feetPositionStatusOutput;

        subscriberNode = new RosSubscriberNode(
                NODE_NAME_STR, config.feetPositionTopic, FootStateArray._TYPE, this);

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
                .addField("stateCount", sweFactory.createCount()
                        .label("State Count")
                        .description("Number of reported current states")
                        .definition(SWEHelper.getPropertyUri("state_count"))
                        .id("stateCount"))
                .addField("stateArray", sweFactory.createArray()
                        .label("State Array")
                        .description("List of states reported")
                        .definition(SWEHelper.getPropertyUri("state_array"))
                        .withVariableSize("stateCount")
                        .withElement("stateRecord", sweFactory.createRecord()
                                .label("State Record")
                                .description("State parameters reported by the platform")
                                .definition(SWEHelper.getPropertyUri("state_record"))
                                .addField("contact", sweFactory.createText()
                                        .label("Contact")
                                        .description("Description of type of contact for the particular foot state")
                                        .definition(SWEHelper.getPropertyUri("contact")))
                                .addField("footPosition", sweFactory.createLocationVectorXYZ("m"))
                                .addField("terrainState", sweFactory.createRecord()
                                        .addField("frameName", sweFactory.createText()
                                                .label("Frame Name")
                                                .description("Reference frame name for vector data.")
                                                .definition(SWEHelper.getPropertyUri("frame_name")))
                                        .addField("groundMuEstimate", sweFactory.createQuantity()
                                                .label("Ground Mu Estimate")
                                                .description("Estimated ground coefficient of friction for this foot.")
                                                .definition(SWEHelper.getPropertyUri("ground_mu_estimate"))
                                                .dataType(DataType.DOUBLE))
                                        .addField("visualSurfaceGroundPenetrationMean", sweFactory.createQuantity()
                                                .label("Visual Surface Ground Penetration Mean")
                                                .description("Mean penetration of the foot below the ground visual\n" +
                                                        "surface. For penetrable terrains (gravel/sand/grass etc.) these values are\n" +
                                                        "positive. Negative values would indicate potential odometry issues.")
                                                .definition(SWEHelper.getPropertyUri("visual_surface_ground_penetration_mean"))
                                                .uom("m"))
                                        .addField("visualSurfaceGroundPenetrationStd", sweFactory.createQuantity()
                                                .label("Visual Surface Ground Penetration Std Dev")
                                                .description("Standard deviation of the visual surface ground penetration.")
                                                .definition(SWEHelper.getPropertyUri("visual_surface_ground_penetration_std")))
                                        .addField("footSlipDistanceRtFrame", sweFactory.createLocationVectorXYZ("m"))
                                        .addField("footSlipVelocityRtFrame", sweFactory.createVelocityVector("m/s"))
                                        .addField("groundContactNormalRtFrame", sweFactory.createLocationVectorXYZ("m"))
                                )
                        ))
                .build();

        dataEncoding = sweFactory.newTextEncoding(",", "\n");
    }

    @Override
    public void onNewMessage(Object object) {

        updateSamplingPeriodHistogram();

        FootStateArray message = (FootStateArray) object;

        // Need to recreate the data record structure or createDataBlock will fail the second and subsequent times.
        defineRecordStructure();
        DataBlock dataBlock = dataStruct.createDataBlock();
        // Pair the data record with the data block.
        // This is necessary for updating the array size after setting the array count.
        dataStruct.setData(dataBlock);

        List<FootState> states = message.getStates();

        int index = 0;

        dataBlock.setDoubleValue(index++, System.currentTimeMillis() / 1000.);
        // Set the array count.
        dataBlock.setIntValue(index++, states.size());

        // Update the array size. This can only be done after setting the array count.
        var stateArray = ((DataArrayImpl) dataStruct.getComponent("stateArray"));
        stateArray.updateSize();
        dataBlock.updateAtomCount();

        for (FootState currentState : states) {

            // Set each element for the underlying container
            dataBlock.setStringValue(index++, CONTACT_STATES[currentState.getContact()]);
            dataBlock.setDoubleValue(index++, currentState.getFootPositionRtBody().getX());
            dataBlock.setDoubleValue(index++, currentState.getFootPositionRtBody().getY());
            dataBlock.setDoubleValue(index++, currentState.getFootPositionRtBody().getZ());

            TerrainState terrainState = currentState.getTerrain();

            dataBlock.setStringValue(index++, terrainState.getFrameName());
            dataBlock.setDoubleValue(index++, terrainState.getGroundMuEst());
            dataBlock.setDoubleValue(index++, terrainState.getVisualSurfaceGroundPenetrationMean());
            dataBlock.setDoubleValue(index++, terrainState.getVisualSurfaceGroundPenetrationStd());

            dataBlock.setDoubleValue(index++, terrainState.getFootSlipDistanceRtFrame().getX());
            dataBlock.setDoubleValue(index++, terrainState.getFootSlipDistanceRtFrame().getY());
            dataBlock.setDoubleValue(index++, terrainState.getFootSlipDistanceRtFrame().getZ());

            dataBlock.setDoubleValue(index++, terrainState.getFootSlipVelocityRtFrame().getX());
            dataBlock.setDoubleValue(index++, terrainState.getFootSlipVelocityRtFrame().getY());
            dataBlock.setDoubleValue(index++, terrainState.getFootSlipVelocityRtFrame().getZ());

            dataBlock.setDoubleValue(index++, terrainState.getGroundContactNormalRtFrame().getX());
            dataBlock.setDoubleValue(index++, terrainState.getGroundContactNormalRtFrame().getY());
            dataBlock.setDoubleValue(index++, terrainState.getGroundContactNormalRtFrame().getZ());
        }

        latestRecord = dataBlock;

        latestRecordTime = System.currentTimeMillis();

        eventHandler.publish(new DataEvent(latestRecordTime, FeetPositionOutput.this, dataBlock));
    }
}