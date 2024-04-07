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
import spot_msgs.Lease;
import spot_msgs.LeaseArray;
import spot_msgs.LeaseOwner;
import spot_msgs.LeaseResource;

import java.net.URI;
import java.util.List;

/**
 * Defines the output of the lease status from the platform and publishes the observations thereof
 *
 * @author Nick Garay
 * @since Sept. 25, 2023
 */
public class LeaseStatusOutput extends RosSensorOutput<SpotSensor> {

    /**
     * Name of the output
     */
    private static final String SENSOR_OUTPUT_NAME = "LeaseStatus";

    /**
     * Label for the output
     */
    private static final String SENSOR_OUTPUT_LABEL = "Lease Status";

    /**
     * Description of the output
     */
    private static final String SENSOR_OUTPUT_DESCRIPTION = "A list of what leases are held on the system";

    /**
     * ROS Node name assigned at creation
     */
    private static final String NODE_NAME_STR = "/SensorHub/spot/lease_status";

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
    public LeaseStatusOutput(SpotSensor parentSpotSensor) {

        super(SENSOR_OUTPUT_NAME, parentSpotSensor, LoggerFactory.getLogger(BatteryStatusOutput.class));

        getLogger().debug("Output created");
    }

    @Override
    public void doInit() {

        getLogger().debug("Initializing Output");

        defineRecordStructure();

        nodeMainExecutor = DefaultNodeMainExecutor.newDefault();

        SpotStatusConfig.LeaseStatusOutput config =
                getParentProducer().getConfiguration().spotStatusConfig.leaseStatusOutput;

        subscriberNode = new RosSubscriberNode(
                NODE_NAME_STR, config.leaseStatusTopic, LeaseArray._TYPE, this);

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
                .addField("leaseResourceCount", sweFactory.createCount()
                        .label("Lease Resource Count")
                        .description("Number of reported lease resources")
                        .definition(SWEHelper.getPropertyUri("lease_resource_count"))
                        .id("leaseResourceCount"))
                .addField("leaseResourceArray", sweFactory.createArray()
                        .label("Lease Resource Array")
                        .description("List of lease resources reported")
                        .definition(SWEHelper.getPropertyUri("lease_resource_array"))
                        .withVariableSize("leaseResourceCount")
                        .withElement("leaseResource", sweFactory.createRecord()
                                .label("Lease Resource")
                                .description("Lease resource reported by the platform")
                                .definition(SWEHelper.getPropertyUri("lease_resource"))
                                .addField("lease", sweFactory.createRecord()
                                        .label("Lease")
                                        .description("Leases are used to verify that a client has exclusive access to a shared resources")
                                        .definition(SWEHelper.getPropertyUri("lease"))
                                        .addField("epoch", sweFactory.createText()
                                                .label("Epoch")
                                                .description("The epoch for the Lease. The sequences field are scoped to a particular epoch.\n" +
                                                        "One example of where this can be used is to generate a random epoch\n" +
                                                        "at LeaseService startup.")
                                                .definition(SWEHelper.getPropertyUri("")))
                                        .addField("resource", sweFactory.createText()
                                                .label("Resource")
                                                .description("The resource that the Lease is for")
                                                .definition(SWEHelper.getPropertyUri("resource")))
                                        .addField("leaseSequenceCount", sweFactory.createCount()
                                                .label("Lease Sequence Count")
                                                .description("")
                                                .definition(SWEHelper.getPropertyUri("lease_sequence_count"))
                                                .id("leaseSequenceCount"))
                                        .addField("leaseSequence", sweFactory.createArray()
                                                .label("Lease Sequence")
                                                .description("Logical vector clock indicating when the Lease was generated.")
                                                .definition(SWEHelper.getPropertyUri("lease_sequence"))
                                                .withVariableSize("leaseSequenceCount")
                                                .withElement("element", sweFactory.createQuantity()
                                                        .label("Element")
                                                        .description("Element of the sequence")
                                                        .definition(SWEHelper.getPropertyUri("sequence_element"))
                                                        .dataType(DataType.INT))))
                                .addField("resource", sweFactory.createText()
                                        .label("Resource")
                                        .description("The resource name.")
                                        .definition(SWEHelper.getPropertyUri("resource")))
                                .addField("leaseOwner", sweFactory.createRecord()
                                        .label("Lease Owner")
                                        .description("Information identifying owner of the lease")
                                        .definition(SWEHelper.getPropertyUri("lease_owner"))
                                        .addField("clientName", sweFactory.createText()
                                                .label("Client Name")
                                                .description("Name of client owning the lease")
                                                .definition(SWEHelper.getPropertyUri("client_name")))
                                        .addField("userName", sweFactory.createText()
                                                .label("User Name")
                                                .description("User name of the lease owner")
                                                .definition(SWEHelper.getPropertyUri("user_name"))))
                        ))
                .build();

        dataEncoding = sweFactory.newTextEncoding(",", "\n");
    }

    @Override
    public void onNewMessage(Object object) {

        updateSamplingPeriodHistogram();

        LeaseArray message = (LeaseArray) object;

        // Need to recreate the data record structure or createDataBlock will fail the second and subsequent times.
        defineRecordStructure();
        DataBlock dataBlock = dataStruct.createDataBlock();
        // Pair the data record with the data block.
        // This is necessary for updating the array size after setting the array count.
        dataStruct.setData(dataBlock);

        List<LeaseResource> leaseResources = message.getResources();

        int index = 0;

        dataBlock.setDoubleValue(index++, System.currentTimeMillis() / 1000.);
        // Set the array count.
        dataBlock.setIntValue(index++, leaseResources.size());

        // Update the array size. This can only be done after setting the array count.
        var leaseResourceArray = ((DataArrayImpl) dataStruct.getComponent("leaseResourceArray"));
        leaseResourceArray.updateSize();
        dataBlock.updateAtomCount();

        for (int i = 0; i < leaseResources.size(); i++) {
            LeaseResource leaseResource = leaseResources.get(i);
            Lease lease = leaseResource.getLease();
            dataBlock.setStringValue(index++, lease.getEpoch());
            dataBlock.setStringValue(index++, lease.getResource());

            int[] leaseSequence = lease.getSequence();
            dataBlock.setIntValue(index++, leaseSequence.length);

            // Note that we use the array index to get the leaseResource data record because each element of the
            // leaseResourceArray is a data record, and each one needs its child array updated.
            var leaseResourceDataRecord = (DataRecord) leaseResourceArray.getComponent(i);
            var leaseDataRecord = (DataRecord) leaseResourceDataRecord.getComponent("lease");
            var leaseSequenceArray = (DataArrayImpl) leaseDataRecord.getComponent("leaseSequence");
            leaseSequenceArray.updateSize();
            dataBlock.updateAtomCount();

            for (int sequenceElement : leaseSequence) {
                dataBlock.setIntValue(index++, sequenceElement);
            }

            dataBlock.setStringValue(index++, leaseResource.getResource());

            LeaseOwner leaseOwner = leaseResource.getLeaseOwner();
            dataBlock.setStringValue(index++, leaseOwner.getClientName());
            dataBlock.setStringValue(index++, leaseOwner.getUserName());
        }

        latestRecord = dataBlock;

        latestRecordTime = System.currentTimeMillis();

        eventHandler.publish(new DataEvent(latestRecordTime, LeaseStatusOutput.this, dataBlock));
    }
}