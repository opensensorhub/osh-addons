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
import org.vast.data.AbstractDataBlock;
import org.vast.data.DataArrayImpl;
import org.vast.swe.SWEHelper;
import spot_msgs.BatteryState;
import spot_msgs.BatteryStateArray;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Defines the output of the battery status from the platform and publishes the observations thereof
 *
 * @author Nick Garay
 * @since Sept. 25, 2023
 */
public class BatteryStatusOutput extends RosSensorOutput<SpotSensor> {

    /**
     * Conversion factor
     */
    private static final double NANOS_PER_SEC = 1000000000.;

    /**
     * Possible status values received
     */
    private static final String[] STATUSES = {"UNKNOWN", "MISSING", "CHARGING", "DISCHARGING", "BOOTING"};

    /**
     * Name of the output
     */
    private static final String SENSOR_OUTPUT_NAME = "BatteryStatus";

    /**
     * Label for the output
     */
    private static final String SENSOR_OUTPUT_LABEL = "Battery Status";

    /**
     * Description of the output
     */
    private static final String SENSOR_OUTPUT_DESCRIPTION = "Information for the battery and all cells in the system";

    /**
     * ROS Node name assigned at creation
     */
    private static final String NODE_NAME_STR = "/SensorHub/spot/battery";

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
    public BatteryStatusOutput(SpotSensor parentSpotSensor) {

        super(SENSOR_OUTPUT_NAME, parentSpotSensor, LoggerFactory.getLogger(BatteryStatusOutput.class));

        getLogger().debug("Output created");
    }

    @Override
    public void doInit() {

        getLogger().debug("Initializing Output");

        defineRecordStructure();

        nodeMainExecutor = DefaultNodeMainExecutor.newDefault();

        SpotStatusConfig.BatteryStatusOutput config =
                getParentProducer().getConfiguration().spotStatusConfig.batteryStatusOutput;

        subscriberNode = new RosSubscriberNode(
                NODE_NAME_STR, config.batteryStatusTopic, BatteryStateArray._TYPE, this);

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
                .addField("sampleTime", sweFactory.createTime().asSamplingTimeIsoUTC().build())
                .addField("batteryCount", sweFactory.createCount()
                        .label("Num Batteries")
                        .description("The count of batteries on the platform")
                        .definition(SWEHelper.getPropertyUri("battery_count"))
                        .id("batteryCount"))
                .addField("batteryStateArray", sweFactory.createArray()
                        .label("Battery State Array")
                        .description("List of battery states reported")
                        .definition(SWEHelper.getPropertyUri("battery_state_array"))
                        .withVariableSize("batteryCount")
                        .withElement("batteryState", sweFactory.createRecord()
                                .label("Battery State")
                                .description("State of a battery as reported by the platform")
                                .definition(SWEHelper.getPropertyUri("battery_state"))
                                .addField("identifier", sweFactory.createText()
                                        .label("Identifier")
                                        .description("Battery Identifier")
                                        .definition(SWEHelper.getPropertyUri("identifier")))
                                .addField("charge", sweFactory.createQuantity()
                                        .label("Charge")
                                        .description("Battery charge")
                                        .definition(SWEHelper.getPropertyUri("charge"))
                                        .uom("%")
                                        .dataType(DataType.DOUBLE))
                                .addField("runtime", sweFactory.createQuantity()
                                        .label("Runtime")
                                        .description("Battery runtime")
                                        .definition(SWEHelper.getPropertyUri("runtime"))
                                        .uom("s")
                                        .dataType(DataType.DOUBLE))
                                .addField("current", sweFactory.createQuantity()
                                        .label("Current")
                                        .description("Current")
                                        .definition(SWEHelper.getPropertyUri("current"))
                                        .uom("A")
                                        .dataType(DataType.DOUBLE))
                                .addField("voltage", sweFactory.createQuantity()
                                        .label("Voltage")
                                        .description("Voltage")
                                        .definition(SWEHelper.getPropertyUri("voltage"))
                                        .uom("V")
                                        .dataType(DataType.DOUBLE))
                                .addField("status", sweFactory.createText()
                                        .label("Status")
                                        .description("Reported battery status")
                                        .definition(SWEHelper.getPropertyUri("battery_status"))
                                        .addAllowedValues(STATUSES))
                                .addField("tempCount", sweFactory.createCount()
                                        .label("Temperature Count")
                                        .description("The count of temperatures reported by the platform for the battery")
                                        .definition(SWEHelper.getPropertyUri("temp_count"))
                                        .id("tempCount"))
                                .addField("batteryTempArray", sweFactory.createArray()
                                        .label("Battery Temperature Array")
                                        .description("List of battery temperatures reported")
                                        .definition(SWEHelper.getPropertyUri("battery_temp_array"))
                                        .withVariableSize("tempCount")
                                        .withElement("temp", sweFactory.createQuantity()
                                                .label("Temperature")
                                                .description("Temperature measurement")
                                                .definition(SWEHelper.getPropertyUri("temperature"))
                                                .uom("Cel")
                                                .dataType(DataType.DOUBLE))
                                )
                        ))
                .build();

        dataEncoding = sweFactory.newTextEncoding(",", "\n");
    }

    @Override
    public void onNewMessage(Object object) {

        updateSamplingPeriodHistogram();

        BatteryStateArray message = (BatteryStateArray) object;

        List<BatteryState> batteryStates = message.getBatteryStates();

        DataBlock dataBlock;
        if (latestRecord == null) {

            dataBlock = dataStruct.createDataBlock();

        } else {

            dataBlock = latestRecord.renew();
        }

        dataBlock.setDoubleValue(0, System.currentTimeMillis() / 1000.);
        dataBlock.setIntValue(1, message.getBatteryStates().size());

        DataArrayImpl batteryStateArray = (DataArrayImpl)dataStruct.getComponent("batteryStateArray");
        batteryStateArray.updateSize();
        List<DataBlock> batteryStatesList = new ArrayList<>();

        for (BatteryState batteryState : batteryStates) {

            DataBlock batteryStateDataBlock = dataStruct
                    .getComponent("batteryStateArray")
                    .getComponent("batteryState")
                    .createDataBlock();

            // Set each element for the underlying container
            batteryStateDataBlock.setStringValue(0, batteryState.getIdentifier());
            batteryStateDataBlock.setDoubleValue(1, batteryState.getChargePercentage());
            batteryStateDataBlock.setDoubleValue(2, batteryState.getEstimatedRuntime().totalNsecs() / NANOS_PER_SEC);
            batteryStateDataBlock.setDoubleValue(3, batteryState.getCurrent());
            batteryStateDataBlock.setDoubleValue(4, batteryState.getVoltage());
            batteryStateDataBlock.setStringValue(5, STATUSES[batteryState.getStatus()]);
            batteryStateDataBlock.setIntValue(6, batteryState.getTemperatures().length);

            DataArrayImpl batteryTempArray = (DataArrayImpl) dataStruct.getComponent("batteryStateArray")
                    .getComponent("batteryState")
                    .getComponent("batteryTempArray");
            batteryTempArray.updateSize();

            double[] temps = new double[batteryState.getTemperatures().length];

            int idx = 0;
            for (double temp : batteryState.getTemperatures()) {

                temps[idx++] = temp;
            }

            ((AbstractDataBlock[])(batteryStateDataBlock.getUnderlyingObject()))[7].setUnderlyingObject(temps);

            batteryStatesList.add(batteryStateDataBlock);
        }

        ((AbstractDataBlock[])(dataBlock.getUnderlyingObject()))[2].setUnderlyingObject(batteryStatesList);

        dataBlock.updateAtomCount();

        latestRecord = dataBlock;

        latestRecordTime = System.currentTimeMillis();

        eventHandler.publish(new DataEvent(latestRecordTime, BatteryStatusOutput.this, dataBlock));
    }
}
