/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License Version
 1.1 (the "License"); you may not use this file except in compliance with
 the License. You may obtain a copy of the License at
 http://www.mozilla.org/MPL/MPL-1.1.html
 
 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.
 
 The Original Code is the "SensorML Processing Engine".
 
 The Initial Developer of the Original Code is the VAST team at the University of Alabama in Huntsville (UAH). <http://vast.uah.edu> Portions created by the Initial Developer are Copyright (C) 2007 the Initial Developer. All Rights Reserved. Please Contact Mike Botts <mike.botts@uah.edu> for more information.
 
 Contributor(s): 
    Alexandre Robin <robin@nsstc.uah.edu>
    Gregoire Berthiau <berthiau@nsstc.uah.edu>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.process.vecmath;

import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.Quantity;
import org.sensorhub.algo.vecmath.Mat3d;
import org.vast.process.SMLException;
import org.vast.sensorML.ExecutableProcessImpl;


/**
 * <p>
 * Multiplication of two 3x3 matrices and a scalar,
 * such that Mres = s.M1.M2, where M1, M2, and Mres are matrices and s a scalar.
 * </p>
 *
 * @author Gregoire Berthiau
 * @date Jan 16, 2008
 */
public class MulMat3Mat3_Process extends ExecutableProcessImpl
{
    private DataArray mat1, mat2, resultMat;
    private Quantity scalar;
    private Mat3d m1;
    private Mat3d m2;
	
    
    public MulMat3Mat3_Process()
    {
        VecMathHelper sweHelper = new VecMathHelper();
        
        // create input M1
        mat1 = sweHelper.newMatrix(3, 3);
        inputData.add("M1", mat1);
        
        // create input M1
        mat2 = sweHelper.newMatrix(3, 3);
        inputData.add("M2", mat2);
        
        // create scalar input
        scalar = sweHelper.newQuantity();
        scalar.setValue(1.0);
        inputData.add("s", scalar);
        
        // create matrix output
        resultMat = sweHelper.newMatrix(3, 3);
        outputData.add("Mres", resultMat);
    }
    
    
    @Override
    public void init() throws SMLException
    {
        m1 = new Mat3d();
        m2 = new Mat3d();
    }
    

    @Override
    public void execute() throws SMLException
    {
        // read input matrix data
        VecMathHelper.toMat3d(mat1.getData(), m1);
        VecMathHelper.toMat3d(mat2.getData(), m2);
        
        // read scalar
        double s = scalar.getData().getDoubleValue();
        
        // compute product
        m1.mul(m2).mul(s);

        // set output matrix values
        VecMathHelper.fromMat3d(m1, resultMat.getData());
    } 
}