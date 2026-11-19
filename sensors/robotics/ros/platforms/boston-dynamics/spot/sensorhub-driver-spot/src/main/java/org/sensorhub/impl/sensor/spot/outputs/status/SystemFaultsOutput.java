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
import net.opengis.swe.v20.DataRecord;
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
import spot_msgs.SystemFault;
import spot_msgs.SystemFaultState;

import java.net.URI;
import java.util.List;

/**
 * Defines the output of the system fault status from the platform and publishes the observations thereof
 *
 * @author Nick Garay
 * @since Sept. 25, 2023
 */
public class SystemFaultsOutput extends RosSensorOutput<SpotSensor> {

    /**
     * Conversion factor
     */
    private static final double NANOS_PER_SEC = 1000000000.;

    /**
     * Possible severity of faults reported
     */
    private static final String[] SEVERITIES = {"SEVERITY_UNKNOWN", "SEVERITY_INFO", "SEVERITY_WARN", "SEVERITY_CRITICAL"};

    /**
     * Name of the output
     */
    private static final String SENSOR_OUTPUT_NAME = "SystemFaults";

    /**
     * Label for the output
     */
    private static final String SENSOR_OUTPUT_LABEL = "System Faults";

    /**
     * Description of the output
     */
    private static final String SENSOR_OUTPUT_DESCRIPTION = "A listing of system faults in the system";

    /**
     * ROS Node name assigned at creation
     */
    private static final String NODE_NAME_STR = "/SensorHub/spot/system_faults";

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
    public SystemFaultsOutput(SpotSensor parentSpotSensor) {

        super(SENSOR_OUTPUT_NAME, parentSpotSensor, LoggerFactory.getLogger(BatteryStatusOutput.class));

        getLogger().debug("Output created");
    }

    @Override
    public void doInit() {

        getLogger().debug("Initializing Output");

        defineRecordStructure();

        nodeMainExecutor = DefaultNodeMainExecutor.newDefault();

        SpotStatusConfig.SystemFaultsStatusOutput config =
                getParentProducer().getConfiguration().spotStatusConfig.systemFaultsStatusOutput;

        subscriberNode = new RosSubscriberNode(
                NODE_NAME_STR, config.systemFaultsTopic, SystemFaultState._TYPE, this);

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
                .addField("sampleTime", sweFactory.createTime().asSamplingTimeIsoUTC())
                .addField("faultCount", sweFactory.createCount()
                        .label("Fault Count")
                        .description("Number of reported current system faults")
                        .definition(SWEHelper.getPropertyUri("fault_count"))
                        .id("faultCount"))
                .addField("historicalFaultCount", sweFactory.createCount()
                        .label("Historical Fault Count")
                        .description("Number of reported historical system faults")
                        .definition(SWEHelper.getPropertyUri("historical_fault_count"))
                        .id("historicalFaultCount"))
                .addField("faultArray", sweFactory.createArray()
                        .label("Fault Array")
                        .description("List of faults reported")
                        .definition(SWEHelper.getPropertyUri("fault_array"))
                        .withVariableSize("faultCount")
                        .withElement("faultRecord", sweFactory.createRecord()
                                .label("Fault Record")
                                .description("Fault parameters reported by the platform")
                                .definition(SWEHelper.getPropertyUri("fault_record"))
                                .addField("faultName", sweFactory.createText()
                                        .label("Fault Name")
                                        .description("The reported name of the fault")
                                        .definition(SWEHelper.getPropertyUri("")))
                                .addField("duration", sweFactory.createQuantity()
                                        .label("Duration")
                                        .description("Duration of the fault")
                                        .definition(SWEHelper.getPropertyUri("fault_duration"))
                                        .uom("s")
                                        .dataType(DataType.DOUBLE))
                                .addField("code", sweFactory.createText()
                                        .label("Fault Code")
                                        .description("Fault code reported")
                                        .definition(SWEHelper.getPropertyUri("fault_code")))
                                .addField("uid", sweFactory.createText()
                                        .label("Fault UID")
                                        .description("Fault unique identifier")
                                        .definition(SWEHelper.getPropertyUri("fault_uid")))
                                .addField("errorMessage", sweFactory.createText()
                                        .label("Error Message")
                                        .description("Error message reported with the fault")
                                        .definition(SWEHelper.getPropertyUri("error_message")))
                                .addField("severity", sweFactory.createText()
                                        .label("Severity")
                                        .description("Fault severity")
                                        .definition(SWEHelper.getPropertyUri("fault_severity"))
                                        .addAllowedValues(SEVERITIES))
                                .addField("attributeCount", sweFactory.createCount()
                                        .label("Attribute Count")
                                        .description("Count of the attributes reported with the fault")
                                        .definition(SWEHelper.getPropertyUri("attribute_count"))
                                        .id("attributeCount"))
                                .addField("attributeArray", sweFactory.createArray()
                                        .label("Attribute Array")
                                        .description("The list of attributes reported with the fault")
                                        .definition(SWEHelper.getPropertyUri("attributes"))
                                        .withVariableSize("attributeCount")
                                        .withElement("attribute", sweFactory.createText()
                                                .label("Attribute")
                                                .description("Fault attributes")
                                                .definition(SWEHelper.getPropertyUri("fault_record"))))
                        ))
                .addField("historicalFaultArray", sweFactory.createArray()
                        .label("Historical Fault Array")
                        .description("List of historical faults reported")
                        .definition(SWEHelper.getPropertyUri("hsitorical_fault_array"))
                        .withVariableSize("historicalFaultCount")
                        .withElement("faultRecord", sweFactory.createRecord()
                                .label("Fault Record")
                                .description("Fault parameters reported by the platform")
                                .definition(SWEHelper.getPropertyUri("fault_record"))
                                .addField("faultName", sweFactory.createText()
                                        .label("Fault Name")
                                        .description("The reported name of the fault")
                                        .definition(SWEHelper.getPropertyUri("")))
                                .addField("duration", sweFactory.createQuantity()
                                        .label("Duration")
                                        .description("Duration of the fault")
                                        .definition(SWEHelper.getPropertyUri("fault_duration"))
                                        .uom("s")
                                        .dataType(DataType.DOUBLE))
                                .addField("code", sweFactory.createText()
                                        .label("Fault Code")
                                        .description("Fault code reported")
                                        .definition(SWEHelper.getPropertyUri("fault_code")))
                                .addField("uid", sweFactory.createText()
                                        .label("Fault UID")
                                        .description("Fault unique identifier")
                                        .definition(SWEHelper.getPropertyUri("fault_uid")))
                                .addField("errorMessage", sweFactory.createText()
                                        .label("Error Message")
                                        .description("Error message reported with the fault")
                                        .definition(SWEHelper.getPropertyUri("error_message")))
                                .addField("severity", sweFactory.createText()
                                        .label("Severity")
                                        .description("Fault severity")
                                        .definition(SWEHelper.getPropertyUri("fault_severity"))
                                        .addAllowedValues(SEVERITIES))
                                .addField("attributeCount", sweFactory.createCount()
                                        .label("Attribute Count")
                                        .description("Count of the attributes reported with the fault")
                                        .definition(SWEHelper.getPropertyUri("attribute_count"))
                                        .id("attributeCount"))
                                .addField("attributeArray", sweFactory.createArray()
                                        .label("Attribute Array")
                                        .description("The list of attributes reported with the fault")
                                        .definition(SWEHelper.getPropertyUri("attributes"))
                                        .withVariableSize("attributeCount")
                                        .withElement("attribute", sweFactory.createText()
                                                .label("Attribute")
                                                .description("Fault attributes")
                                                .definition(SWEHelper.getPropertyUri("fault_record"))))
                        ))
                .build();

        dataEncoding = sweFactory.newTextEncoding(",", "\n");
    }

    private int setFaultData(DataArrayImpl dataArray, DataBlock dataBlock, List<SystemFault> faults, int index) {

        for (int i =0; i < faults.size(); i++) {

            SystemFault currentFault = faults.get(i);
            // Set each element for the underlying container
            dataBlock.setStringValue(index++, currentFault.getName());
            dataBlock.setDoubleValue(index++, currentFault.getDuration().totalNsecs() / NANOS_PER_SEC);
            dataBlock.setStringValue(index++, String.valueOf(currentFault.getCode()));
            dataBlock.setStringValue(index++, String.valueOf(currentFault.getUid()));
            dataBlock.setStringValue(index++, currentFault.getErrorMessage());
            dataBlock.setStringValue(index++, SEVERITIES[currentFault.getSeverity()]);
            dataBlock.setIntValue(index++, currentFault.getAttributes().size());

            var faultRecord = (DataRecord)dataArray.getComponent(i);
            var attributeArray = (DataArrayImpl)faultRecord.getComponent("attributeArray");
            attributeArray.updateSize();
            dataBlock.updateAtomCount();

            for (String attribute : currentFault.getAttributes()) {

                dataBlock.setStringValue(index++, attribute);
            }
        }

        return index;
    }

    @Override
    public void onNewMessage(Object object) {

        updateSamplingPeriodHistogram();

        SystemFaultState message = (SystemFaultState) object;

        // Need to recreate the data record structure or createDataBlock will fail the second and subsequent times.
        defineRecordStructure();
        DataBlock dataBlock = dataStruct.createDataBlock();
        // Pair the data record with the data block.
        // This is necessary for updating the array size after setting the array count.
        dataStruct.setData(dataBlock);

        List<SystemFault> faults = message.getFaults();
        List<SystemFault> historicalFaults = message.getHistoricalFaults();

        int index = 0;

        dataBlock.setDoubleValue(index++, System.currentTimeMillis() / 1000.);
        dataBlock.setIntValue(index++, faults.size());
        var faultArray = ((DataArrayImpl) dataStruct.getComponent("faultArray"));
        faultArray.updateSize();
        dataBlock.updateAtomCount();

        dataBlock.setIntValue(index++, historicalFaults.size());
        var historicalFaultArray = ((DataArrayImpl) dataStruct.getComponent("historicalFaultArray"));
        historicalFaultArray.updateSize();
        dataBlock.updateAtomCount();

        index = setFaultData(faultArray, dataBlock, faults, index);

        setFaultData(historicalFaultArray, dataBlock, historicalFaults, index);

        latestRecord = dataBlock;

        latestRecordTime = System.currentTimeMillis();

        eventHandler.publish(new DataEvent(latestRecordTime, SystemFaultsOutput.this, dataBlock));
    }
}