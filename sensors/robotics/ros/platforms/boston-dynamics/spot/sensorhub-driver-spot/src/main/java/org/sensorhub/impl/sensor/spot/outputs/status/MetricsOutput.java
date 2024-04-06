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
import spot_msgs.Metrics;

import java.net.URI;

/**
 * Defines the output of the metrics from the platform and publishes the observations thereof
 *
 * @author Nick Garay
 * @since Sept. 25, 2023
 */
public class MetricsOutput extends RosSensorOutput<SpotSensor> {

    /**
     * Conversion factor
     */
    private static final double NANOS_PER_SEC = 1000000000.;

    /**
     * Name of the output
     */
    private static final String SENSOR_OUTPUT_NAME = "Metrics";

    /**
     * Label for the output
     */
    private static final String SENSOR_OUTPUT_LABEL = "Metrics";

    /**
     * Description of the output
     */
    private static final String SENSOR_OUTPUT_DESCRIPTION = "General metrics for the system like distance walked";

    /**
     * ROS Node name assigned at creation
     */
    private static final String NODE_NAME_STR = "/SensorHub/spot/metrics";

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
    public MetricsOutput(SpotSensor parentSpotSensor) {

        super(SENSOR_OUTPUT_NAME, parentSpotSensor, LoggerFactory.getLogger(BatteryStatusOutput.class));

        getLogger().debug("Output created");
    }

    @Override
    public void doInit() {

        getLogger().debug("Initializing Output");

        defineRecordStructure();

        nodeMainExecutor = DefaultNodeMainExecutor.newDefault();

        SpotStatusConfig.MetricsOutput config =
                getParentProducer().getConfiguration().spotStatusConfig.metricsOutput;

        subscriberNode = new RosSubscriberNode(
                NODE_NAME_STR, config.metricsTopic, Metrics._TYPE, this);

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
                .addField("distance", sweFactory.createQuantity()
                        .label("Distance")
                        .description("Distance traveled by the platform")
                        .definition(SWEHelper.getPropertyUri("distance"))
                        .uom("m")
                        .dataType(DataType.DOUBLE))
                .addField("gaitCycles", sweFactory.createCount()
                        .label("Gait Cycles")
                        .description("Count of the number of gait cycles")
                        .definition(SWEHelper.getPropertyUri("gait_cycles"))
                        .dataType(DataType.INT))
                .addField("electricPower", sweFactory.createQuantity()
                        .label("Electric Power")
                        .description("Duration of time powered")
                        .definition(SWEHelper.getPropertyUri("electric_power"))
                        .dataType(DataType.DOUBLE)
                        .uom("s"))
                .addField("timeMoving", sweFactory.createQuantity()
                        .label("Time Moving")
                        .description("Duration of time spent moving")
                        .definition(SWEHelper.getPropertyUri("time_moving"))
                        .dataType(DataType.DOUBLE)
                        .uom("s"))
                .build();

        dataEncoding = sweFactory.newTextEncoding(",", "\n");
    }

    @Override
    public void onNewMessage(Object object) {

        updateSamplingPeriodHistogram();

        Metrics message = (Metrics) object;

        DataBlock dataBlock;
        if (latestRecord == null) {

            dataBlock = dataStruct.createDataBlock();

        } else {

            dataBlock = latestRecord.renew();
        }

        dataBlock.setDoubleValue(0, System.currentTimeMillis() / 1000.);
        dataBlock.setDoubleValue(1, message.getDistance());
        dataBlock.setIntValue(2, message.getGaitCycles());
        dataBlock.setDoubleValue(3, message.getElectricPower().totalNsecs() / NANOS_PER_SEC);
        dataBlock.setDoubleValue(4, message.getTimeMoving().totalNsecs() / NANOS_PER_SEC);

        latestRecord = dataBlock;

        latestRecordTime = System.currentTimeMillis();

        eventHandler.publish(new DataEvent(latestRecordTime, MetricsOutput.this, dataBlock));
    }
}