/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.uas.outputs;

import org.sensorhub.impl.sensor.uas.UasSensor;
import net.opengis.swe.v20.DataBlock;
import org.sensorhub.api.data.DataEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.SWEHelper;

/**
 * Output specification and provider for MISB-TS STANAG 4609 ST0601.16 UAS Metadata
 *
 * @author Nick Garay
 * @since Oct. 6, 2020
 */
public class AirframePosition extends UasOutput {

    private static final String SENSOR_OUTPUT_NAME = "AirframePosition";
    private static final String SENSOR_OUTPUT_LABEL = "Airframe Position";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "Position as Decoded from MPEG-TS MISB STANAG 4609 Metadata";

    private static final Logger logger = LoggerFactory.getLogger(AirframePosition.class);

    /**
     * Constructor
     *
     * @param parentSensor Sensor driver providing this output
     */
    public AirframePosition(UasSensor parentSensor) {

        super(SENSOR_OUTPUT_NAME, parentSensor);

        logger.debug("AirframePosition created");
    }

    @Override
    public void init() {

        logger.debug("Initializing AirframePosition");

        // Get an instance of SWE Factory suitable to build components
        UasHelper sweFactory = new UasHelper();

        // Create a new data structure to build record
        dataStruct = sweFactory.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_LABEL).description(SENSOR_OUTPUT_DESCRIPTION)
                .definition(SWEHelper.getPropertyUri(SENSOR_OUTPUT_NAME))
                .addField("time", sweFactory.createTimeStamp())
                .addField("platformLocation", sweFactory.createPlatformLocation())
                .addField("platformHPR", sweFactory.createPlatformHPR())
                .build();

        dataEncoding = sweFactory.newTextEncoding(",", "\n");

        logger.debug("Initializing AirframePosition Complete");
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    protected void setData(DataBlock dataBlock, int localSetTag, Object value) {

        switch (localSetTag) {

            case 0x02: // "Precision Time Stamp", "Timestamp for all metadata in this Local Set; used to coordinate with Motion Imagery", "microseconds"
                dataBlock.setDoubleValue(0, (Double) value);
                break;

            case 0x0D: // "Sensor Latitude", "Sensor latitude", "deg"
                dataBlock.setDoubleValue(1, (Double) value);
                break;

            case 0x0E: // "Sensor Longitude", "Sensor longitude", "deg"
                dataBlock.setDoubleValue(2, (Double) value);
                break;

            case 0x0F: // "Sensor True Altitude", "Altitude of sensor as measured from Mean Sea Level (MSL)", "m"
                dataBlock.setDoubleValue(3, (Double) value);
                break;

            case 0x05: // "Platform Heading Angle", "Aircraft heading angle", "deg"
                dataBlock.setDoubleValue(4, (Double) value);
                break;

            case 0x06: // "Platform Pitch Angle", "Aircraft pitch angle", "deg"
                dataBlock.setDoubleValue(5, (Double) value);
                break;

            case 0x07: // "Platform Roll Angle", "Platform roll angle", "deg"
                dataBlock.setDoubleValue(6, (Double) value);
                break;

            default:
                break;
        }
    }

    @Override
    protected void publish(DataBlock dataBlock) {

        eventHandler.publish(new DataEvent(latestRecordTime, AirframePosition.this, dataBlock));
    }
}
