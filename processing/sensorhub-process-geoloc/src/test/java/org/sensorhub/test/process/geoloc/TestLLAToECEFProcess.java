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
import org.sensorhub.process.geoloc.LLAToECEF_Process;
import org.sensorhub.process.vecmath.VecMathHelper;
import org.vast.sensorML.SMLUtils;
import org.vast.sensorML.SimpleProcessImpl;


public class TestLLAToECEFProcess
{
    
    private LLAToECEF_Process createProcess() throws Exception
    {
        LLAToECEF_Process p = new LLAToECEF_Process();
        p.init();
        p.createNewInputBlocks();
        p.createNewOutputBlocks();
                
        // serialize
        SimpleProcessImpl wp = new SimpleProcessImpl();
        wp.setExecutableImpl(p);
        new SMLUtils(SMLUtils.V2_0).writeProcess(System.out, wp, true);
        
        return p;
    }
    
    
    private DataBlock execProcess(LLAToECEF_Process p, Vect3d ecef) throws Exception
    {
        VecMathHelper.fromVect3d(ecef, p.getInputList().getComponent(0).getData());
        p.execute();
        return p.getOutputList().getComponent(0).getData();
    }
    
    
    private void checkOutput(DataBlock ecefPos, double x, double y, double z)
    {
        assertEquals(x, ecefPos.getDoubleValue(0), 1e-6);
        assertEquals(y, ecefPos.getDoubleValue(1), 1e-6);
        assertEquals(z, ecefPos.getDoubleValue(2), 1e-6);
    }
    
    
    @Test
    public void testConversion() throws Exception
    {
        LLAToECEF_Process p = createProcess();
        DataBlock ecefPos;
        
        // lat0, lon0
        ecefPos = execProcess(p, new Vect3d(0.0, 0.0, 0.0));
        checkOutput(ecefPos, Ellipsoid.WGS84.getEquatorRadius(), 0.0, 0.0);
        
        // lat0, lon90
        ecefPos = execProcess(p, new Vect3d(0.0, 90.0, 0.0));
        checkOutput(ecefPos, 0.0, Ellipsoid.WGS84.getEquatorRadius(), 0.0);
        
        // lat0, lon180
        ecefPos = execProcess(p, new Vect3d(0.0, 180.0, 0.0));
        checkOutput(ecefPos, -Ellipsoid.WGS84.getEquatorRadius(), 0.0, 0.0);
        
        // lat0, lon-90
        ecefPos = execProcess(p, new Vect3d(0.0, -90., 0.0));
        checkOutput(ecefPos, 0.0, -Ellipsoid.WGS84.getEquatorRadius(), 0.0);
        
        // lat90, lon0
        ecefPos = execProcess(p, new Vect3d(90.0, 0.0, 0.0));
        checkOutput(ecefPos, 0.0, 0.0, Ellipsoid.WGS84.getPolarRadius());
        
        // lat-90, lon0
        ecefPos = execProcess(p, new Vect3d(-90.0, 0.0, 0.0));
        checkOutput(ecefPos, 0.0, 0.0, -Ellipsoid.WGS84.getPolarRadius());
    }
}
