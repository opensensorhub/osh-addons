/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License Version
 1.1 (the "License"); you may not use this file except in compliance with
 the License. You may obtain a copy of the License at
 http://www.mozilla.org/MPL/MPL-1.1.html
 
 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.
 
 The Original Code is the "SensorML AbstractProcessing Engine".
 
 The Initial Developer of the Original Code is the VAST team at the University of Alabama in Huntsville (UAH). <http://vast.uah.edu> Portions created by the Initial Developer are Copyright (C) 2007 the Initial Developer. All Rights Reserved. Please Contact Mike Botts <mike.botts@uah.edu> for more information.
 
 Contributor(s): 
    Alexandre Robin <robin@nsstc.uah.edu>
    Gregoire Berthiau <berthiau@nsstc.uah.edu>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.process.vecmath;

import net.opengis.swe.v20.AllowedTokens;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.Text;
import net.opengis.swe.v20.Vector;
import org.sensorhub.algo.vecmath.Mat3d;
import org.vast.process.SMLException;
import org.vast.sensorML.ExecutableProcessImpl;


/**
 * <p>
 * Construct a 3D matrix from the euler rotation angles about the 3 axis
 * in the specified order
 * </p>
 *
 * @author Alexandre Robin & Gregoire Berthiau
 * @date Mar 7, 2007
 */
public class Euler2Mat3_Process extends ExecutableProcessImpl
{
    private Quantity r1Data, r2Data, r3Data;
    private Text orderParam;
    private DataArray outputMatrix;
    private char[] rotAxes = {'X','Y','Z'};
    private double[] rotValues = new double[3];
    private Mat3d rotM = new Mat3d();
    
    
    public Euler2Mat3_Process()
    {
        VecMathHelper sweHelper = new VecMathHelper();
        
        // create euler input
        Vector eulerData = sweHelper.newEulerAngles();
        eulerData.setReferenceFrame(null);
        inputData.add("orientation", eulerData);
        r1Data = (Quantity)eulerData.getComponent(0);
        r2Data = (Quantity)eulerData.getComponent(1);
        r3Data = (Quantity)eulerData.getComponent(2);
        
        // create matrix output
        outputMatrix = sweHelper.newMatrix(VecMathHelper.DEF_ROT_MATRIX, null, 3, 3);
        outputData.add("rotMatrix", outputMatrix);
        
        // create rot order param
        orderParam = sweHelper.newText();
        AllowedTokens values = sweHelper.newAllowedTokens();
        values.addValue("XYZ");
        values.addValue("YZX");
        values.addValue("ZXY");
        values.addValue("XZY");
        values.addValue("ZYX");
        values.addValue("YXZ");
        orderParam.setConstraint(values);
        paramData.add("rotAxes", orderParam);
    }
    
   
    @Override
    public void init() throws SMLException
    {
        String orderString = orderParam.getValue();
        for (int i = 0; i < 3; i++)
            rotAxes[i] = orderString.charAt(i);
    }
    
    
    @Override
    public void execute() throws SMLException
    {
        rotValues[0] = r1Data.getData().getDoubleValue();
        rotValues[1] = r2Data.getData().getDoubleValue();
        rotValues[2] = r3Data.getData().getDoubleValue();
  
        // set up rotation matrices
        rotM.setIdentity();
 
        // rotate in reverse order as the one given
        // to get intrisic rotations (i.e. in rotating frames)
        for (int i=2; i>=0; i--)
        {
            char axis = rotAxes[i];
            double r = -rotValues[i];
            
            switch (axis)
            {
                case 'X':
                    rotM.rotateX(r);
                    break;
                    
                case 'Y':
                    rotM.rotateY(r);
                    break;
                    
                case 'Z':
                    rotM.rotateZ(r);
                    break;
            }
        }
        
        // assign values to output matrix
        VecMathHelper.fromMat3d(rotM, outputMatrix.getData());
    }
}