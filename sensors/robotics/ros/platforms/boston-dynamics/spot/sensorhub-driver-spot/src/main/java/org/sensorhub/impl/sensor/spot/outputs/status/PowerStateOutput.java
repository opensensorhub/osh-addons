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
import net.opengis.swe.v20.Time;
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
import spot_msgs.PowerState;

import java.net.URI;

/**
 * Defines the output of the power state status from the platform and publishes the observations thereof
 *
 * @author Nick Garay
 * @since Sept. 25, 2023
 */
public class PowerStateOutput extends RosSensorOutput<SpotSensor> {

    /**
     * Conversion factor
     */
    private static final double NANOS_PER_SEC = 1000000000.;

    /**
     * Possible motor power state values reported
     */
    private static final String[] MOTOR_POWER_STATE = {"STATE_UNKNOWN", "STATE_OFF", "STATE_ON",
            "STATE_POWERING_ON", "STATE_POWERING_OFF", "STATE_ERROR"};

    /**
     * Shore power state values reported
     */
    private static final String[] SHORE_POWER_STATE = {"STATE_UNKNOWN_SHORE_POWER", "STATE_ON_SHORE_POWER",
            "STATE_OFF_SHORE_POWER"};

    /**
     * Name of the output
     */
    private static final String SENSOR_OUTPUT_NAME = "PowerState";

    /**
     * Label for the output
     */
    private static final String SENSOR_OUTPUT_LABEL = "PowerState";

    /**
     * Description of the output
     */
    private static final String SENSOR_OUTPUT_DESCRIPTION = "General power information";

    /**
     * ROS Node name assigned at creation
     */
    private static final String NODE_NAME_STR = "/SensorHub/spot/power_state";

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
    public PowerStateOutput(SpotSensor parentSpotSensor) {

        super(SENSOR_OUTPUT_NAME, parentSpotSensor, LoggerFactory.getLogger(BatteryStatusOutput.class));

        getLogger().debug("Output created");
    }

    @Override
    public void doInit() {

        getLogger().debug("Initializing Output");

        defineRecordStructure();

        nodeMainExecutor = DefaultNodeMainExecutor.newDefault();

        SpotStatusConfig.PowerStateStatusOutput config =
                getParentProducer().getConfiguration().spotStatusConfig.powerStateStatusOutput;

        subscriberNode = new RosSubscriberNode(
                NODE_NAME_STR, config.powerStateTopic, PowerState._TYPE, this);

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

        Time timeStamp = sweFactory.createTime().asSamplingTimeIsoUTC().build();

        // Create data record description
        dataStruct = sweFactory.createRecord()
                .name(getName())
                .updatable(true)
                .definition(SWEHelper.getPropertyUri(SENSOR_OUTPUT_NAME))
                .label(SENSOR_OUTPUT_LABEL)
                .description(SENSOR_OUTPUT_DESCRIPTION)
                .addField("SampleTime", timeStamp)
                .addField("motorPowerState", sweFactory.createCategory()
                        .label("Motor Power State")
                        .description("State of power applied to motors")
                        .definition(SWEHelper.getPropertyUri("motor_power_state"))
                        .addAllowedValues(MOTOR_POWER_STATE))
                .addField("shorePowerState", sweFactory.createCategory()
                        .label("Shore Power State")
                        .description("State of power applied while docked")
                        .definition(SWEHelper.getPropertyUri("shore_power_state"))
                        .addAllowedValues(SHORE_POWER_STATE))
                .addField("locomotionChargePercentage", sweFactory.createQuantity()
                        .label("Locomotion Charge Percentage")
                        .description("Number from 0 (empty) to 100 (full) indicating the estimated state of charge")
                        .definition(SWEHelper.getPropertyUri("charge_percentage"))
                        .dataType(DataType.DOUBLE)
                        .uom("%"))
                .addField("locomotionEstimatedRuntime", sweFactory.createQuantity()
                        .label("Locomotion Estimated Runtime")
                        .description("An estimate of remaining runtime")
                        .definition(SWEHelper.getPropertyUri("estimated_runtime"))
                        .dataType(DataType.DOUBLE)
                        .uom("s"))
                .build();

        dataEncoding = sweFactory.newTextEncoding(",", "\n");
    }

    @Override
    public void onNewMessage(Object object) {

        updateSamplingPeriodHistogram();

        PowerState message = (PowerState) object;

        DataBlock dataBlock;
        if (latestRecord == null) {

            dataBlock = dataStruct.createDataBlock();

        } else {

            dataBlock = latestRecord.renew();
        }

        dataBlock.setDoubleValue(0, System.currentTimeMillis() / 1000.);
        dataBlock.setStringValue(1, MOTOR_POWER_STATE[message.getMotorPowerState()]);
        dataBlock.setStringValue(2, SHORE_POWER_STATE[message.getShorePowerState()]);
        dataBlock.setDoubleValue(3, message.getLocomotionChargePercentage());
        dataBlock.setDoubleValue(4, message.getLocomotionEstimatedRuntime().totalNsecs() / NANOS_PER_SEC);

        latestRecord = dataBlock;

        latestRecordTime = System.currentTimeMillis();

        eventHandler.publish(new DataEvent(latestRecordTime, PowerStateOutput.this, dataBlock));
    }
}