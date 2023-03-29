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
package org.sensorhub.impl.sensor.transbot.battery;

import org.sensorhub.impl.ros.config.RosMasterConfig;
import org.sensorhub.impl.ros.nodes.pubsub.RosSubscriberNode;
import org.sensorhub.impl.ros.output.RosSensorOutput;
import org.sensorhub.impl.ros.utils.RosUtils;
import net.opengis.swe.v20.DataBlock;
import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.sensorhub.api.data.DataEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.SWEHelper;
import transbot_msgs.Battery;

import java.net.URI;

/**
 * Output specification and provider for ...
 *
 * @author Nick Garay
 * @since Feb. 2, 2023
 */
public class BatteryOutput extends RosSensorOutput<BatterySensor> {

    private static final String SENSOR_OUTPUT_NAME = "BatteryVoltage";
    private static final String SENSOR_OUTPUT_LABEL = "Battery Sensor";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "Current voltage of battery pack";

    private static final String VOLTAGE_STR = "Voltage";

    private static final String NODE_NAME_STR = "/SensorHub/transbot/battery";

    private static final String TOPIC_STR = "/voltage";

    private static final Logger logger = LoggerFactory.getLogger(BatteryOutput.class);

    private NodeMainExecutor nodeMainExecutor;

    private RosSubscriberNode subscriberNode;

    /**
     * Constructor
     *
     * @param parentBatterySensor Sensor driver providing this output
     */
    BatteryOutput(BatterySensor parentBatterySensor) {

        super(SENSOR_OUTPUT_NAME, parentBatterySensor, logger);

        logger.debug("Output created");
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
                .addField("SampleTime",
                        sweFactory.createTime()
                                .asSamplingTimeIsoUTC()
                                .build())
                .addField(VOLTAGE_STR,
                        sweFactory.createQuantity()
                                .name(VOLTAGE_STR)
                                .label(VOLTAGE_STR)
                                .definition("http://qudt.org/vocab/quantitykind/Voltage")
                                .description("Current reported battery pack voltage")
                                .uom("V")
                                .build())
                .build();

        dataEncoding = sweFactory.newTextEncoding(",", "\n");
    }

    @Override
    public void onNewMessage(Object object) {

        updateSamplingPeriodHistogram();

        Battery batteryMessage = (Battery) object;

        float currentVoltage = batteryMessage.getVoltage();

        DataBlock dataBlock;
        if (latestRecord == null) {

            dataBlock = dataStruct.createDataBlock();

        } else {

            dataBlock = latestRecord.renew();
        }

        // Populate data block
        dataBlock.setDoubleValue(0, System.currentTimeMillis()/ 1000.0);
        dataBlock.setFloatValue(1, currentVoltage);

        latestRecord = dataBlock;

        latestRecordTime = System.currentTimeMillis();

        eventHandler.publish(new DataEvent(latestRecordTime, BatteryOutput.this, dataBlock));
    }

    @Override
    public void doInit() {

        logger.debug("Initializing Output");

        defineRecordStructure();

        nodeMainExecutor = DefaultNodeMainExecutor.newDefault();

        subscriberNode = new RosSubscriberNode(NODE_NAME_STR, TOPIC_STR, Battery._TYPE, this);

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
