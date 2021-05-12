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
import org.vast.swe.SWEHelper;

/**
 * Output specification and provider for MISB-TS STANAG 4609 ST0601.16 UAS Metadata
 *
 * @author Nick Garay
 * @since Oct. 6, 2020
 */
public class GeoRefImageFrame extends UasOutput {

    private static final String SENSOR_OUTPUT_NAME = "GeoRefImageFrame";
    private static final String SENSOR_OUTPUT_LABEL = "GeoReferenced Image Frame";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "Image Frame data geo referenced as Decoded from MPEG-TS MISB STANAG 4609 Metadata";

    private static final Logger logger = LoggerFactory.getLogger(GeoRefImageFrame.class);

    /**
     * Constructor
     *
     * @param parentSensor Sensor driver providing this output
     */
    public GeoRefImageFrame(UasSensor parentSensor) {

        super(SENSOR_OUTPUT_NAME, parentSensor);

        logger.debug("GeoRefImageFrame created");
    }

    @Override
    public void init() {

        logger.debug("Initializing GeoRefImageFrame");

        // Get an instance of SWE Factory suitable to build components
        UasHelper sweFactory = new UasHelper();

        // Create a new data structure to build record
        dataStruct = sweFactory.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_LABEL).description(SENSOR_OUTPUT_DESCRIPTION)
                .definition(SWEHelper.getPropertyUri(SENSOR_OUTPUT_NAME))
                .addField("time", sweFactory.createTimeStamp())
                .addField("geoRefImageFrame", sweFactory.createImageFrame())
                .build();

        dataEncoding = sweFactory.newTextEncoding(",", "\n");

        logger.debug("Initializing GeoRefImageFrame Complete");
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
    
                case 0x10: // "Sensor Horizontal Field of View", "Horizontal field of view of selected imaging sensor", "deg"
                    dataBlock.setDoubleValue(1, (Double) value);
                    break;
    
                case 0x11: // "Sensor Vertical Field of View", "Vertical field of view of selected imaging sensor", "deg"
                    dataBlock.setDoubleValue(2, (Double) value);
                    break;
    
                case 0x17: // "Frame Center Latitude", "Terrain latitude of frame center", "deg"
                    dataBlock.setDoubleValue(3, (Double) value);
                    break;
    
                case 0x18: // "Frame Center Longitude", "Terrain longitude of frame center", "deg"
                    dataBlock.setDoubleValue(4, (Double) value);
                    break;
    
                case 0x19: // "Frame Center Elevation", "Terrain elevation at frame center relative to Mean Sea Level (MSL)", "m"
                    dataBlock.setDoubleValue(5, (Double) value);
                    break;
    
                case 0x1A: // "Offset Corner Latitude Point 1", "Frame latitude offset for upper right corner", "deg"
                    dataBlock.setDoubleValue(6, (Double) value);
                    break;
    
                case 0x1B: // "Offset Corner Longitude Point 1", "Frame longitude offset for upper right corner", "deg"
                    dataBlock.setDoubleValue(7, (Double) value);
                    break;
    
                case 0x1C: // "Offset Corner Latitude Point 2", "Frame latitude offset for lower right corner", "deg"
                    dataBlock.setDoubleValue(8, (Double) value);
                    break;
    
                case 0x1D: // "Offset Corner Longitude Point 2", "Frame longitude offset for lower right corner", "deg"
                    dataBlock.setDoubleValue(9, (Double) value);
                    break;
    
                case 0x1E: // "Offset Corner Latitude Point 3", "Frame latitude offset for lower left corner", "deg"
                    dataBlock.setDoubleValue(10, (Double) value);
                    break;
    
                case 0x1F: // "Offset Corner Longitude Point 3", "Frame longitude offset for lower left corner", "deg"
                    dataBlock.setDoubleValue(11, (Double) value);
                    break;
    
                case 0x20: // "Offset Corner Latitude Point 4", "Frame latitude offset for upper left corner", "deg"
                    dataBlock.setDoubleValue(12, (Double) value);
                    break;
    
                case 0x21: // "Offset Corner Longitude Point 4", "Frame longitude offset for upper left corner", "deg"
                    dataBlock.setDoubleValue(13, (Double) value);
                    break;
    
                default:
                    break;
            }
        }
    }

    @Override
    protected void publish(DataBlock dataBlock) {

        eventHandler.publish(new DataEvent(latestRecordTime, GeoRefImageFrame.this, dataBlock));
    }
}
