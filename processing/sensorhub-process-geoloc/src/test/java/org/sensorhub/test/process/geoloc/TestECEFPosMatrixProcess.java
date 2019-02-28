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
import org.junit.Test;
import org.sensorhub.algo.geoloc.GeoTransforms;
import org.sensorhub.algo.vecmath.Mat4d;
import org.sensorhub.algo.vecmath.Quat4d;
import org.sensorhub.algo.vecmath.Vect3d;
import org.sensorhub.process.geoloc.ECEFPositionMatrix_Process;
import org.sensorhub.process.vecmath.VecMathHelper;
import org.vast.sensorML.SMLUtils;
import org.vast.sensorML.SimpleProcessImpl;


public class TestECEFPosMatrixProcess
{
    
    private ECEFPositionMatrix_Process createProcess() throws Exception
    {
        ECEFPositionMatrix_Process p = new ECEFPositionMatrix_Process();
        
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
    
    
    private Mat4d execProcess(ECEFPositionMatrix_Process p, Vect3d loc, Quat4d dir) throws Exception
    {
        VecMathHelper.fromVect3d(loc, p.getInputList().getComponent("platformLoc").getData());
        VecMathHelper.fromQuat4d(dir, p.getInputList().getComponent("platformAtt").getData());
        p.execute();
        Mat4d m = new Mat4d();
        VecMathHelper.toMat4d(p.getOutputList().getComponent("posMatrix").getData(), m);
        return m;
    }
    
    
    private void checkResult(Mat4d t, Vect3d localPos, Vect3d ecefPos)
    {
        localPos.transform(t);
        assertEquals(localPos.x, ecefPos.x, 1e-6);
        assertEquals(localPos.y, ecefPos.y, 1e-6);
        assertEquals(localPos.z, ecefPos.z, 1e-6);
    }
    
    
    @Test
    public void testMatrixHorizontal() throws Exception
    {
        ECEFPositionMatrix_Process p = createProcess();
        
        GeoTransforms geoConv = new GeoTransforms();
        Mat4d posMat;
        Vect3d ecefLoc = new Vect3d();
        Quat4d enuRot = new Quat4d();
        Quat4d attQ = new Quat4d();
        Vect3d newEcefLoc = new Vect3d();
        
        
        // above lat0, lon0, horizontal and pointing north (identity in ENU)
        geoConv.LLAtoECEF(new Vect3d(0.0, 0.0, 0.0), ecefLoc);
        posMat = execProcess(p, ecefLoc, enuRot);
        posMat.toQuat(attQ);
        
        // check output
        assertEquals(0.5, attQ.s, 1e-12);
        assertEquals(0.5, attQ.x, 1e-12);
        assertEquals(0.5, attQ.y, 1e-12);
        assertEquals(0.5, attQ.z, 1e-12);
        checkResult(posMat, new Vect3d(0.0, 0.0, 0.0), ecefLoc);
        checkResult(posMat, new Vect3d(10.0, 0.0, 0.0), newEcefLoc.add(ecefLoc, new Vect3d(0, 10, 0)));
        checkResult(posMat, new Vect3d(0.0, 10.0, 0.0), newEcefLoc.add(ecefLoc, new Vect3d(0, 0, 10)));
        
        
        // above lat0, lon0, horizontal and pointing east
        geoConv.LLAtoECEF(new Vect3d(0.0, 0.0, 0.0), ecefLoc);
        enuRot.set(0.0, 0.0, Math.sin(Math.PI/2/2), Math.cos(Math.PI/2/2));
        posMat = execProcess(p, ecefLoc, enuRot);
        posMat.toQuat(attQ);
        
        // check output
        assertEquals(0.0, attQ.s, 1e-12);
        assertEquals(Math.sqrt(2)/2, attQ.x, 1e-12);
        assertEquals(0.0, attQ.y, 1e-12);
        assertEquals(Math.sqrt(2)/2, attQ.z, 1e-12);
        checkResult(posMat, new Vect3d(0.0, 0.0, 0.0), ecefLoc);
        checkResult(posMat, new Vect3d(10.0, 0.0, 0.0), newEcefLoc.add(ecefLoc, new Vect3d(0, 0, 10)));
        checkResult(posMat, new Vect3d(0.0, 10.0, 0.0), newEcefLoc.add(ecefLoc, new Vect3d(0, -10, 0)));
    }
}
