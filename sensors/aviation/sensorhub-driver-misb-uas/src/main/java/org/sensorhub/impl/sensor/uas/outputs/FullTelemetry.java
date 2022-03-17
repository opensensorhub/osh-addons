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
import org.sensorhub.impl.sensor.uas.common.ITimeSynchronizedUasDataProducer;
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
 * @since Feb. 6, 2020
 */
public class FullTelemetry extends UasOutput {

    private static final String SENSOR_OUTPUT_NAME = "UasTelemetry";
    private static final String SENSOR_OUTPUT_LABEL = "UAS Telemetry";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "Decoded MPEG-TS MISB STANAG 4609 Metadata";

    private static final Logger logger = LoggerFactory.getLogger(FullTelemetry.class);

    /**
     * Constructor
     *
     * @param parentSensor Sensor driver providing this output
     */
    public FullTelemetry(ITimeSynchronizedUasDataProducer parentSensor) {

        super(SENSOR_OUTPUT_NAME, parentSensor);

        logger.debug("FullTelemetry created");
    }

    @Override
    public void init() {

        logger.debug("Initializing FullTelemetry");

        // Get an instance of SWE Factory suitable to build components
        UasHelper sweFactory = new UasHelper();

        // Create a new data structure to build record
        dataStruct = sweFactory.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_LABEL)
                .description(SENSOR_OUTPUT_DESCRIPTION)
                .addField("time", sweFactory.createTimeStamp())
                .addField("dataLinkVersion", sweFactory.createDataLinkVersion())
                .addField("platformDesignation", sweFactory.createPlatformDesignation())
                .addField("security", sweFactory.createSecurityDataRecord())
                .addField("platformTailNumber", sweFactory.createPlatformTailNumber())
                .addField("sensorLocation", sweFactory.createSensorLocation())
                .addField("sensorParams", sweFactory.createSensorParams())
                .addField("geoRefImageFrame", sweFactory.createImageFrame())
                .addField("imageSourceSensor", sweFactory.createImageSourceSensor())
                .addField("imageCoordinateSystem", sweFactory.createImageCoordinateSystem())
                .addField("slantRange", sweFactory.createSlantRange())
                .addField("sensorAttitude", sweFactory.createSensorAttitude())
                .addField("platformHPR", sweFactory.createPlatformHPR())
                .addField("platformGroundSpeed", sweFactory.createPlatformGroundSpeed())
                .build();

        dataEncoding = sweFactory.newTextEncoding(",", "\n");

        logger.debug("Initializing FullTelemetry Complete");
    }

    @Override
    protected void setData(DataBlock dataBlock, TagSet localSet, int localSetTag, Object value) {

        if (localSet == UasDataLinkSet.UAS_LOCAL_SET) {
            
            switch (localSetTag) {
    
                case 0x01: // "Checksum"
                    break;
    
                case 0x02: // "Precision Time Stamp", "Timestamp for all metadata in this Local Set; used to coordinate with Motion Imagery", "microseconds"
                    dataBlock.setDoubleValue(0, (Double) value);
                    break;
    
                case 0x41: // "UAS Datalink LS Version Number", "Version number of the UAS Datalink LS document used to generate KLV metadata"
                    dataBlock.setIntValue(1, (Integer) value);
                    break;
    
                case 0x0A: // "Platform Designation", "Model name for the platform"
                    dataBlock.setStringValue(2, (String) value);
                    break;
    
                case 0x30: // "Security Local Set", "MISB ST 0102 local let Security Metadata items"
                    break;
    
                case 0x04: // "Platform Tail Number", "Identifier of platform as posted"
                    dataBlock.setStringValue(20, (String) value);
                    break;
    
                case 0x0D: // "Sensor Latitude", "Sensor latitude", "deg"
                    dataBlock.setDoubleValue(21, (Double) value);
                    break;
    
                case 0x0E: // "Sensor Longitude", "Sensor longitude", "deg"
                    dataBlock.setDoubleValue(22, (Double) value);
                    break;
    
                case 0x0F: // "Sensor True Altitude", "Altitude of sensor as measured from Mean Sea Level (MSL)", "m"
                    dataBlock.setDoubleValue(23, (Double) value);
                    break;
    
                case 0x10: // "Sensor Horizontal Field of View", "Horizontal field of view of selected imaging sensor", "deg"
                    dataBlock.setDoubleValue(24, (Double) value);
                    break;
    
                case 0x11: // "Sensor Vertical Field of View", "Vertical field of view of selected imaging sensor", "deg"
                    dataBlock.setDoubleValue(25, (Double) value);
                    break;
    
                case 0x17: // "Frame Center Latitude", "Terrain latitude of frame center", "deg"
                    dataBlock.setDoubleValue(26, (Double) value);
                    break;
    
                case 0x18: // "Frame Center Longitude", "Terrain longitude of frame center", "deg"
                    dataBlock.setDoubleValue(27, (Double) value);
                    break;
    
                case 0x19: // "Frame Center Elevation", "Terrain elevation at frame center relative to Mean Sea Level (MSL)", "m"
                    dataBlock.setDoubleValue(28, (Double) value);
                    break;
    
                case 0x1A: // "Offset Corner Latitude Point 1", "Frame latitude offset for upper left corner", "deg"
                    dataBlock.setDoubleValue(29, (Double) value);
                    break;
    
                case 0x1B: // "Offset Corner Longitude Point 1", "Frame longitude offset for upper left corner", "deg"
                    dataBlock.setDoubleValue(30, (Double) value);
                    break;
    
                case 0x1C: // "Offset Corner Latitude Point 2", "Frame latitude offset for upper right corner", "deg"
                    dataBlock.setDoubleValue(31, (Double) value);
                    break;
    
                case 0x1D: // "Offset Corner Longitude Point 2", "Frame longitude offset for upper right corner", "deg"
                    dataBlock.setDoubleValue(32, (Double) value);
                    break;
    
                case 0x1E: // "Offset Corner Latitude Point 3", "Frame latitude offset for lower right corner", "deg"
                    dataBlock.setDoubleValue(33, (Double) value);
                    break;
    
                case 0x1F: // "Offset Corner Longitude Point 3", "Frame longitude offset for lower right corner", "deg"
                    dataBlock.setDoubleValue(34, (Double) value);
                    break;
    
                case 0x20: // "Offset Corner Latitude Point 4", "Frame latitude offset for lower left corner", "deg"
                    dataBlock.setDoubleValue(35, (Double) value);
                    break;
    
                case 0x21: // "Offset Corner Longitude Point 4", "Frame longitude offset for lower left corner", "deg"
                    dataBlock.setDoubleValue(36, (Double) value);
                    break;
    
                case 0x0B: // "Image Source Sensor", "Name of currently active sensor"
                    dataBlock.setStringValue(37, (String) value);
                    break;
    
                case 0x0C: // "Image Coordinate System", "Name of the image coordinate system used"
                    dataBlock.setStringValue(38, (String) value);
                    break;
    
                case 0x15: // "Slant Range", "Slant range in meters", "m"
                    dataBlock.setDoubleValue(39, (Double) value);
                    break;
    
                case 0x12: // "Sensor Relative Azimuth Angle", "Relative rotation angle of sensor to platform longitudinal axis", "deg"
                    dataBlock.setDoubleValue(40, (Double) value);
                    break;
    
                case 0x13: // "Sensor Relative Elevation Angle", "Relative elevation angle of sensor to platform longitudinal-transverse plane", "deg"
                    dataBlock.setDoubleValue(41, (Double) value);
                    break;
    
                case 0x14: // "Sensor Relative Roll Angle", "Relative roll angle of sensor to aircraft platform", "deg"
                    dataBlock.setDoubleValue(42, (Double) value);
                    break;
    
                case 0x05: // "Platform Heading Angle", "Aircraft heading angle", "deg"
                    dataBlock.setDoubleValue(43, (Double) value);
                    break;
    
                case 0x06: // "Platform Pitch Angle", "Aircraft pitch angle", "deg"
                    dataBlock.setDoubleValue(44, (Double) value);
                    break;
    
                case 0x07: // "Platform Roll Angle", "Platform roll angle", "deg"
                    dataBlock.setDoubleValue(45, (Double) value);
                    break;
    
                case 0x38: // "Platform Ground Speed", "Speed projected to the ground of an airborne platform passing overhead", "m/s"
                    dataBlock.setIntValue(46, (int) value);
                    break;
    
                default:
                    break;
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
                dataBlock.setStringValue(3, (String) value);
                break;

            case 0x02: // Classifying Country and Releasing Instructions Country Coding Method
                dataBlock.setStringValue(4, (String) value);
                break;

            case 0x03: // Classifying Country
                dataBlock.setStringValue(5, (String) value);
                break;

            case 0x04: // Security-SCI/SHI Information
                dataBlock.setStringValue(6, (String) value);
                break;

            case 0x05: // Caveats
                dataBlock.setStringValue(7, (String) value);
                break;

            case 0x06: // Releasing Instructions
                dataBlock.setStringValue(8, (String) value);
                break;

            case 0x07: // Classified By
                dataBlock.setStringValue(9, (String) value);
                break;

            case 0x08: // Derived From
                dataBlock.setStringValue(10, (String) value);
                break;

            case 0x09: // Classification Reason
                dataBlock.setStringValue(11, (String) value);
                break;

            case 0x0A: // Declassification Date
                dataBlock.setStringValue(12, (String) value);
                break;

            case 0x0B: // Classification and Marking System
                dataBlock.setStringValue(13, (String) value);
                break;

            case 0x0C: // Object Country Coding Method
                dataBlock.setStringValue(14, (String) value);
                break;

            case 0x0D: // Object Country Codes
                dataBlock.setStringValue(15, (String) value);
                break;

            case 0x0E: // Classification Comments
                dataBlock.setStringValue(16, (String) value);
                break;

            case 0x16: // Version
                dataBlock.setStringValue(17, (String) value);
                break;

            case 0x17: // Classifying Country and Releasing Instructions Country Coding Method Version Date
                dataBlock.setStringValue(18, (String) value);
                break;

            case 0x18: // Object Country Coding Method Version Date
                dataBlock.setStringValue(19, (String) value);
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

        eventHandler.publish(new DataEvent(latestRecordTime, FullTelemetry.this, dataBlock));
    }
}
