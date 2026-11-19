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
import org.vast.data.DataArrayImpl;
import org.vast.swe.SWEHelper;
import spot_msgs.BehaviorFault;
import spot_msgs.BehaviorFaultState;

import java.net.URI;
import java.util.List;

/**
 * Defines the output of the behavior faults status from the platform and publishes the observations thereof
 *
 * @author Nick Garay
 * @since Sept. 25, 2023
 */
public class BehaviorFaultsOutput extends RosSensorOutput<SpotSensor> {

    /**
     * Possible cause values received
     */
    private static final String[] CAUSES = {"CAUSE_UNKNOWN", "CAUSE_FALL", "CAUSE_HARDWARE"};

    /**
     * Possible status values received
     */
    private static final String[] STATUSES = {"STATUS_UNKNOWN", "STATUS_CLEARABLE", "STATUS_UNCLEARABLE"};

    /**
     * Name of the output
     */
    private static final String SENSOR_OUTPUT_NAME = "BehaviorFaults";

    /**
     * Label for the output
     */
    private static final String SENSOR_OUTPUT_LABEL = "Behaviour Faults";

    /**
     * Description of the output
     */
    private static final String SENSOR_OUTPUT_DESCRIPTION = "A listing of behavior faults in the system";

    /**
     * ROS Node name assigned at creation
     */
    private static final String NODE_NAME_STR = "/SensorHub/spot/behaviour_faults";

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
    public BehaviorFaultsOutput(SpotSensor parentSpotSensor) {

        super(SENSOR_OUTPUT_NAME, parentSpotSensor, LoggerFactory.getLogger(BatteryStatusOutput.class));

        getLogger().debug("Output created");
    }

    @Override
    public void doInit() {

        getLogger().debug("Initializing Output");

        defineRecordStructure();

        nodeMainExecutor = DefaultNodeMainExecutor.newDefault();

        SpotStatusConfig.BehaviorFaultsStatusOutput config =
                getParentProducer().getConfiguration().spotStatusConfig.behaviorFaultsStatusOutput;

        subscriberNode = new RosSubscriberNode(
                NODE_NAME_STR, config.behaviorFaultsTopic, BehaviorFaultState._TYPE, this);

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
                .addField("faultCount", sweFactory.createCount()
                        .label("Fault Count")
                        .description("Number of reported faults")
                        .definition(SWEHelper.getPropertyUri("fault_count"))
                        .id("faultCount"))
                .addField("faultArray", sweFactory.createArray()
                        .label("Fault Array")
                        .description("List of faults reported")
                        .definition(SWEHelper.getPropertyUri("fault_array"))
                        .withVariableSize("faultCount")
                        .withElement("faultRecord", sweFactory.createRecord()
                                .label("Fault Record")
                                .description("Fault parameters are reported by the platform")
                                .definition(SWEHelper.getPropertyUri("fault_record"))
                                .addField("faultId", sweFactory.createText()
                                        .label("Fault ID")
                                        .description("Identifier for the reported fault")
                                        .definition(SWEHelper.getPropertyUri("fault_id")))
                                .addField("cause", sweFactory.createText()
                                        .label("Cause")
                                        .description("Cause of the reported fault")
                                        .definition(SWEHelper.getPropertyUri("cause")))
                                .addField("status", sweFactory.createText()
                                        .label("Status")
                                        .description("Reported status with the fault")
                                        .definition(SWEHelper.getPropertyUri("status")))
                        ))
                .build();

        dataEncoding = sweFactory.newTextEncoding(",", "\n");
    }

    @Override
    public void onNewMessage(Object object) {

        updateSamplingPeriodHistogram();

        BehaviorFaultState message = (BehaviorFaultState) object;

        // Need to recreate the data record structure or createDataBlock will fail the second and subsequent times.
        defineRecordStructure();
        DataBlock dataBlock = dataStruct.createDataBlock();
        // Pair the data record with the data block.
        // This is necessary for updating the array size after setting the array count.
        dataStruct.setData(dataBlock);

        List<BehaviorFault> faults = message.getFaults();

        int index = 0;

        dataBlock.setDoubleValue(index++, System.currentTimeMillis() / 1000.);
        // Set the array count.
        dataBlock.setIntValue(index++, faults.size());

        // Update the array size. This can only be done after setting the array count.
        var faultArray = ((DataArrayImpl) dataStruct.getComponent("faultArray"));
        faultArray.updateSize();
        dataBlock.updateAtomCount();

        for (BehaviorFault currentFault : faults) {

            dataBlock.setStringValue(index++, String.valueOf(currentFault.getBehaviorFaultId()));
            dataBlock.setStringValue(index++, CAUSES[currentFault.getCause()]);
            dataBlock.setStringValue(index++, STATUSES[currentFault.getStatus()]);
        }

        latestRecord = dataBlock;

        latestRecordTime = System.currentTimeMillis();

        eventHandler.publish(new DataEvent(latestRecordTime, BehaviorFaultsOutput.this, dataBlock));
    }
}
