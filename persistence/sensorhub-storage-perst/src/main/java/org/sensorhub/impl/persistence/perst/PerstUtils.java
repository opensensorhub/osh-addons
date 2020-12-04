/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence.perst;

import org.garret.perst.RectangleRn;
import org.vast.util.Bbox;
import net.opengis.gml.v32.AbstractGeometry;
import net.opengis.gml.v32.LineString;
import net.opengis.gml.v32.Point;
import net.opengis.gml.v32.Polygon;


public class PerstUtils
{
    static String GEOM_DIM_ERROR = "Only 2D and 3D geometries are supported";
    
    
    public static RectangleRn getBoundingRectangle(AbstractGeometry geom)
    {
        double[] bboxCoords = null;
        int numDims = -1;
        
        // get geom dimension if specified
        if (geom.isSetSrsDimension())
        {
            numDims = geom.getSrsDimension();
            if (numDims != 2 && numDims != 3)
                throw new IllegalArgumentException(GEOM_DIM_ERROR);
        }        
        
        // case of JTS geom
        /*if (geom instanceof Geometry)
        {
            Envelope env = ((Geometry) geom).getEnvelopeInternal();
            bboxCoords = new double[] {env.getMinX(), env.getMinY(), env.getMaxX(), env.getMaxY()};
        }*/
        
        // case of points
        if (geom instanceof Point)
        {
            double[] pos = ((Point)geom).getPos();
                        
            if (pos.length == 2)
                bboxCoords = new double[] {pos[0], pos[1], pos[0], pos[1]};
            else if (pos.length == 3)
                bboxCoords = new double[] {pos[0], pos[1], pos[2], pos[0], pos[1], pos[2]};
            else
                throw new IllegalArgumentException(GEOM_DIM_ERROR);
        }
        
        // case of polylines
        else if (geom instanceof LineString)
        {
            double[] posList = ((LineString)geom).getPosList();
            bboxCoords = getBoundingRectangle(numDims, posList);
        }
        
        // case of polygons
        else if (geom instanceof Polygon)
        {
            double[] posList = ((Polygon)geom).getExterior().getPosList();
            bboxCoords = getBoundingRectangle(numDims, posList);
        }
        
        if (bboxCoords != null)
            return new RectangleRn(bboxCoords); 
            
        return null;
    }
    
    
    public static double[] getBoundingRectangle(int numDims, double[] geomCoords)
    {
        int numPoints = geomCoords.length / numDims;
        double[] bboxCoords = new double[2*numDims];
        
        // try to guess number of dimensions if not specified
        if (numDims < 2 && geomCoords.length % 2 == 0)
            numDims = 2;
        else if (numDims < 2 && geomCoords.length % 3 == 0)
            numDims = 3;
        
        int c = 0;
        for (int p = 0; p < numPoints; p++)
        {
            for (int i = 0; i < numDims; i++, c++)
            {
                double val = geomCoords[c];
                int imax = i + numDims;
                
                if (p == 0)
                {
                    bboxCoords[i] = val;
                    bboxCoords[imax] = val;
                }
                else
                {
                    if (val < bboxCoords[i])
                        bboxCoords[i] = val;
                    if (val > bboxCoords[imax])
                        bboxCoords[imax] = val;
                }
            }
        }
        
        return bboxCoords;
    }
    
    
    public static Bbox toBbox(RectangleRn rect)
    {
        int nDims = rect.nDimensions();
        Bbox bbox = new Bbox();
        
        bbox.setMinX(rect.getMinCoord(0));
        bbox.setMaxX(rect.getMaxCoord(0));
        
        bbox.setMinY(rect.getMinCoord(1));
        bbox.setMaxY(rect.getMaxCoord(1));
        
        if (nDims == 3)
        {
            bbox.setMinZ(rect.getMinCoord(2));
            bbox.setMaxZ(rect.getMaxCoord(2));
        }
        
        return bbox;
    }
}
