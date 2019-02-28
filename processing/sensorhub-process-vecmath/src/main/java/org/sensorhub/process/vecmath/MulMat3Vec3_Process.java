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
import net.opengis.swe.v20.Vector;
import org.sensorhub.algo.vecmath.Mat3d;
import org.sensorhub.algo.vecmath.Vect3d;
import org.vast.process.SMLException;
import org.vast.sensorML.ExecutableProcessImpl;


/**
 * <p>
 * Multiplication of a 3D matrix by a 3D vector Vres = M.V 
 * </p>
 *
 * @author Alexandre Robin & Gregoire Berthiau
 * @date Mar 7, 2007
 */
public class MulMat3Vec3_Process extends ExecutableProcessImpl
{
	private DataArray mat;
    private Vector vect;
    private Vector vres;
    private Mat3d m;
    private Vect3d v;
    
    
    public MulMat3Vec3_Process()
    {
        VecMathHelper sweHelper = new VecMathHelper();
        
        // create matrix input M
        mat = sweHelper.newMatrix(3, 3);
        inputData.add("M", mat);
        
        // create vector input V
        vect = sweHelper.newVector3(null, null);
        inputData.add("V", vect);
     
        // create result vector output
        vres = sweHelper.newVector3(null, null);
        outputData.add("Vres", vres);
    }

    
    @Override
    public void init() throws SMLException
    {
        m = new Mat3d();
        v = new Vect3d();
    }
    

    @Override
    public void execute() throws SMLException
    {
        VecMathHelper.toMat3d(mat.getData(), m);
        VecMathHelper.toVect3d(vect.getData(), v);

        m.mul(v, v);
    	
        VecMathHelper.fromVect3d(v, vres.getData());		
    } 
}