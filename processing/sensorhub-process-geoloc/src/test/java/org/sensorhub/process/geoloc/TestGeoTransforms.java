/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2022 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.process.geoloc;

import static org.junit.Assert.*;
import org.junit.Test;
import org.sensorhub.algo.geoloc.GeoTransforms;
import org.sensorhub.algo.vecmath.Vect3d;


public class TestGeoTransforms
{
    private final static double MEAN_EARTH_RADIUS = 6371000.;
    GeoTransforms geo = new GeoTransforms();
    
    
    @Test
    public void testComputeGreatCircleDistanceHaversine()
    {
        double dLat, dLon, dist;
        
        // going north
        dLat = Math.PI/2;
         dist = geo.computeGreatCircleDistanceHaversine(0.0, 0.0, dLat, 0.0);
        assertEquals(MEAN_EARTH_RADIUS*dLat, dist, 1.0);
        
        // going east
        dLon = Math.PI/8;
        dist = geo.computeGreatCircleDistanceHaversine(0.0, 0.0, 0.0, dLon);
        assertEquals(MEAN_EARTH_RADIUS*dLon, dist, 1.0);
        
        // going south
        dLat = Math.toRadians(36.6);
        dist = geo.computeGreatCircleDistanceHaversine(0.0, 0.0, dLat, 0.0);
        assertEquals(MEAN_EARTH_RADIUS*dLat, dist, 1.0);
        
        // going west
        dLon = Math.toRadians(156.3);
        dist = geo.computeGreatCircleDistanceHaversine(0.0, 0.0, 0.0, dLon);
        assertEquals(MEAN_EARTH_RADIUS*dLon, dist, 1.0);
    }
    
    
    @Test
    public void testComputeGreatCircleDistanceLawOfCosines()
    {
        var dist = geo.computeGreatCircleDistanceLawOfCosines(0.0, 0.0, Math.PI/2, 0.0);
        assertEquals(2*Math.PI*6371000./4.0, dist, 1.0);
    }
    
    
    @Test
    public void testComputeDistanceEquirectangular()
    {
        var dist = geo.computeDistanceEquirectangular(0.0, 0.0, Math.PI/2, 0.0);
        assertEquals(2*Math.PI*6371000./4.0, dist, 1.0);
    }
    
    
    @Test
    public void testComputeBearing()
    {
        // north
        var bearing = geo.computeBearing(0.0, 0.0, 1.0, 0.0);
        assertEquals(0.0, bearing, 1e-8);
        
        // south
        bearing = geo.computeBearing(0.0, 0.0, -1.0, 0.0);
        assertEquals(Math.PI, bearing, 1e-8);
        
        // east
        bearing = geo.computeBearing(0.0, 0.0, 0.0, 1.0);
        assertEquals(Math.PI/2, bearing, 1e-8);
        
        // west
        bearing = geo.computeBearing(0.0, 0.0, 0.0, -1.0);
        assertEquals(-Math.PI/2, bearing, 1e-8);
    }
    
    
    @Test
    public void computeIntermediatePoint()
    {
        double lat1 = 0.0;
        double lon1 = 0.0;
        double lat2 = Math.PI/4;
        double lon2 = 0.0;
        
        var result = new Vect3d();
        var dist = geo.computeGreatCircleDistanceHaversine(lat1, lon1, lat2, lon2);
        
        geo.computeIntermediatePoint(lat1, lon1, lat2, lon2, dist, 0.5, result);
        assertEquals(result.x, 0.0, 1e-8);
        assertEquals(result.y, Math.PI/8, 1e-8);
        
        geo.computeIntermediatePoint(lat1, lon1, lat2, lon2, dist, 0.5, result);
        assertEquals(result.x, 0.0, 1e-8);
        assertEquals(result.y, Math.PI/8, 1e-8);
    }

}
