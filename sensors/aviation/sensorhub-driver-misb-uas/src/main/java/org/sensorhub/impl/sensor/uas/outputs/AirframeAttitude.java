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

import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.uas.UasSensorBase;
import org.sensorhub.impl.sensor.uas.config.UasConfig;
import org.sensorhub.impl.sensor.uas.klv.UasDataLinkSet;
import org.sensorhub.misb.stanag4609.tags.TagSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opengis.swe.v20.DataBlock;

/**
 * Output specification and provider for MISB-TS STANAG 4609 ST0601.16 UAS Metadata
 *
 * @param <UasConfigType> A type parameter that allows us to use this output on both types of sensors that are defined
 *   in this module.
 *
 * @author Nick Garay
 * @since Oct. 6, 2020
 */
public class AirframeAttitude<UasConfigType extends UasConfig> extends UasOutput<UasConfigType> {

    static final String SENSOR_OUTPUT_NAME = "platformAttitude";
    private static final String SENSOR_OUTPUT_LABEL = "Platform Attitude";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "Attitude/orientation of UAS platform around its center of mass";

    private static final Logger logger = LoggerFactory.getLogger(AirframeAttitude.class);

    /**
     * Constructor
     *
     * @param parentSensor Sensor driver providing this output
     */
    public AirframeAttitude(UasSensorBase<UasConfigType> parentSensor) {

        super(SENSOR_OUTPUT_NAME, parentSensor);

        logger.debug("AirframeAttitude created");
    }

    @Override
    public void init() {

        logger.debug("Initializing AirframeAttitude");

        // Get an instance of SWE Factory suitable to build components
        UasHelper sweFactory = new UasHelper();

        // Create a new data structure to build record
        dataStruct = sweFactory.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_LABEL).description(SENSOR_OUTPUT_DESCRIPTION)
                .addField("time", sweFactory.createTimeStamp())
                .addField("attitude", sweFactory.createPlatformHPR())
                .build();

        dataEncoding = sweFactory.newTextEncoding(",", "\n");

        logger.debug("Initializing AirframeAttitude Complete");
    }

    @Override
    protected void setData(DataBlock dataBlock, TagSet localSet, int localSetTag, Object value) {

        if (localSet == UasDataLinkSet.UAS_LOCAL_SET) {
            
            switch (localSetTag) {
    
                case 0x02: // "Precision Time Stamp", "Timestamp for all metadata in this Local Set; used to coordinate with Motion Imagery", "microseconds"
                    dataBlock.setDoubleValue(0, (Double) value);
                    break;
    
                case 0x05: // "Platform Heading Angle", "Aircraft heading angle", "deg"
                    dataBlock.setDoubleValue(1, (Double) value);
                    break;
    
                case 0x06: // "Platform Pitch Angle", "Aircraft pitch angle", "deg"
                    dataBlock.setDoubleValue(2, (Double) value);
                    break;
    
                case 0x07: // "Platform Roll Angle", "Platform roll angle", "deg"
                    dataBlock.setDoubleValue(3, (Double) value);
                    break;
    
                default:
                    break;
            }
        }
    }

    @Override
    protected void publish(DataBlock dataBlock) {

        eventHandler.publish(new DataEvent(latestRecordTime, this, parentSensor.getUasFoiUID(), dataBlock));
    }
}
