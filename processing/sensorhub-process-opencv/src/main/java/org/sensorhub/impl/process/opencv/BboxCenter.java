/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.process.opencv;

import net.opengis.swe.v20.Count;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.Vector;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;
import org.vast.swe.helper.RasterHelper;


/**
 * <p>
 * Simple process to extract the center of an image plane bounding box.
 * </p>
 *
 * @author Alex Robin
 * @date Jun 10, 2021
 */
public class BboxCenter extends ExecutableProcessImpl
{
	public static final OSHProcessInfo INFO = new OSHProcessInfo("opencv:BboxCenter", "Extract center of bbox from list", null, BboxCenter.class);
    
	Count numInputBboxes;
    DataArray bboxesIn;
    Vector bboxCenterOut;    
    
    
    public BboxCenter()
    {
    	super(INFO);
        RasterHelper swe = new RasterHelper();
    	
        // inputs
    	inputData.add("rois", swe.createRecord()
            .label("Image Bboxes")
            .description("Rectangular image areas containing objects of interest")
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
        outputData.add("bboxCenter", bboxCenterOut = swe.createGridCoordinates2D()
            .label("Bbox Center")
            .build());
    }
    

    @Override
    public void execute() throws ProcessException
    {
        double cx = Double.NaN;
        double cy = Double.NaN;
        
        if (numInputBboxes.getData().getIntValue() > 0)
        {
            var x = bboxesIn.getData().getDoubleValue(0);
            var y = bboxesIn.getData().getDoubleValue(1);
            var w = bboxesIn.getData().getDoubleValue(2);
            var h = bboxesIn.getData().getDoubleValue(3);
            
            cx = x + w/2.;
            cy = y + h/2.;
        }
        
        bboxCenterOut.getData().setDoubleValue(0, cx);
        bboxCenterOut.getData().setDoubleValue(1, cy);
    } 
}