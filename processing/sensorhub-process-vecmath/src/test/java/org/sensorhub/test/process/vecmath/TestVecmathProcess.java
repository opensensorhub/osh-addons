/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.process.vecmath;

import static org.junit.Assert.assertEquals;
import net.opengis.sensorml.v20.Settings;
import net.opengis.sensorml.v20.ValueSetting;
import net.opengis.sensorml.v20.impl.SettingsImpl;
import net.opengis.sensorml.v20.impl.ValueSettingImpl;
import net.opengis.swe.v20.DataBlock;
import org.junit.Test;
import org.sensorhub.algo.vecmath.Mat3d;
import org.sensorhub.algo.vecmath.Mat4d;
import org.sensorhub.algo.vecmath.Vect3d;
import org.sensorhub.process.vecmath.MulMat3Mat3_Process;
import org.sensorhub.process.vecmath.MulMat3Vec3_Process;
import org.sensorhub.process.vecmath.MulMatMat_Process;
import org.sensorhub.process.vecmath.VecMathHelper;
import org.vast.sensorML.SMLUtils;
import org.vast.sensorML.SimpleProcessImpl;


public class TestVecmathProcess
{
    
    
    @Test
    public void testMulM3V3() throws Exception
    {
        MulMat3Vec3_Process p = new MulMat3Vec3_Process();
        p.init();
        p.createNewInputBlocks();
        p.createNewOutputBlocks();
                
        // serialize
        SimpleProcessImpl wp = new SimpleProcessImpl();
        wp.setExecutableImpl(p);
        new SMLUtils(SMLUtils.V2_0).writeProcess(System.out, wp, true);
        
        // mul identity
        Mat3d m = new Mat3d();
        for (int i = 0; i < 9; i++)
            m.setElement(i/3, i%3, i);
        VecMathHelper.fromMat3d(m, p.getInputList().getComponent("M").getData());
        Vect3d v = new Vect3d(1.0, 2.0, 3.0);
        VecMathHelper.fromVect3d(v, p.getInputList().getComponent("V").getData());
        p.execute();
        DataBlock outMat = p.getOutputList().getComponent(0).getData();
        assertEquals(0*1+1*2+2*3, outMat.getDoubleValue(0), 1e-12);
        assertEquals(3*1+4*2+5*3, outMat.getDoubleValue(1), 1e-12);
        assertEquals(6*1+7*2+8*3, outMat.getDoubleValue(2), 1e-12);
    }
    
    
    @Test
    public void testMulM3M3() throws Exception
    {
        MulMat3Mat3_Process p = new MulMat3Mat3_Process();
        p.init();
        p.createNewInputBlocks();
        p.createNewOutputBlocks();
        
        // serialize
        SimpleProcessImpl wp = new SimpleProcessImpl();
        wp.setExecutableImpl(p);
        new SMLUtils(SMLUtils.V2_0).writeProcess(System.out, wp, true);
        
        // mul identity
        Mat3d m1 = new Mat3d();
        for (int i = 0; i < 9; i++)
            m1.setElement(i/3, i%3, i);
        VecMathHelper.fromMat3d(m1, p.getInputList().getComponent("M1").getData());
        VecMathHelper.fromMat3d(new Mat3d().setIdentity(), p.getInputList().getComponent("M2").getData());
        p.getInputList().getComponent("s").getData().setDoubleValue(4.0);
        p.execute();
        DataBlock outMat = p.getOutputList().getComponent(0).getData();
        for (int i = 0; i < 9; i++)
            assertEquals(i*4.0, outMat.getDoubleValue(i), 1e-12);
    }
    
    
    @Test
    public void testMulMM() throws Exception
    {
        MulMatMat_Process p = new MulMatMat_Process();
                
        // configure process
        SimpleProcessImpl wp = new SimpleProcessImpl();
        Settings config = new SettingsImpl();
        wp.setConfiguration(config);
        
        ValueSetting m1Rows = new ValueSettingImpl();
        m1Rows.setRef("inputs/M1/elementCount");
        m1Rows.setValue("4");
        config.addSetValue(m1Rows);
        
        ValueSetting m1Cols = new ValueSettingImpl();
        m1Cols.setRef("inputs/M1/row/elementCount");
        m1Cols.setValue("4");
        config.addSetValue(m1Cols);
        
        ValueSetting m2Rows = new ValueSettingImpl();
        m2Rows.setRef("inputs/M2/elementCount");
        m2Rows.setValue("4");
        config.addSetValue(m2Rows);
        
        ValueSetting m2Cols = new ValueSettingImpl();
        m2Cols.setRef("inputs/M2/row/elementCount");
        m2Cols.setValue("4");
        config.addSetValue(m2Cols);  
        
        wp.setExecutableImpl(p);
        p.init();
        p.createNewInputBlocks();
        p.createNewOutputBlocks();
        
        // serialize
        new SMLUtils(SMLUtils.V2_0).writeProcess(System.out, wp, true);
        
        // mul identity
        Mat4d m1 = new Mat4d();
        for (int i = 0; i < 16; i++)
            m1.setElement(i/4, i%4, i);
        Mat4d m2 = new Mat4d();
        for (int i = 0; i < 16; i++)
            m2.setElement(i/4, i%4, i*2);
        //m1.setIdentity();
        VecMathHelper.fromMat4d(m1, p.getInputList().getComponent("M1").getData());
        VecMathHelper.fromMat4d(m2, p.getInputList().getComponent("M2").getData());
        p.getInputList().getComponent("s").getData().setDoubleValue(4.0);
        p.execute();
        
        m1.mul(m2);
        m1.mul(4.0);
        DataBlock outMat = p.getOutputList().getComponent(0).getData();
        for (int i = 0; i < 16; i++)
            assertEquals(m1.getElement(i/4, i%4), outMat.getDoubleValue(i), 1e-12);
    }
}
