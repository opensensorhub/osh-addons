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
import spot_msgs.Feedback;

import java.net.URI;

/**
 * Defines the output of the feedback from the platform and publishes the observations thereof
 *
 * @author Nick Garay
 * @since Sept. 25, 2023
 */
public class FeedbackOutput extends RosSensorOutput<SpotSensor> {

    /**
     * Name of the output
     */
    private static final String SENSOR_OUTPUT_NAME = "Feedback";

    /**
     * Label for the output
     */
    private static final String SENSOR_OUTPUT_LABEL = "Feedback";

    /**
     * Description of the output
     */
    private static final String SENSOR_OUTPUT_DESCRIPTION = "Feedback from the Spot robot";

    /**
     * ROS Node name assigned at creation
     */
    private static final String NODE_NAME_STR = "/SensorHub/spot/feedback";

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
    public FeedbackOutput(SpotSensor parentSpotSensor) {

        super(SENSOR_OUTPUT_NAME, parentSpotSensor, LoggerFactory.getLogger(BatteryStatusOutput.class));

        getLogger().debug("Output created");
    }

    @Override
    public void doInit() {

        getLogger().debug("Initializing Output");

        defineRecordStructure();

        nodeMainExecutor = DefaultNodeMainExecutor.newDefault();

        SpotStatusConfig.FeedbackStatusOutput config =
                getParentProducer().getConfiguration().spotStatusConfig.feedbackStatusOutput;

        subscriberNode = new RosSubscriberNode(
                NODE_NAME_STR, config.feedbackTopic, Feedback._TYPE, this);

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
        SWEHelper sweFactory = new SWEHelper();

        // Create data record description
        dataStruct = sweFactory.createRecord()
                .name(getName())
                .updatable(true)
                .definition(SWEHelper.getPropertyUri(SENSOR_OUTPUT_NAME))
                .label(SENSOR_OUTPUT_LABEL)
                .description(SENSOR_OUTPUT_DESCRIPTION)
                .addField("SampleTime", sweFactory.createTime().asSamplingTimeIsoUTC().build())
                .addField("standing", sweFactory.createBoolean()
                        .label("Standing")
                        .description("Reports if platform is currently standing")
                        .definition(SWEHelper.getPropertyUri("standing")))
                .addField("sitting", sweFactory.createBoolean()
                        .label("Sitting")
                        .description("Reports if platform is currently sitting")
                        .definition(SWEHelper.getPropertyUri("sitting")))
                .addField("moving", sweFactory.createBoolean()
                        .label("Moving")
                        .description("Reports if platform is currently moving")
                        .definition(SWEHelper.getPropertyUri("moving")))
                .addField("serialNumber", sweFactory.createText()
                        .label("Serial #")
                        .description("The serial number reported by the platform")
                        .definition(SWEHelper.getPropertyUri("serial_number")))
                .addField("species", sweFactory.createText()
                        .label("Species")
                        .description("The type of platform")
                        .definition(SWEHelper.getPropertyUri("species")))
                .addField("version", sweFactory.createText()
                        .label("Version")
                        .description("Version reported by the platform")
                        .definition(SWEHelper.getPropertyUri("version")))
                .addField("nickname", sweFactory.createText()
                        .label("Nickname")
                        .description("The assigned user friendly name for the platform")
                        .definition(SWEHelper.getPropertyUri("nickname")))
                .addField("computerSerialNumber", sweFactory.createText()
                        .label("Computer SN")
                        .description("The serial number of the onboard computer")
                        .definition(SWEHelper.getPropertyUri("computer_serial_number")))
                .build();

        dataEncoding = sweFactory.newTextEncoding(",", "\n");
    }

    @Override
    public void onNewMessage(Object object) {

        updateSamplingPeriodHistogram();

        Feedback message = (Feedback) object;

        DataBlock dataBlock;
        if (latestRecord == null) {

            dataBlock = dataStruct.createDataBlock();

        } else {

            dataBlock = latestRecord.renew();
        }

        dataBlock.setDoubleValue(0, System.currentTimeMillis() / 1000.);
        dataBlock.setBooleanValue(1, message.getStanding());
        dataBlock.setBooleanValue(2, message.getSitting());
        dataBlock.setBooleanValue(3, message.getMoving());
        dataBlock.setStringValue(4, message.getSerialNumber());
        dataBlock.setStringValue(5, message.getSpecies());
        dataBlock.setStringValue(6, message.getVersion());
        dataBlock.setStringValue(7, message.getNickname());
        dataBlock.setStringValue(8, message.getComputerSerialNumber());

        latestRecord = dataBlock;

        latestRecordTime = System.currentTimeMillis();

        eventHandler.publish(new DataEvent(latestRecordTime, FeedbackOutput.this, dataBlock));
    }
}