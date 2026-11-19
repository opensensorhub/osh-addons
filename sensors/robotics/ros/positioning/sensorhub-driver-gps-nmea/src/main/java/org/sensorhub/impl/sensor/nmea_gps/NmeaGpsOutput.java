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
package org.sensorhub.impl.sensor.nmea_gps;

import net.opengis.swe.v20.*;
import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.ros.config.RosMasterConfig;
import org.sensorhub.impl.ros.nodes.pubsub.RosSubscriberNode;
import org.sensorhub.impl.ros.output.RosSensorOutput;
import org.sensorhub.impl.ros.utils.RosUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.DataBlockMixed;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;
import sensor_msgs.NavSatFix;

import java.net.URI;

/**
 * Output specification and provider for ...
 *
 * @author Nick Garay
 * @since Mar. 7, 2023
 */
public class NmeaGpsOutput extends RosSensorOutput<NmeaGpsSensor> {

    private static final String SENSOR_OUTPUT_NAME = "GPS";
    private static final String SENSOR_OUTPUT_LABEL = "GPS";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "GPS Position";

    private static final String NODE_NAME_STR = "/SensorHub/gps";

    private static final String TOPIC_STR = "/fix";

    private static final Logger logger = LoggerFactory.getLogger(NmeaGpsOutput.class);

    private NodeMainExecutor nodeMainExecutor;

    private RosSubscriberNode subscriberNode;

    private final String[] covarianceTypes = {"Unknown", "Approximated", "Diagonal Known", "Type Known"};

    /**
     * Constructor
     *
     * @param parentNmeaGpsSensor Sensor driver providing this output
     */
    NmeaGpsOutput(NmeaGpsSensor parentNmeaGpsSensor) {

        super(SENSOR_OUTPUT_NAME, parentNmeaGpsSensor, logger);

        logger.debug("Output created");
    }

    @Override
    protected void defineRecordStructure() {

        // Get an instance of SWE Factory suitable to build components
        GeoPosHelper sweFactory = new GeoPosHelper();

        Time timeStamp = sweFactory.createTime().asSamplingTimeIsoUTC().build();

        Vector location = sweFactory.createLocationVectorLLA()
                .name("location")
                .label("Location")
                .description("Current LLA position of the platform as reported by GPS")
                .build();

        Category covarianceType = sweFactory.createCategory()
                .name("covarianceType")
                .label("Covariance Type")
                .definition(SWEHelper.getPropertyUri("PositionCovarianceType"))
                .description("The type of measured positional covariance")
                .addAllowedValues(covarianceTypes)
                .build();

        Quantity covariance = sweFactory.createQuantity()
                .name("covariance")
                .label("Covariance")
                .definition(SWEHelper.getPropertyUri("Covariance"))
                .description("Observed covariance")
                .uom("m^2")
                .dataType(DataType.DOUBLE)
                .build();

        DataArray positionCovariance = sweFactory.createArray()
                .name("positionCovariance")
                .label("Position Covariance")
                .definition(SWEHelper.getPropertyUri("PositionCovariance"))
                .description("Position Covariance [m^2] defined relative to a tangential plane " +
                        "through the reported position. The components are East, North, UP (ENU), " +
                        "in row-major order")
                .withElement(covariance.getName(), covariance)
                .withFixedSize(9)
                .build();

        // Create data record description
        dataStruct = sweFactory.createRecord()
                .name(getName())
                .updatable(true)
                .definition(SWEHelper.getPropertyUri(SENSOR_OUTPUT_NAME))
                .label(SENSOR_OUTPUT_LABEL)
                .description(SENSOR_OUTPUT_DESCRIPTION)
                .addField("SampleTime", timeStamp)
                .addField(covarianceType.getName(), covarianceType)
                .addField(location.getName(), location)
                .addField(positionCovariance.getName(), positionCovariance)
                .build();

        dataEncoding = sweFactory.newTextEncoding(",", "\n");
    }

    @Override
    public void onNewMessage(Object object) {

        updateSamplingPeriodHistogram();

        NavSatFix navSatFix = (NavSatFix) object;

        DataBlock dataBlock;
        if (latestRecord == null) {

            dataBlock = dataStruct.createDataBlock();

        } else {

            dataBlock = latestRecord.renew();
        }

        // Populate data block
        dataBlock.setDoubleValue(0, System.currentTimeMillis() / 1000.0);
        dataBlock.setStringValue(1, covarianceTypes[navSatFix.getPositionCovarianceType()]);
        ((DataBlockMixed) dataBlock).getUnderlyingObject()[2].setDoubleValue(0, navSatFix.getLatitude());
        ((DataBlockMixed) dataBlock).getUnderlyingObject()[2].setDoubleValue(1, navSatFix.getLongitude());
        ((DataBlockMixed) dataBlock).getUnderlyingObject()[2].setDoubleValue(2, navSatFix.getAltitude());
        ((DataBlockMixed) dataBlock).getUnderlyingObject()[3].setUnderlyingObject(navSatFix.getPositionCovariance());

        latestRecord = dataBlock;

        latestRecordTime = System.currentTimeMillis();

        eventHandler.publish(new DataEvent(latestRecordTime, NmeaGpsOutput.this, dataBlock));
    }

    @Override
    public void doInit() {

        logger.debug("Initializing Output");

        defineRecordStructure();

        nodeMainExecutor = DefaultNodeMainExecutor.newDefault();

        subscriberNode = new RosSubscriberNode(NODE_NAME_STR, TOPIC_STR, NavSatFix._TYPE, this);

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
