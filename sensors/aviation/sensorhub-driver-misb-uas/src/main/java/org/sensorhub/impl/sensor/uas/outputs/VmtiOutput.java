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
import org.sensorhub.impl.sensor.uas.klv.VmtiLocalSet;
import org.sensorhub.misb.stanag4609.tags.Tag;
import org.sensorhub.misb.stanag4609.tags.TagSet;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataBlock;
import java.awt.Point;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;
import org.sensorhub.api.data.DataEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.DataBlockList;
import org.vast.data.DataBlockMixed;
import org.vast.swe.SWEConstants;

/**
 * Output specification and provider for moving target indicator (VMTI) data
 *
 * @author Alex Robin
 * @since May. 10, 2021
 */
public class VmtiOutput extends UasOutput {

    private static final String SENSOR_OUTPUT_NAME = "TargetIndicators";
    private static final String SENSOR_OUTPUT_LABEL = "Moving Target Indicators";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "Moving target indicators as decoded from MISB ST 0903 Metadata";

    private static final Logger logger = LoggerFactory.getLogger(VmtiOutput.class);
    
    int frameWidth;
    int frameHeight;
    double frameCenterLat;
    double frameCenterLon;
    DataBlock vTargetPackTemplateBlock;
    
    
    /**
     * Constructor
     *
     * @param parentSensor Sensor driver providing this output
     */
    public VmtiOutput(UasSensor parentSensor) {

        super(SENSOR_OUTPUT_NAME, parentSensor);

        logger.debug("VMTI created");
    }

    @Override
    public void init() {

        logger.debug("Initializing VMTI");

        // Get an instance of SWE Factory suitable to build components
        UasHelper swe = new UasHelper();
        String numTargetsID = "NUM_TARGETS";
        
        // Create a new data structure to build record
        dataStruct = swe.createRecord()
            .name(getName())
            .label(SENSOR_OUTPUT_LABEL).description(SENSOR_OUTPUT_DESCRIPTION)
            .addField("time", swe.createTimeStamp())
            .addField("frameNum", swe.createCount()
                .definition(UasHelper.MISB_ST0903_DEF_URI_PREFIX + "FrameNumber")
                .label("Frame Number"))
            .addField("numTargets", swe.createCount()
                .definition(UasHelper.MISB_ST0903_DEF_URI_PREFIX + "NumTargetsReported")
                .label("Number of Targets")
                .id(numTargetsID))
            .addField("targetSeries", swe.createArray()
                .definition(UasHelper.MISB_ST0903_DEF_URI_PREFIX + "TargetSeries")
                .withVariableSize(numTargetsID)
                .withElement("target", swe.createRecord()
                    .addField("id", swe.createCount()
                        .definition(UasHelper.MISB_ST0903_DEF_URI_PREFIX + "TargetId"))
                    .addField("centroid", swe.createVector()
                        .definition(UasHelper.MISB_ST0903_DEF_URI_PREFIX + "TargetCentroid")
                        .refFrame("#IMAGE_FRAME")
                        .label("Target Centroid")
                        .description("Target centroid in pixel coordinates")
                        .addCoordinate("x", swe.createCount()
                            .definition(SWEConstants.DEF_COEF)
                            .axisId("X"))
                        .addCoordinate("y", swe.createCount()
                            .definition(SWEConstants.DEF_COEF)
                            .axisId("Y")))
                    .addField("location", swe.createLocationVectorLLA()
                        .definition(UasHelper.MISB_ST0903_DEF_URI_PREFIX + "TargetLocation")
                        .label("Target Location")
                        .description("Target location in geographic coordinates"))
                    //.addField("priority", dataStruct)
                    //.addField("confidenceLevel", dataStruct)
                    //.addField("color", dataStruct)
                    //.addField("intensity", dataStruct)
                    )
                .build())
            
            
            .build();

        dataEncoding = swe.newTextEncoding(",", "\n");
        
        // compute size of vTarget record
        var vTargetSeries = (DataArray)dataStruct.getComponent("targetSeries");
        vTargetPackTemplateBlock = vTargetSeries.getElementType().createDataBlock();
            
        logger.debug("Initializing VMTI Complete");
    }

    @Override
    protected void setData(DataBlock dataBlock, TagSet localSet, int localSetTag, Object value) {

        if (localSet == UasDataLinkSet.UAS_LOCAL_SET) {
            
            switch (localSetTag) {
    
                case 0x02: // Precision Time Stamp
                    dataBlock.setDoubleValue(0, (Double)value);
                    break;
                    
                case 0x17: // Frame Center Latitude
                    this.frameCenterLat = (Double)value;
                    break;
    
                case 0x18: // Frame Center Longitude
                    this.frameCenterLon = (Double)value;
                    break;
    
                default:
                    break;
            }
        }
        else if (localSet == VmtiLocalSet.VMTI_LOCAL_SET) {
            
            switch (localSetTag) {
                
                case 2: // Precision Time Stamp
                    dataBlock.setDoubleValue(0, (Double)value);
                    break;
                    
                case 5: // Total Number of Targets Detected
                    var numTargets = ((Long)value).intValue();
                    dataBlock.setIntValue(2, numTargets);
                    break;
                    
                case 7: // Motion Imagery Frame Number
                    dataBlock.setIntValue(1, ((Long)value).intValue());
                    break;
                    
                case 8: // Frame Width
                    this.frameWidth = ((Long)value).intValue();
                    break;
                    
                case 9: // Frame Height
                    this.frameHeight = ((Long)value).intValue();
                    break;
                    
                case 101: // VTarget Series
                    @SuppressWarnings("unchecked")
                    var targetSeries = (Collection<HashMap<Tag, Object>>)value;
                    
                    var seriesData = (DataBlockList)((DataBlockMixed)dataBlock).getUnderlyingObject()[3];
                    seriesData.resize(0);
                    
                    for (var targetPack: targetSeries) {
                        var dataBlk = vTargetPackTemplateBlock.clone();
                        setTargetSeriesData(dataBlk, targetPack);
                        seriesData.add(dataBlk);
                    }                    
                    break;
    
                default:
                    break;
            }
        }
    }
    
    protected void setTargetSeriesData(DataBlock dataBlock, HashMap<Tag, Object> targetPack) {
        for (Entry<Tag, Object> entry: targetPack.entrySet()) {
            
            int localSetTag = entry.getKey().getLocalSetTag();
            Object value = entry.getValue(); 
                
            switch (localSetTag) {
                
                case 0: // Target ID
                    dataBlock.setIntValue(0, (int)value);
                    break;
                    
                case 1: // Target Centroid
                    var p = computePixelCoords(((Long)value).intValue());
                    dataBlock.setIntValue(2, p.x);
                    dataBlock.setIntValue(1, p.y);
                    break;
                    
                case 19: // Centroid Pix Row
                    dataBlock.setIntValue(2, ((Long)value).intValue());
                    break;
                    
                case 20: // Centroid Pix Col
                    dataBlock.setIntValue(1, ((Long)value).intValue());
                    break;
    
                default:
                    break;
            }
        }
    }
    
    private Point computePixelCoords(int count) {
        int x = count / frameWidth + 1;
        int y = count % frameWidth;
        return new Point(x, y);
    }

    @Override
    protected void publish(DataBlock dataBlock) {

        eventHandler.publish(new DataEvent(latestRecordTime, this, parentSensor.getImagedFoiUID(), dataBlock));
    }
}
