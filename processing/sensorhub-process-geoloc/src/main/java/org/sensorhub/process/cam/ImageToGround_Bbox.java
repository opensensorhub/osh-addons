/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.process.cam;

import net.opengis.swe.v20.Count;
import net.opengis.swe.v20.DataArray;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.vast.process.ProcessException;
import org.vast.process.ProcessInfo;
import org.vast.swe.helper.GeoPosHelper;


/**
 * <p>
 * Transforms bboxes in image space to 4-sided polygons approximating the
 * corresponding geographic ROI on the ground, taking into account the full
 * camera model and intersecting with the earth ellipsoid.
 * </p>
 *
 * @author Alex Robin
 * @since Jun 11, 2021
 */
public class ImageToGround_Bbox extends ImageToGround
{
    public static final OSHProcessInfo INFO = new OSHProcessInfo("geoloc:ImageBboxToGround", "Image to Ground (BBOX)", "Compute ground location of a bbox corners knowing their image coordinates", ImageToGround_Bbox.class);
    
    protected Count numInputBboxes;
    protected DataArray bboxesIn;
    protected Count numOutputRois;
    protected DataArray roisOut;

    public ImageToGround_Bbox()
    {
        this(INFO);
    }
    
    
    public ImageToGround_Bbox(ProcessInfo info)
    {
        super(info);
        var swe = new GeoPosHelper();
        
        // inputs
        inputData.clear();        
        inputData.add("image_rois", swe.createRecord()
            .label("Image ROIs")
            .description("Rectangular regions of interest in image space")
            .addField("numRois", numInputBboxes = swe.createCount()
                .id("NUM_ROIS")
                .build())
            .addField("bboxList", bboxesIn = swe.createArray()
                .withSizeComponent(numInputBboxes)
                .withElement("bbox", swe.createRecord()
                    .addField("x", swe.createCount()
                        .description("X coordinate of upper left corner, in pixels"))
                    .addField("y", swe.createCount()
                        .description("Y coordinate of upper left corner, in pixels"))
                    .addField("width", swe.createCount()
                        .description("Bbox width in pixels"))
                    .addField("height", swe.createCount()
                        .description("Bbox height in pixels")))
                .build())
            .build());
        
        // outputs
        outputData.clear();
        outputData.add("ground_rois", swe.createRecord()
            .label("Ground ROIs")
            .description("Projections of image space ROIs on the ground")
            .addField("numRois", numOutputRois = swe.createCount()
                .id("NUM_ROIS")
                .build())
            .addField("roiList", roisOut = swe.createArray()
                .withSizeComponent(numOutputRois)
                .withElement("bbox", swe.createRecord()
                    .addField("center", swe.createLocationVectorLLA()
                        .description("Ground location of image center"))
                    .addField("ul", swe.createLocationVectorLLA()
                        .description("Ground location of upper left corner"))
                    .addField("ur", swe.createLocationVectorLLA()
                        .description("Ground location of upper right corner"))
                    .addField("lr", swe.createLocationVectorLLA()
                        .description("Ground location of lower right corner"))
                    .addField("ll", swe.createLocationVectorLLA()
                        .description("Ground location of lower left corner")))
                .build())
            .build());
    }
    
    
    @Override
    public void execute() throws ProcessException
    {
        readPositionParams();
        
        // wait until platform location has been received
        var llaData = platformLocParam.getData();
        if (llaData.getDoubleValue(0) == 0.0 && llaData.getDoubleValue(1) == 0.0)
            return;            
            
        // get bbox coordinates input
        int numBboxes = numInputBboxes.getData().getIntValue();
        roisOut.updateSize(numBboxes);
        int bboxDataIdx = 0;
        int roisDataIdx = 0;
        for (int i = 0; i < numBboxes; i++)
        {
            var x = bboxesIn.getData().getDoubleValue(bboxDataIdx++);
            var y = bboxesIn.getData().getDoubleValue(bboxDataIdx++);
            var w = bboxesIn.getData().getDoubleValue(bboxDataIdx++);
            var h = bboxesIn.getData().getDoubleValue(bboxDataIdx++);
            
            // center
            toGroundLocation(x + w/2., y + h/2., intersect);
            roisOut.getData().setDoubleValue(roisDataIdx++, Math.toDegrees(intersect.y));
            roisOut.getData().setDoubleValue(roisDataIdx++, Math.toDegrees(intersect.x));
            roisOut.getData().setDoubleValue(roisDataIdx++, Math.toDegrees(intersect.z));
            
            // upper left corner
            toGroundLocation(x, y, intersect);
            roisOut.getData().setDoubleValue(roisDataIdx++, Math.toDegrees(intersect.y));
            roisOut.getData().setDoubleValue(roisDataIdx++, Math.toDegrees(intersect.x));
            roisOut.getData().setDoubleValue(roisDataIdx++, Math.toDegrees(intersect.z));
            
            // upper right corner
            toGroundLocation(x+w, y, intersect);
            roisOut.getData().setDoubleValue(roisDataIdx++, Math.toDegrees(intersect.y));
            roisOut.getData().setDoubleValue(roisDataIdx++, Math.toDegrees(intersect.x));
            roisOut.getData().setDoubleValue(roisDataIdx++, Math.toDegrees(intersect.z));
            
            // lower right corner
            toGroundLocation(x+w, y+h, intersect);
            roisOut.getData().setDoubleValue(roisDataIdx++, Math.toDegrees(intersect.y));
            roisOut.getData().setDoubleValue(roisDataIdx++, Math.toDegrees(intersect.x));
            roisOut.getData().setDoubleValue(roisDataIdx++, Math.toDegrees(intersect.z));
            
            // lower left corner
            toGroundLocation(x, y+h, intersect);
            roisOut.getData().setDoubleValue(roisDataIdx++, Math.toDegrees(intersect.y));
            roisOut.getData().setDoubleValue(roisDataIdx++, Math.toDegrees(intersect.x));
            roisOut.getData().setDoubleValue(roisDataIdx++, Math.toDegrees(intersect.z));
        }
    }
}
