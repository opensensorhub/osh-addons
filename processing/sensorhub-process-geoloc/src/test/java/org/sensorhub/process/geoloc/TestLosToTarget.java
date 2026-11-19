/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.process.geoloc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.sensorhub.algo.vecmath.Vect3d;
import org.sensorhub.process.vecmath.VecMathHelper;
import org.vast.sensorML.SMLUtils;
import org.vast.sensorML.SimpleProcessImpl;
import net.opengis.swe.v20.DataBlock;


public class TestLosToTarget
{
    
    private LosToTarget createProcess() throws Exception
    {
        var p = new LosToTarget();
        p.init();
                
        // serialize
        var wp = new SimpleProcessImpl();
        wp.setExecutableImpl(p);
        new SMLUtils(SMLUtils.V2_0).writeProcess(System.out, wp, true);
        
        return p;
    }
    
    
    private DataBlock execProcess(LosToTarget p, Vect3d obsLocation, double az, double elev, double dist) throws Exception
    {
        VecMathHelper.fromVect3d(obsLocation, p.getInputList().getComponent("observerLocation").getData());
        var losInput = p.getInputList().getComponent("losDirection").getData();
        losInput.setDoubleValue(0, az);
        losInput.setDoubleValue(1, elev);
        p.getInputList().getComponent("targetDistance").getData().setDoubleValue(dist);
        p.execute();
        return p.getOutputList().getComponent(0).getData();
    }
    
    
    private void checkOutputEquals(DataBlock llaPos, double lat, double lon, double alt)
    {
        assertEquals(lat, llaPos.getDoubleValue(0), 1e-8);
        assertEquals(lon, llaPos.getDoubleValue(1), 1e-8);
        assertEquals(alt, llaPos.getDoubleValue(2), 1e-8);
    }
    
    
    @Test
    public void testExecute() throws Exception
    {
        var p = createProcess();
        DataBlock targetPos;
        
        // loc 0,0,0 looking up
        targetPos = execProcess(p, new Vect3d(0.0, 0.0, 0.0), 0, 90, 100);
        checkOutputEquals(targetPos, 0.0, 0.0, 100.0);
        
        // loc 0,0,100 looking down
        targetPos = execProcess(p, new Vect3d(0.0, 0.0, 100.0), 0, -90, 100);
        checkOutputEquals(targetPos, 0.0, 0.0, 0.0);
        
        // loc 0,0,0 looking east
        targetPos = execProcess(p, new Vect3d(0.0, 0.0, 0.0), 90, 0, 100);
        assertEquals(0.0, targetPos.getDoubleValue(0), 1e-8); // check lat unchanged
        assertTrue(targetPos.getDoubleValue(1) > 0); // check lon > 0
        assertEquals(0.0, targetPos.getDoubleValue(2), 1e-2); // check alt  0
                
        // loc 0,0,0 looking east, down 45°
        targetPos = execProcess(p, new Vect3d(0.0, 0.0, 0.0), 90, -45, 100);
        assertEquals(0.0, targetPos.getDoubleValue(0), 1e-8); // check lat unchanged
        assertTrue(targetPos.getDoubleValue(1) > 0); // check lon > 0
        assertEquals(-Math.sqrt(2)/2*100.0, targetPos.getDoubleValue(2), 1e-2); // check alt ~ -70m
        
        // loc 0,0,0 looking west, down 45°
        targetPos = execProcess(p, new Vect3d(0.0, 0.0, 0.0), -90, -45, 100);
        assertEquals(0.0, targetPos.getDoubleValue(0), 1e-8); // check lat unchanged
        assertTrue(targetPos.getDoubleValue(1) < 0); // check lon < 0
        assertEquals(-Math.sqrt(2)/2*100.0, targetPos.getDoubleValue(2), 1e-2); // check alt ~ -70m
        
        // loc 0,0,0 looking south, up 30°
        targetPos = execProcess(p, new Vect3d(0.0, 0.0, 0.0), 180, 30, 100);
        assertTrue(targetPos.getDoubleValue(0) < 0); // check lat < 0
        assertEquals(0.0, targetPos.getDoubleValue(1), 1e-8); // check lon unchanged
        assertEquals(Math.sin(Math.PI/6)*100.0, targetPos.getDoubleValue(2), 1e-2); // check alt ~ 50m
    }
}
