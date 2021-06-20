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
import java.awt.Color;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.sensorhub.impl.process.video.VideoDisplay;
import org.vast.process.ProcessException;


/**
 * <p>
 * Video display window for testing CV algorithms.<br/>
 * It extends VideoDisplay to allow displaying not only the decoded RGB video
 * frames but also object bounding boxes and info.
 * </p>
 *
 * @author Alex Robin
 * @date Jun 7, 2021
 */
public class VideoDisplayCV extends VideoDisplay
{
	public static final OSHProcessInfo INFO = new OSHProcessInfo("opencv:VideoDisplayCV", "CV Video Display Window", null, VideoDisplayCV.class);

    Count numRois;
	DataArray bboxes;
	
    
    public VideoDisplayCV()
    {
    	super(INFO);
        var swe = new CVHelper();
        
        inputData.add("rois", swe.createRecord()
            .label("Regions of Interest")
            .addField("numRois", numRois = swe.createCount()
                .id("NUM_ROIS")
                .build())
            .addField("bboxList", bboxes = swe.createBboxList(numRois)
                .build())
            .build());
    }
    

    @Override
    public void execute() throws ProcessException
    {
        super.execute();
        
        // draw object bounding boxes
        graphicCtx.setColor(Color.GREEN);
        var numBbox = numRois.getData().getIntValue();
        var bboxData = bboxes.getData();
        
        int idx = 0;
        for (int i = 0; i < numBbox; i++)
        {
            graphicCtx.drawRect(
                bboxData.getIntValue(idx++),  // x
                bboxData.getIntValue(idx++),  // y
                bboxData.getIntValue(idx++),  // width
                bboxData.getIntValue(idx++)); // height
        }
    }
            
            
    @Override
    public void dispose()
    {
        super.dispose();
    }
}