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
package org.sensorhub.impl.sensor.track;

import org.sensorhub.impl.ros.config.RosMasterConfig;
import org.sensorhub.impl.ros.nodes.pubsub.RosSubscriberNode;
import org.sensorhub.impl.ros.output.RosSensorOutput;
import org.sensorhub.impl.ros.utils.RosUtils;
import geometry_msgs.Twist;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.Time;
import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.sensorhub.api.data.DataEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.DataBlockMixed;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

import java.net.URI;

/**
 * Output specification and provider for ...
 *
 * @author Nick Garay
 * @since Feb. 2, 2023
 */
public class TrackControlOutput extends RosSensorOutput<TrackControlSensor> {

    private static final String SENSOR_OUTPUT_NAME = "CommandVel";
    private static final String SENSOR_OUTPUT_LABEL = "CommandVel";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "Resulting command velocity of remote control operation";

    private static final String NODE_NAME_STR = "/SensorHub/trackControl/commandVel";

    private static final String TOPIC_STR = "/cmd_vel";

    private static final Logger logger = LoggerFactory.getLogger(TrackControlOutput.class);

    private NodeMainExecutor nodeMainExecutor;

    private RosSubscriberNode subscriberNode;

    /**
     * Constructor
     *
     * @param trackControlSensor Sensor driver providing this output
     */
    TrackControlOutput(TrackControlSensor trackControlSensor) {

        super(SENSOR_OUTPUT_NAME, trackControlSensor, logger);

        logger.debug("Output created");
    }

    @Override
    protected void defineRecordStructure() {

        // Get an instance of SWE Factory suitable to build components
        GeoPosHelper sweFactory = new GeoPosHelper();

        Time timeStamp = sweFactory.createTime().asSamplingTimeIsoUTC().build();

        // Create data record description
        dataStruct = sweFactory.createRecord()
                .name(getName())
                .updatable(true)
                .definition(SWEHelper.getPropertyUri(SENSOR_OUTPUT_LABEL))
                .label(SENSOR_OUTPUT_LABEL)
                .description(SENSOR_OUTPUT_DESCRIPTION)
                .addField("SampleTime", timeStamp)
                .addField("AngularVelocity", sweFactory.createAngularVelocityVector("rad/s").build())
                .addField("LinearVelocity", sweFactory.createVelocityVector("m/s"))
                .build();

        dataEncoding = sweFactory.newTextEncoding(",", "\n");
    }

    @Override
    public void onNewMessage(Object object) {

        updateSamplingPeriodHistogram();

        Twist twist = (Twist) object;

        DataBlock dataBlock;
        if (latestRecord == null) {

            dataBlock = dataStruct.createDataBlock();

        } else {

            dataBlock = latestRecord.renew();
        }

        // Populate data block
        dataBlock.setDoubleValue(0, System.currentTimeMillis() / 1000.0);

        // Angular Velocity
        ((DataBlockMixed) dataBlock).getUnderlyingObject()[1].setDoubleValue(0, twist.getAngular().getX());
        ((DataBlockMixed) dataBlock).getUnderlyingObject()[1].setDoubleValue(1, twist.getAngular().getY());
        ((DataBlockMixed) dataBlock).getUnderlyingObject()[1].setDoubleValue(2, twist.getAngular().getZ());

        // Linear Acceleration
        ((DataBlockMixed) dataBlock).getUnderlyingObject()[2].setDoubleValue(0, twist.getLinear().getX());
        ((DataBlockMixed) dataBlock).getUnderlyingObject()[2].setDoubleValue(1, twist.getLinear().getY());
        ((DataBlockMixed) dataBlock).getUnderlyingObject()[2].setDoubleValue(2, twist.getLinear().getZ());

        latestRecord = dataBlock;

        latestRecordTime = System.currentTimeMillis();

        eventHandler.publish(new DataEvent(latestRecordTime, TrackControlOutput.this, dataBlock));
    }

    @Override
    public void doInit() {

        logger.debug("Initializing Output");

        defineRecordStructure();

        nodeMainExecutor = DefaultNodeMainExecutor.newDefault();

        subscriberNode = new RosSubscriberNode(NODE_NAME_STR, TOPIC_STR, Twist._TYPE, this);

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
