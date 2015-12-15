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
import org.sensorhub.algo.geoloc.Ellipsoid;
import org.sensorhub.algo.vecmath.Vect3d;
import org.sensorhub.process.geoloc.RayIntersectEllipsoid_Process;
import org.sensorhub.process.vecmath.VecMathHelper;
import org.vast.sensorML.SMLUtils;
import org.vast.sensorML.SimpleProcessImpl;


public class TestRayIntersectEllipsoidProcess
{
    
    private RayIntersectEllipsoid_Process createProcess() throws Exception
    {
        RayIntersectEllipsoid_Process p = new RayIntersectEllipsoid_Process();
        p.init();
        p.createNewInputBlocks();
        p.createNewOutputBlocks();
                
        // serialize
        SimpleProcessImpl wp = new SimpleProcessImpl();
        wp.setExecutableImpl(p);
        new SMLUtils(SMLUtils.V2_0).writeProcess(System.out, wp, true);
        
        return p;
    }
    
    
    private DataBlock execProcess(RayIntersectEllipsoid_Process p, Vect3d orig, Vect3d dir) throws Exception
    {
        VecMathHelper.fromVect3d(orig, p.getInputList().getComponent("rayOrigin").getData());
        VecMathHelper.fromVect3d(dir, p.getInputList().getComponent("rayDirection").getData());
        p.execute();
        return p.getOutputList().getComponent(0).getData();
    }
    
    
    @Test
    public void testIntersectFromOutside() throws Exception
    {
        RayIntersectEllipsoid_Process p = createProcess();
        DataBlock intersectPos;
        
        // straight down from above lat0, lon0
        intersectPos = execProcess(p, new Vect3d(20e6, 0.0, 0.0), new Vect3d(-1.0, 0.0, 0.0));
        assertEquals(Ellipsoid.WGS84.getEquatorRadius(), intersectPos.getDoubleValue(0), 1e-6);
        assertEquals(0.0, intersectPos.getDoubleValue(1), 1e-6);
        assertEquals(0.0, intersectPos.getDoubleValue(2), 1e-6);
        
        // straight down from above north pole
        intersectPos = execProcess(p, new Vect3d(0.0, 0.0, 10e6), new Vect3d(0.0, 0.0, -1.0));
        assertEquals(0.0, intersectPos.getDoubleValue(0), 1e-6);
        assertEquals(0.0, intersectPos.getDoubleValue(1), 1e-6);
        assertEquals(Ellipsoid.WGS84.getPolarRadius(), intersectPos.getDoubleValue(2), 1e-6);
    }
    
    
    @Test
    public void testIntersectFromInside() throws Exception
    {
        RayIntersectEllipsoid_Process p = createProcess();
        DataBlock intersectPos;
        
        // from earth center to lat0, lon0
        intersectPos = execProcess(p, new Vect3d(0.0, 0.0, 0.0), new Vect3d(1.0, 0.0, 0.0));
        assertEquals(Ellipsoid.WGS84.getEquatorRadius(), intersectPos.getDoubleValue(0), 1e-6);
        assertEquals(0.0, intersectPos.getDoubleValue(1), 1e-6);
        assertEquals(0.0, intersectPos.getDoubleValue(2), 1e-6);
        
        // from inside earth on X axis to lat0, lon0
        intersectPos = execProcess(p, new Vect3d(1e6, 0.0, 0.0), new Vect3d(1.0, 0.0, 0.0));
        assertEquals(Ellipsoid.WGS84.getEquatorRadius(), intersectPos.getDoubleValue(0), 1e-6);
        assertEquals(0.0, intersectPos.getDoubleValue(1), 1e-6);
        assertEquals(0.0, intersectPos.getDoubleValue(2), 1e-6);
        
        // from inside earth on -X axis to lat0, lon0
        intersectPos = execProcess(p, new Vect3d(-1e6, 0.0, 0.0), new Vect3d(1.0, 0.0, 0.0));
        assertEquals(Ellipsoid.WGS84.getEquatorRadius(), intersectPos.getDoubleValue(0), 1e-6);
        assertEquals(0.0, intersectPos.getDoubleValue(1), 1e-6);
        assertEquals(0.0, intersectPos.getDoubleValue(2), 1e-6);
        
        // from inside earth on Y axis to lat0, lon90
        intersectPos = execProcess(p, new Vect3d(0.0, 1e6, 0.0), new Vect3d(0.0, 1.0, 0.0));
        assertEquals(0.0, intersectPos.getDoubleValue(0), 1e-6);
        assertEquals(Ellipsoid.WGS84.getEquatorRadius(), intersectPos.getDoubleValue(1), 1e-6);
        assertEquals(0.0, intersectPos.getDoubleValue(2), 1e-6);
    }
}
