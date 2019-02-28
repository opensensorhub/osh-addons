/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.process.geoloc;

import static org.junit.Assert.assertEquals;
import net.opengis.swe.v20.DataBlock;
import org.junit.Test;
import org.sensorhub.algo.geoloc.GeoTransforms;
import org.sensorhub.algo.geoloc.SRTMUtil;
import org.sensorhub.algo.vecmath.Vect3d;
import org.sensorhub.process.geoloc.RayIntersectTerrain_Process;
import org.sensorhub.process.vecmath.VecMathHelper;
import org.vast.sensorML.SMLUtils;
import org.vast.sensorML.SimpleProcessImpl;


public class TestRayIntersectTerrainProcess
{
    static String srtmRoot = "/media/alex/Backup500/Data/SRTM/US/1arcsec";
    
    
    private RayIntersectTerrain_Process createProcess() throws Exception
    {
        RayIntersectTerrain_Process p = new RayIntersectTerrain_Process();
        
        // set params        
        p.getParameterList().getComponent("srtmDataPath").getData().setStringValue(srtmRoot);
        
        // prepare for exec
        p.init();
        p.createNewInputBlocks();
        p.createNewOutputBlocks();
                
        // serialize
        SimpleProcessImpl wp = new SimpleProcessImpl();
        wp.setExecutableImpl(p);
        new SMLUtils(SMLUtils.V2_0).writeProcess(System.out, wp, true);
        
        return p;
    }
    
    
    private DataBlock execProcess(RayIntersectTerrain_Process p, Vect3d orig, Vect3d dir) throws Exception
    {
        VecMathHelper.fromVect3d(orig, p.getInputList().getComponent("rayOrigin").getData());
        VecMathHelper.fromVect3d(dir, p.getInputList().getComponent("rayDirection").getData());
        p.execute();
        return p.getOutputList().getComponent(0).getData();
    }
    
    
    @Test
    public void testIntersectFromOutside() throws Exception
    {
        RayIntersectTerrain_Process p = createProcess();
        DataBlock intersectPos;
        
        GeoTransforms geoConv = new GeoTransforms();
        SRTMUtil srtm = new SRTMUtil(srtmRoot);
        Vect3d intersect = new Vect3d();
        
        // straight down from lat 35.0, lon -114.5;
        double lat = Math.toRadians(35.0);
        double lon = Math.toRadians(-114.5);        
        Vect3d origin = geoConv.LLAtoECEF(new Vect3d(lon, lat, 1000.), new Vect3d());
        Vect3d down = geoConv.LLAtoECEF(new Vect3d(lon, lat, 0.0), new Vect3d());
        down.sub(origin).normalize();
        intersectPos = execProcess(p, origin, down);
        VecMathHelper.toVect3d(intersectPos, intersect);
        geoConv.ECEFtoLLA(intersect, intersect);
        assertEquals(lon, intersect.x, 1e-6);
        assertEquals(lat, intersect.y, 1e-6);
        assertEquals(srtm.getInterpolatedElevation(Math.toDegrees(lat), Math.toDegrees(lon)), intersect.z, 1e-3);
        
        // oblique
        lat = Math.toRadians(35.5);
        lon = Math.toRadians(-114.7);
        double alt = srtm.getInterpolatedElevation(Math.toDegrees(lat), Math.toDegrees(lon));
        down = geoConv.LLAtoECEF(new Vect3d(lon, lat, alt), new Vect3d());
        origin.add(down, new Vect3d(-1000., -1000., 1000.));
        down.sub(origin).normalize();
        intersectPos = execProcess(p, origin, down);
        VecMathHelper.toVect3d(intersectPos, intersect);
        geoConv.ECEFtoLLA(intersect, intersect);
        assertEquals(lon, intersect.x, 1e-6);
        assertEquals(lat, intersect.y, 1e-6);
        assertEquals(alt, intersect.z, 15.0);
    }
}
