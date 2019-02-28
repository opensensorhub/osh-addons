/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence.h2;

import org.h2.mvstore.rtree.SpatialKey;
import net.opengis.gml.v32.AbstractGeometry;
import net.opengis.gml.v32.LineString;
import net.opengis.gml.v32.Point;
import net.opengis.gml.v32.Polygon;


public class GeomUtils
{
    static final String GEOM_DIM_ERROR = "Only 2D and 3D geometries are supported";
    
    
    public static SpatialKey getBoundingRectangle(int hashID, AbstractGeometry geom)
    {
        float[] minMaxCoords = null;
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
            minMaxCoords = getBoundingRectangle(numDims, pos);
        }
        
        // case of polylines
        else if (geom instanceof LineString)
        {
            double[] posList = ((LineString)geom).getPosList();
            minMaxCoords = getBoundingRectangle(numDims, posList);
        }
        
        // case of polygons
        else if (geom instanceof Polygon)
        {
            double[] posList = ((Polygon)geom).getExterior().getPosList();
            minMaxCoords = getBoundingRectangle(numDims, posList);
        }
        
        if (minMaxCoords != null)
            return new SpatialKey(hashID, minMaxCoords);
        else
            return null;
    }
    
    
    public static float[] getBoundingRectangle(int numDims, double[] geomCoords)
    {
        int numPoints = geomCoords.length / numDims;
        float[] minMaxCoords = new float[2*numDims];
        minMaxCoords[4] = 0.0f;
        minMaxCoords[5] = 0.0f;
        
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
                int imin = i*2;
                int imax = imin+1;
                
                float downVal = Math.nextDown((float)val);
                float upVal = Math.nextUp((float)val);
                
                if (p == 0)
                {
                    minMaxCoords[imin] = downVal;
                    minMaxCoords[imax] = upVal;
                }
                else
                {
                    if (downVal < minMaxCoords[imin])
                        minMaxCoords[imin] = downVal;
                    if (upVal > minMaxCoords[imax])
                        minMaxCoords[imax] = upVal;
                }
            }
        }
        
        return minMaxCoords;
    }
}
