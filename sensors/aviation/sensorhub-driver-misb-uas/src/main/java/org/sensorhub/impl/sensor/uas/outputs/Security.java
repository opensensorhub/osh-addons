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
import org.sensorhub.impl.sensor.uas.klv.SecurityLocalSet;
import org.sensorhub.impl.sensor.uas.klv.UasDataLinkSet;
import org.sensorhub.misb.stanag4609.tags.TagSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opengis.swe.v20.DataBlock;

/**
 * Output specification and provider for MISB-TS STANAG 4609 ST0601.16 UAS Metadata
 *
 * @author Nick Garay
 * @since Oct. 6, 2020
 */
public class Security<UasConfigType extends UasConfig> extends UasOutput<UasConfigType> {

    private static final String SENSOR_OUTPUT_NAME = "Security";
    private static final String SENSOR_OUTPUT_LABEL = "Classification Markings";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "Classification Markings as Decoded from MPEG-TS MISB STANAG 4609 Metadata";

    private static final Logger logger = LoggerFactory.getLogger(Security.class);

    /**
     * Constructor
     *
     * @param parentSensor Sensor driver providing this output
     */
    public Security(UasSensorBase<UasConfigType> parentSensor) {

        super(SENSOR_OUTPUT_NAME, parentSensor);

        logger.debug("Security created");
    }

    @Override
    public void init() {

        logger.debug("Initializing Security");

        // Get an instance of SWE Factory suitable to build components
        UasHelper sweFactory = new UasHelper();

        // Create a new data structure to build record
        dataStruct = sweFactory.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_LABEL).description(SENSOR_OUTPUT_DESCRIPTION)
                .addField("time", sweFactory.createTimeStamp())
                .addField("security", sweFactory.createSecurityDataRecord())
                .build();

        dataEncoding = sweFactory.newTextEncoding(",", "\n");

        logger.debug("Initializing Security Complete");
    }

    @Override
    protected void setData(DataBlock dataBlock, TagSet localSet, int localSetTag, Object value) {

        if (localSet == UasDataLinkSet.UAS_LOCAL_SET)
        {
            // "Precision Time Stamp", "Timestamp for all metadata in this Local Set; used to coordinate with Motion Imagery", "microseconds"
            if (0x02 == localSetTag) {
    
                dataBlock.setDoubleValue(0, (Double) value);
            }
        }
        else if (localSet == SecurityLocalSet.SECURITY_LOCAL_SET)
        {
            setSecurityData(dataBlock, localSetTag, value);
        }
    }

    protected void setSecurityData(DataBlock dataBlock, int localSetTag, Object value) {

        // "Security Local Set", "MISB ST 0102 local let Security Metadata items"
        switch (localSetTag) {

            case 0x01: // Security Classification
                dataBlock.setStringValue(1, (String) value);
                break;

            case 0x02: // Classifying Country and Releasing Instructions Country Coding Method
                dataBlock.setStringValue(2, (String) value);
                break;

            case 0x03: // Classifying Country
                dataBlock.setStringValue(3, (String) value);
                break;

            case 0x04: // Security-SCI/SHI Information
                dataBlock.setStringValue(4, (String) value);
                break;

            case 0x05: // Caveats
                dataBlock.setStringValue(5, (String) value);
                break;

            case 0x06: // Releasing Instructions
                dataBlock.setStringValue(6, (String) value);
                break;

            case 0x07: // Classified By
                dataBlock.setStringValue(7, (String) value);
                break;

            case 0x08: // Derived From
                dataBlock.setStringValue(8, (String) value);
                break;

            case 0x09: // Classification Reason
                dataBlock.setStringValue(9, (String) value);
                break;

            case 0x0A: // Declassification Date
                dataBlock.setStringValue(10, (String) value);
                break;

            case 0x0B: // Classification and Marking System
                dataBlock.setStringValue(11, (String) value);
                break;

            case 0x0C: // Object Country Coding Method
                dataBlock.setStringValue(12, (String) value);
                break;

            case 0x0D: // Object Country Codes
                dataBlock.setStringValue(13, (String) value);
                break;

            case 0x0E: // Classification Comments
                dataBlock.setStringValue(14, (String) value);
                break;

            case 0x16: // Version
                dataBlock.setStringValue(15, (String) value);
                break;

            case 0x17: // Classifying Country and Releasing Instructions Country Coding Method Version Date
                dataBlock.setStringValue(16, (String) value);
                break;

            case 0x18: // Object Country Coding Method Version Date
                dataBlock.setStringValue(17, (String) value);
                break;

            case 0x0F: // Unique Material Identifier Video
            case 0x10: // Unique Material Identifier Audio
            case 0x11: // Unique Material Identifier Data
            case 0x12: // Unique Material Identifier System
            case 0x13: // Stream Id
            case 0x14: // Transport Stream Id
            case 0x15: // Item Designator Id (16 byte)
            default:
                break;
        }
    }

    @Override
    protected void publish(DataBlock dataBlock) {

        eventHandler.publish(new DataEvent(latestRecordTime, Security.this, dataBlock));
    }
}
