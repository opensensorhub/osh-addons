/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 - 2024 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.spot.outputs.cameras;

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
import org.sensorhub.impl.sensor.spot.config.SpotFrameResConfig;
import org.slf4j.LoggerFactory;
import org.vast.data.DataBlockMixed;
import org.vast.data.TextEncodingImpl;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.VectorHelper;
import sensor_msgs.Image;

import java.net.URI;

/**
 * Defines the output of the depth sensors from the platform and publishes the observations thereof
 * 
 * @author Nick Garay
 * @since Sept. 25, 2023
 */
public class DepthOutput extends RosSensorOutput<SpotSensor> {

    /**
     * Name of the output
     */
    private static final String SENSOR_OUTPUT_NAME = "depthOutput";

    /**
     * Label for the output
     */
    private static final String SENSOR_OUTPUT_LABEL = "Depth Output";

    /**
     * Description of the output
     */
    private static final String SENSOR_OUTPUT_DESCRIPTION = "Point cloud representing depth sensor data";

    /**
     * ROS Node name assigned at creation
     */
    private static final String NODE_NAME_STR = "/SensorHub/spot/";

    /**
     * The ROS executor process that manages the lifecycle of ROS nodes and services
     */
    private NodeMainExecutor nodeMainExecutor;

    /**
     * ROS Node used to listen for messages on a specific topic
     */
    private RosSubscriberNode subscriberNode;

    /**
     * The selected subscription topic
     */
    private final String topic;

    /**
     * The position of the sensor on the platform, used to identify which sensor is producing the observations
     */
    private final SensorPosition sensorPosition;

    /**
     * Configuration parameters for the resolution (width x height) of the sensor observations
     */
    private final SpotFrameResConfig resConfig;

    /**
     * Constructor
     *
     * @param parentSensor   Parent sensor owning this output definition
     * @param sensorPosition The image sensor position
     * @param topic          The topic to register on for image data
     * @param resConfig      Resolution configuration for the sensor frames
     */
    public DepthOutput(SpotSensor parentSensor, SensorPosition sensorPosition, String topic, SpotFrameResConfig resConfig) {

        super(SENSOR_OUTPUT_NAME + "_" + sensorPosition.name(), parentSensor, LoggerFactory.getLogger(ImageOutput.class));
        this.sensorPosition = sensorPosition;
        this.topic = topic;
        this.resConfig = resConfig;

        getLogger().debug("Output created");
    }

    @Override
    public void doInit() {

        getLogger().debug("Initializing Output");

        defineRecordStructure();

        nodeMainExecutor = DefaultNodeMainExecutor.newDefault();

        subscriberNode = new RosSubscriberNode(
                NODE_NAME_STR + sensorPosition.name(),
                topic, Image._TYPE, this);

        initSamplingTime();

        getLogger().debug("Initializing Output Complete");
    }

    @Override
    public void doStart() {

        RosMasterConfig config = parentSensor.getConfiguration().rosMaster;

        NodeConfiguration nodeConfiguration = RosUtils.getNodeConfiguration(
                config.localHostIp, NODE_NAME_STR + sensorPosition.name(), URI.create(config.uri));

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

        VectorHelper factory = new VectorHelper();

        dataStruct = factory.createRecord()
                .name(SENSOR_OUTPUT_NAME + "_" + sensorPosition.name())
                .label(SENSOR_OUTPUT_LABEL + "_" + sensorPosition.name())
                .description(SENSOR_OUTPUT_DESCRIPTION)
                .definition(SWEHelper.getPropertyUri("depth_output"))
                .addField("sampleTime", factory.createTime().asSamplingTimeIsoUTC())
                .addField("numPoints", factory.createCount()
                        .label("Num Points")
                        .description("Number of points in the depth point cloud")
                        .definition(SWEHelper.getPropertyUri("num_points"))
                        .id("NUM_POINTS"))
                .addField("depthData", factory.createArray()
                        .label("Point Cloud Model")
                        .description("Point Cloud Data read from Depth Sensor")
                        .definition(SWEHelper.getPropertyUri("depth_point_cloud"))
                        .withElement("point", factory.createQuantity()
                                .label("Distance")
                                .description("A measure of distance from the sensor, the raw point cloud data retrieved from depth sensor.")
                                .definition(SWEHelper.getPropertyUri("distance"))
                                .dataType(DataType.BYTE)
                                .uom("m"))
                        .withVariableSize("NUM_POINTS"))
                .build();

        dataEncoding = new TextEncodingImpl();
    }

    @Override
    public void onNewMessage(Object object) {

        updateSamplingPeriodHistogram();

        Image image = (Image) object;

        DataBlock dataBlock;

        if (latestRecord == null) {

            dataBlock = dataStruct.createDataBlock();

        } else {

            dataBlock = latestRecord.renew();
        }

        // Populate data block
        dataBlock.setDoubleValue(0, System.currentTimeMillis() / 1000.0);
        dataBlock.setIntValue(1, resConfig.height * resConfig.height);
        ((DataBlockMixed) dataBlock).getUnderlyingObject()[2].setUnderlyingObject(image.getData().array());

        // update the latest record and send event
        latestRecord = dataBlock;

        latestRecordTime = System.currentTimeMillis();

        eventHandler.publish(new DataEvent(latestRecordTime, DepthOutput.this, dataBlock));
    }
}
