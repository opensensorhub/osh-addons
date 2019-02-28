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

import net.opengis.sensorml.v20.Settings;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.Quantity;
import org.vast.process.SMLException;
import org.vast.sensorML.ExecutableProcessImpl;
import org.vast.sensorML.SMLHelper;


/**
 * <p>
 * Multiplication of two matrices and a scalar,
 * such that Mres = s.M1.M2, where M1, M2, and Mres are matrices and s a scalar.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @date Jan 16, 2008
 */
public class MulMatMat_Process extends ExecutableProcessImpl
{
    private DataArray mat1, mat2, resultMat;
    private Quantity scalar;
    private int m1Rows, m1Cols, m2Cols;
	
    
    public MulMatMat_Process()
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
        /*m1Rows = 4;
        m1Cols = 4;
        m2Cols = 4;
        
        mat1.updateSize(m1Rows);
        ((DataArray)mat1.getElementType()).updateSize(m1Cols);
        mat1.renewDataBlock();
        
        mat2.updateSize(m1Cols);
        ((DataArray)mat2.getElementType()).updateSize(m2Cols);
        mat2.renewDataBlock();
        
        resultMat.updateSize(m1Rows);
        ((DataArray)resultMat.getElementType()).updateSize(m2Cols);
        resultMat.renewDataBlock();*/
        
        SMLHelper.applyConfig(wrapperProcess, (Settings)wrapperProcess.getConfiguration());
        m1Rows = mat1.getComponentCount();
        m1Cols = mat1.getElementType().getComponentCount();
        m2Cols = mat2.getElementType().getComponentCount();
        
        // enforce output matrix size
        resultMat.updateSize(m1Rows);
        ((DataArray)resultMat.getElementType()).updateSize(m2Cols);
        resultMat.renewDataBlock();
    }
    

    @Override
    public void execute() throws SMLException
    {
        // get reference matrix data
        final double[] mat1Data = (double[])mat1.getData().getUnderlyingObject();
        final double[] mat2Data = (double[])mat2.getData().getUnderlyingObject();
        final double[] resMatData = (double[])resultMat.getData().getUnderlyingObject();
        
        // read scalar
        double s = scalar.getData().getDoubleValue();
        
        // compute product
        for (int i = 0; i < m1Rows; i++)
        {
            for (int j = 0; j < m2Cols; j++)
            {
                double val = 0.;
                
                for (int k = 0; k < m1Cols; k++)
                {
                    double m1 = mat1Data[i*m1Cols+k];
                    double m2 = mat2Data[k*m2Cols+j];
                    val += m1*m2;
                }
                
                resMatData[i*m2Cols+j] = s*val;
            }
        }
    } 
}