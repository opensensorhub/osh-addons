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
import org.sensorhub.impl.sensor.uas.klv.UasDataLinkSet;
import org.sensorhub.misb.stanag4609.tags.TagSet;
import net.opengis.swe.v20.DataBlock;
import org.sensorhub.api.data.DataEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Output specification and provider for MISB-TS STANAG 4609 ST0601.16 UAS Metadata
 *
 * @author Nick Garay
 * @since Oct. 6, 2020
 */
public class Identification extends UasOutput {

    private static final String SENSOR_OUTPUT_NAME = "platformInfo";
    private static final String SENSOR_OUTPUT_LABEL = "Platform Identification";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "Platform identification as decoded from MISB ST0601 metadata";

    private static final Logger logger = LoggerFactory.getLogger(Identification.class);

    /**
     * Constructor
     *
     * @param parentSensor Sensor driver providing this output
     */
    public Identification(UasSensor parentSensor) {

        super(SENSOR_OUTPUT_NAME, parentSensor);

        logger.debug("Identification created");
    }

    @Override
    public void init() {

        logger.debug("Initializing Identification");

        // Get an instance of SWE Factory suitable to build components
        UasHelper sweFactory = new UasHelper();

        // Create a new data structure to build record
        dataStruct = sweFactory.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_LABEL).description(SENSOR_OUTPUT_DESCRIPTION)
                .addField("time", sweFactory.createTimeStamp())
                .addField("designation", sweFactory.createPlatformDesignation())
                .addField("tailNumber", sweFactory.createPlatformTailNumber())
                .build();

        dataEncoding = sweFactory.newTextEncoding(",", "\n");

        logger.debug("Initializing Identification Complete");
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    protected void setData(DataBlock dataBlock, TagSet localSet, int localSetTag, Object value) {

        if (localSet == UasDataLinkSet.UAS_LOCAL_SET) {
            
            switch (localSetTag) {
    
                case 0x02: // "Precision Time Stamp", "Timestamp for all metadata in this Local Set; used to coordinate with Motion Imagery", "microseconds"
                    dataBlock.setDoubleValue(0, (Double) value);
                    break;
    
                case 0x0A: // "Platform Designation", "Model name for the platform"
                    dataBlock.setStringValue(1, (String) value);
                    break;
    
                case 0x04: // "Platform Tail Number", "Identifier of platform as posted"
                    dataBlock.setStringValue(2, (String) value);
                    break;
    
                default:
                    break;
            }
        }
    }

    @Override
    protected void publish(DataBlock dataBlock) {

        eventHandler.publish(new DataEvent(latestRecordTime, Identification.this, dataBlock));
    }
}
