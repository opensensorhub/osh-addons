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

import org.vast.swe.SWEBuilders.DataArrayBuilder;
import org.vast.swe.SWEBuilders.VectorBuilder;
import org.vast.swe.helper.RasterHelper;
import net.opengis.swe.v20.Count;


/**
 * <p>
 * Helper class for creating CV related data structures for use as inputs
 * and outputs of processing blocks.
 * </p>
 *
 * @author Alex Robin
 * @since Jun 20, 2021
 */
public class CVHelper extends RasterHelper
{

    /**
     * Create a BBOX record with minX, minY, width and height
     * @return The builder for chaining
     */
    public VectorBuilder createBbox()
    {
        return createVector()
            .refFrame(DEF_GRID_CRS_2D)
            .addCoordinate("x", createCount()
                .definition(DEF_COORD)
                .axisId("i")
                .description("X coordinate of upper left corner, in pixels"))
            .addCoordinate("y", createCount()
                .definition(DEF_COORD)
                .axisId("j")
                .description("Y coordinate of upper left corner, in pixels"))
            .addCoordinate("width", createCount()
                .definition(DEF_RASTER_WIDTH)
                .axisId("i")
                .description("Bbox width in pixels"))
            .addCoordinate("height", createCount()
                .definition(DEF_RASTER_HEIGHT)
                .axisId("j")
                .description("Bbox height in pixels"));
    }
    
    
    /**
     * Create a DataArray containing a list of BBOX objects
     * @param sizeComponent Size component for the DataArray (can be fixed or variable size)
     * @return The builder for chaining
     */
    public DataArrayBuilder createBboxList(Count sizeComponent)
    {
        return createArray()
            .withSizeComponent(sizeComponent)
            .withElement("bbox", createBbox());
    }
}
