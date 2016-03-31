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
import org.sensorhub.algo.vecmath.Mat4d;
import org.vast.process.SMLException;
import org.vast.sensorML.ExecutableProcessImpl;
import org.vast.swe.helper.VectorHelper;


/**
 * <p>
 * Construct a 3D matrix from the euler rotation angles about the 3 axis
 * in the specified order
 * </p>
 *
 * @author Alexandre Robin & Gregoire Berthiau
 * @date Mar 7, 2007
 */
public class Pos2Mat4_Process extends ExecutableProcessImpl
{
    private Quantity txData, tyData, tzData;
    private Quantity r1Data, r2Data, r3Data;
    private Text orderParam;
    private DataArray outputMatrix;
    private char[] rotAxes = {'X','Y','Z'};
    private double[] rotValues;
    private Mat4d newMatrix;
    
    
    public Pos2Mat4_Process()
    {
        VectorHelper vecHelper = new VectorHelper();
        
        // create location input
        Vector tData = vecHelper.newLocationVectorXYZ(null, null, "m");
        tData.setReferenceFrame(null);
        tData.setReferenceFrame(null);
        inputData.add("location", tData);
        txData = (Quantity)tData.getComponent(0);
        tyData = (Quantity)tData.getComponent(1);
        tzData = (Quantity)tData.getComponent(2);
        
        // create euler input
        Vector eulerData = vecHelper.newEulerAngles();
        eulerData.setReferenceFrame(null);
        inputData.add("orientation", eulerData);
        r1Data = (Quantity)eulerData.getComponent(0);
        r2Data = (Quantity)eulerData.getComponent(1);
        r3Data = (Quantity)eulerData.getComponent(2);
        
        // create matrix output
        outputMatrix = vecHelper.newMatrix(4, 4);
        outputData.add("posMatrix", outputMatrix);
        
        // create rot order param
        orderParam = vecHelper.newText();
        AllowedTokens values = vecHelper.newAllowedTokens();
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
        
        rotValues = new double[3];
        newMatrix = new Mat4d();
    }
    
    
    @Override
    public void execute() throws SMLException
    {
        double tx = 0.0;
        double ty = 0.0;
        double tz = 0.0;
        
        if (txData != null)
            tx = txData.getData().getDoubleValue();

        if (tyData != null)
            ty = tyData.getData().getDoubleValue();

        if (tzData != null)
            tz = tzData.getData().getDoubleValue();

        if (r1Data != null)
            rotValues[0] = r1Data.getData().getDoubleValue();
  
        if (r2Data != null)
            rotValues[1] = r2Data.getData().getDoubleValue();
  
        if (r3Data != null)
            rotValues[2] = r3Data.getData().getDoubleValue();
  
        // set up rotation matrices
        newMatrix.setIdentity();
 
        // rotate in reverse order as the one given
        // to get intrisic rotations (i.e. in rotating frames)
        for (int i=2; i>=0; i--)
        {
            char axis = rotAxes[i];
            double r = -rotValues[i];
            
            switch (axis)
            {
                case 'X':
                    newMatrix.rotateX(r);
                    break;
                    
                case 'Y':
                    newMatrix.rotateY(r);
                    break;
                    
                case 'Z':
                    newMatrix.rotateZ(r);
                    break;
            }
        }
        
        // translation part
        newMatrix.setTranslation(tx, ty, tz);
        
        // assign values to output matrix
        VecMathHelper.fromMat4d(newMatrix, outputMatrix.getData());
    }
}