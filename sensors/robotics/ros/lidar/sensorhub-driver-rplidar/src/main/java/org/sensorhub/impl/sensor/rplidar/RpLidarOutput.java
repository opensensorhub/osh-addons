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
package org.sensorhub.impl.sensor.rplidar;

import org.sensorhub.impl.ros.config.RosMasterConfig;
import org.sensorhub.impl.ros.nodes.pubsub.RosSubscriberNode;
import org.sensorhub.impl.ros.output.RosSensorOutput;
import org.sensorhub.impl.ros.utils.RosUtils;
import net.opengis.swe.v20.*;
import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.sensorhub.api.data.DataEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.DataBlockMixed;
import org.vast.swe.SWEHelper;
import sensor_msgs.LaserScan;

import java.net.URI;

/**
 * Output specification and provider for ...
 *
 * @author Nick Garay
 * @since Feb. 2, 2023
 */
public class RpLidarOutput extends RosSensorOutput<RpLidarSensor> {

    private static final String SENSOR_OUTPUT_NAME = "LaserScan";
    private static final String SENSOR_OUTPUT_LABEL = "Laser Scan";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "Laser scan results from RP LIDAR";

    private static final String NODE_NAME_STR = "/SensorHub/rplidar/laserscan";

    private static final String TOPIC_STR = "/scan";

    private static final Logger logger = LoggerFactory.getLogger(RpLidarOutput.class);

    private NodeMainExecutor nodeMainExecutor;

    private RosSubscriberNode subscriberNode;

    /**
     * Constructor
     *
     * @param parentRpLidarSensor Sensor driver providing this output
     */
    RpLidarOutput(RpLidarSensor parentRpLidarSensor) {

        super(SENSOR_OUTPUT_NAME, parentRpLidarSensor, logger);

        logger.debug("Output created");
    }

    @Override
    protected void defineRecordStructure() {

        // Get an instance of SWE Factory suitable to build components
        SWEHelper sweFactory = new SWEHelper();

        Time timeStamp = sweFactory.createTime().asSamplingTimeIsoUTC().build();

        Quantity minAngle = sweFactory.createQuantity()
                .name("minAngle")
                .label("Min Angle")
                .definition(SWEHelper.getPropertyUri("minAngle"))
                .description("Start angle of the scan")
                .uom("rad")
                .dataType(DataType.FLOAT)
                .build();

        Quantity maxAngle = sweFactory.createQuantity()
                .name("maxAngle")
                .label("Max Angle")
                .definition(SWEHelper.getPropertyUri("maxAngle"))
                .description("End angle of the scan")
                .uom("rad")
                .dataType(DataType.FLOAT)
                .build();

        Quantity angleIncrement = sweFactory.createQuantity()
                .name("angleIncrement")
                .label("Angle Increment")
                .definition(SWEHelper.getPropertyUri("angleIncrement"))
                .description("Angular distance between measurements")
                .uom("rad")
                .dataType(DataType.FLOAT)
                .build();

        Quantity timeIncrement = sweFactory.createQuantity()
                .name("timeIncrement")
                .label("Time Increment")
                .definition(SWEHelper.getPropertyUri("timeIncrement"))
                .description("time between measurements - if your scanner " +
                        "is moving, this will be used in interpolating position " +
                        "of 3d points")
                .uom("s")
                .dataType(DataType.FLOAT)
                .build();

        Quantity scanTime = sweFactory.createQuantity()
                .name("scanTime")
                .label("Scan Time")
                .definition(SWEHelper.getPropertyUri("scanTime"))
                .description("time between scans")
                .uom("s")
                .dataType(DataType.FLOAT)
                .build();

        Quantity minRange = sweFactory.createQuantity()
                .name("minRange")
                .label("Min Range")
                .definition(SWEHelper.getPropertyUri("minRange"))
                .description("Minimum range value")
                .uom("m")
                .dataType(DataType.FLOAT)
                .build();

        Quantity maxRange = sweFactory.createQuantity()
                .name("maxRange")
                .label("Max Range")
                .definition(SWEHelper.getPropertyUri("maxRange"))
                .description("Maximum range value")
                .uom("m")
                .dataType(DataType.FLOAT)
                .build();

        Count rangeSampleCount = sweFactory.createCount()
                .name("rangeSampleCount")
                .label("Num Range Samples")
                .id("rangeSampleCount")
                .build();

        Quantity rangeDataPoint = sweFactory.createQuantity()
                .name("rangeDataPoint")
                .label("Range Data Point")
                .definition(SWEHelper.getPropertyUri("Distance"))
                .description("Observed distance")
                .uom("m")
                .dataType(DataType.FLOAT)
                .build();

        DataArray rangeDataArray = sweFactory.createArray()
                .name("rangeDataArray")
                .label("Ranges")
                .definition(SWEHelper.getPropertyUri("rangeDataArray"))
                .description("Range data [m] (Note: values < range_min or > range_max should be discarded)")
                .withVariableSize(rangeSampleCount.getId())
                .build();

        rangeDataArray.addComponent(rangeDataPoint.getName(), rangeDataPoint);

        Count intensitySampleCount = sweFactory.createCount()
                .name("intensitySampleCount")
                .label("Num Intensity Samples")
                .id("intensitySampleCount")
                .build();

        Quantity intensityDataPoint = sweFactory.createQuantity()
                .name("intensityDataPoint")
                .label("Intensity Data Point")
                .definition(SWEHelper.getPropertyUri("Intensity"))
                .description("Observed intensity")
                .uom("cd")
                .dataType(DataType.FLOAT)
                .build();

        DataArray intensityDataArray = sweFactory.createArray()
                .name("intensityDataArray")
                .label("Intensities")
                .definition(SWEHelper.getPropertyUri("intensityDataArray"))
                .description("Intensity data. If your " +
                        "device does not provide intensities, please leave " +
                        "the array empty.")
                .withVariableSize(intensitySampleCount.getId())
                .build();

        intensityDataArray.addComponent(intensityDataPoint.getName(), intensityDataPoint);

        // Create data record description
        dataStruct = sweFactory.createRecord()
                .name(getName())
                .updatable(true)
                .definition(SWEHelper.getPropertyUri(SENSOR_OUTPUT_NAME))
                .label(SENSOR_OUTPUT_LABEL)
                .description(SENSOR_OUTPUT_DESCRIPTION)
                .addField("SampleTime", timeStamp)
                .addField(minAngle.getName(), minAngle)
                .addField(maxAngle.getName(), maxAngle)
                .addField(angleIncrement.getName(), angleIncrement)
                .addField(timeIncrement.getName(), timeIncrement)
                .addField(scanTime.getName(), scanTime)
                .addField(minRange.getName(), minRange)
                .addField(maxRange.getName(), maxRange)
                .addField(rangeSampleCount.getName(), rangeSampleCount)
                .addField(intensitySampleCount.getName(), intensitySampleCount)
                .addField(rangeDataArray.getName(), rangeDataArray)
                .addField(intensityDataArray.getName(), intensityDataArray)
                .build();

        dataEncoding = sweFactory.newTextEncoding(",", "\n");
    }

    @Override
    public void onNewMessage(Object object) {

        updateSamplingPeriodHistogram();

        LaserScan laserScan = (LaserScan) object;

        DataBlock dataBlock;
        if (latestRecord == null) {

            dataBlock = dataStruct.createDataBlock();

        } else {

            dataBlock = latestRecord.renew();
        }

        // Populate data block
        dataBlock.setDoubleValue(0, System.currentTimeMillis() / 1000.0);

        // Min/Max Angles
        dataBlock.setFloatValue(1, laserScan.getAngleMin());
        dataBlock.setFloatValue(2, laserScan.getAngleMax());

        // Angle Increment
        dataBlock.setFloatValue(3, laserScan.getAngleIncrement());

        // Time Increment
        dataBlock.setFloatValue(4, laserScan.getTimeIncrement());

        // Scan Time
        dataBlock.setFloatValue(5, laserScan.getScanTime());

        // Min/Max Ranges
        dataBlock.setFloatValue(6, laserScan.getRangeMin());
        dataBlock.setFloatValue(7, laserScan.getRangeMax());

        // Range Data Count
        dataBlock.setIntValue(8, laserScan.getRanges().length);

        // Intensity Data Count
        dataBlock.setIntValue(9, laserScan.getIntensities().length);

        // Range Data
        ((DataBlockMixed) dataBlock).getUnderlyingObject()[10].setUnderlyingObject(laserScan.getRanges());

        // Intensity Data
        ((DataBlockMixed) dataBlock).getUnderlyingObject()[11].setUnderlyingObject(laserScan.getIntensities());

        ((DataBlockMixed) dataBlock).updateAtomCount();

        latestRecord = dataBlock;

        latestRecordTime = System.currentTimeMillis();

        eventHandler.publish(new DataEvent(latestRecordTime, RpLidarOutput.this, dataBlock));
    }

    @Override
    public void doInit() {

        logger.debug("Initializing Output");

        defineRecordStructure();

        nodeMainExecutor = DefaultNodeMainExecutor.newDefault();

        subscriberNode = new RosSubscriberNode(NODE_NAME_STR, TOPIC_STR, LaserScan._TYPE, this);

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
