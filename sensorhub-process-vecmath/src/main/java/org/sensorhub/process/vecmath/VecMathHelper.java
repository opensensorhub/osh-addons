/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.process.vecmath;

import org.sensorhub.algo.vecmath.Mat3d;
import org.sensorhub.algo.vecmath.Mat4d;
import org.sensorhub.algo.vecmath.Quat4d;
import org.sensorhub.algo.vecmath.Vect3d;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.Matrix;
import net.opengis.swe.v20.Vector;


public class VecMathHelper extends SWEHelper
{
    public static String DEF_UNIT_VECTOR = SWEHelper.getPropertyUri("UnitVector");
    public static String DEF_ROW = SWEHelper.getPropertyUri("Row");
    public static String DEF_COEF = SWEHelper.getPropertyUri("Coordinate");
    public static String DEF_ROT_MATRIX = SWEHelper.getPropertyUri("RotationMatrix");
    
    
    public Vector newVector3()
    {
        return newVector3(null, null);
    }
    
    
    public Vector newVector3(String def, String crs)
    {
        return newVector(
                def,
                crs,
                new String[] {"u1", "u2", "u3"},
                null,
                new String[] {"1", "1", "1"},
                null);
    }
    
    
    public Vector newUnitVectorXYZ(String def, String crs)
    {
        if (def == null)
            def = DEF_UNIT_VECTOR;
        
        return newVector(
                def,
                crs,
                new String[] {"ux", "uy", "uz"},
                null,
                new String[] {"1", "1", "1"},
                new String[] {"X", "Y", "Z"});
    }
    
    
    public Vector newEulerAngles()
    {
        return newVector(
                SWEConstants.DEF_ORIENTATION,
                null,
                new String[] {"r1", "r2", "r3"},
                null,
                new String[] {"rad", "rad", "rad"},
                new String[] {null, null, null});
    }
    
    
    public Matrix newMatrix(int nRows, int nCols)
    {
        return newMatrix(null, null, nRows, nCols);
    }
    
    
    public Matrix newMatrix(String def, String crs, int nRows, int nCols)
    {
        Matrix row = newMatrix(nCols);
        row.setDefinition(DEF_ROW);
        row.setElementType("coef", newQuantity(DEF_COEF, null, null, "1"));
        
        Matrix mat = newMatrix(nRows);
        mat.setDefinition(def);
        mat.setElementType("row", row);
        
        return mat;
    }
    
    
    public static void toVect3d(DataBlock vData, Vect3d v)
    {
        v.x = vData.getDoubleValue(0);
        v.y = vData.getDoubleValue(1);
        v.z = vData.getDoubleValue(2);
    }
    
    
    public static void fromVect3d(Vect3d v, DataBlock vData)
    {
        vData.setDoubleValue(0, v.x);
        vData.setDoubleValue(1, v.y);
        vData.setDoubleValue(2, v.z);
    }
    
    
    public static void toQuat4d(DataBlock vData, Quat4d q)
    {
        q.s = vData.getDoubleValue(0);
        q.x = vData.getDoubleValue(1);
        q.y = vData.getDoubleValue(2);
        q.z = vData.getDoubleValue(3);
    }
    
    
    public static void fromQuat4d(Quat4d q, DataBlock vData)
    {
        vData.setDoubleValue(0, q.s);
        vData.setDoubleValue(1, q.x);
        vData.setDoubleValue(2, q.y);
        vData.setDoubleValue(3, q.z);
    }
    
    
    public static void toMat3d(DataBlock mData, Mat3d m)
    {
        m.m00 = mData.getDoubleValue(0);
        m.m01 = mData.getDoubleValue(1);
        m.m02 = mData.getDoubleValue(2);
        m.m10 = mData.getDoubleValue(3);
        m.m11 = mData.getDoubleValue(4);
        m.m12 = mData.getDoubleValue(5);
        m.m20 = mData.getDoubleValue(6);
        m.m21 = mData.getDoubleValue(7);
        m.m22 = mData.getDoubleValue(8);
    }
    
    
    public static void fromMat3d(Mat3d m, DataBlock mData)
    {
        mData.setDoubleValue(0, m.m00);
        mData.setDoubleValue(1, m.m01);
        mData.setDoubleValue(2, m.m02);
        mData.setDoubleValue(3, m.m10);
        mData.setDoubleValue(4, m.m11);
        mData.setDoubleValue(5, m.m12);
        mData.setDoubleValue(6, m.m20);
        mData.setDoubleValue(7, m.m21);
        mData.setDoubleValue(8, m.m22);
    }
    
    
    public static void toMat4d(DataBlock mData, Mat4d m)
    {
        m.m00 = mData.getDoubleValue(0);
        m.m01 = mData.getDoubleValue(1);
        m.m02 = mData.getDoubleValue(2);
        m.m03 = mData.getDoubleValue(3);
        
        m.m10 = mData.getDoubleValue(4);
        m.m11 = mData.getDoubleValue(5);
        m.m12 = mData.getDoubleValue(6);
        m.m13 = mData.getDoubleValue(7);
        
        m.m20 = mData.getDoubleValue(8);
        m.m21 = mData.getDoubleValue(9);
        m.m22 = mData.getDoubleValue(10);
        m.m23 = mData.getDoubleValue(11);
        
        m.m30 = mData.getDoubleValue(12);
        m.m31 = mData.getDoubleValue(13);
        m.m32 = mData.getDoubleValue(14);
        m.m33 = mData.getDoubleValue(15);
    }
    
    
    public static void fromMat4d(Mat4d m, DataBlock mData)
    {
        mData.setDoubleValue(0, m.m00);
        mData.setDoubleValue(1, m.m01);
        mData.setDoubleValue(2, m.m02);
        mData.setDoubleValue(3, m.m03);
        
        mData.setDoubleValue(4, m.m10);        
        mData.setDoubleValue(5, m.m11);
        mData.setDoubleValue(6, m.m12);
        mData.setDoubleValue(7, m.m13);
        
        mData.setDoubleValue(8, m.m20);
        mData.setDoubleValue(9, m.m21);
        mData.setDoubleValue(10, m.m22);
        mData.setDoubleValue(11, m.m23);
        
        mData.setDoubleValue(12, m.m30);
        mData.setDoubleValue(13, m.m31);
        mData.setDoubleValue(14, m.m32);
        mData.setDoubleValue(15, m.m33);
    }
}
