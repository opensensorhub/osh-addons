/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.process.geoloc;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.Matrix;
import net.opengis.swe.v20.Vector;
import org.sensorhub.algo.geoloc.Ellipsoid;
import org.sensorhub.algo.geoloc.GeoTransforms;
import org.sensorhub.algo.geoloc.NadirPointing;
import org.sensorhub.algo.vecmath.Mat3d;
import org.sensorhub.algo.vecmath.Mat4d;
import org.sensorhub.algo.vecmath.Quat4d;
import org.sensorhub.algo.vecmath.Vect3d;
import org.sensorhub.process.vecmath.VecMathHelper;
import org.vast.process.SMLException;
import org.vast.sensorML.ExecutableProcessImpl;
import org.vast.swe.SWEConstants;
import org.vast.swe.helper.GeoPosHelper;


/**
 * <p>
 * Generates the 4x4 homogeneous and 3x3 rotation matrices allowing
 * to transform from local (sensor) coordinates to ECEF coordinates. 
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @date Nov 13, 2015
 */
public class ECEFPositionMatrix_Process extends ExecutableProcessImpl
{
    private Vector platformLoc, platformAtt;
    private Vector mountLoc, mountOrient;
    private Matrix rotMatrix, posMatrix;
    private GeoTransforms transforms;
    private NadirPointing pointing;
    private boolean isEnu, isLatLon;
    private Vect3d locEcefPlat, locPlatMount;
    private Quat4d att;
    private Mat3d rotEcefEnu, rotEnuPlat, rotPlatMount;
    private Mat4d posMat;
    

    public ECEFPositionMatrix_Process()
    {
        GeoPosHelper geoHelper = new GeoPosHelper();
        
        //// INPUTS ////
        // location in earth ref frame (ECEF by default)
        platformLoc = geoHelper.newLocationVectorECEF(null);
        platformLoc.setDescription("Platform location in geodetic reference frame");
        inputData.add("platformLoc", platformLoc);
        isLatLon = false;
        
        // attitude in local ref frame (ENU by default)
        platformAtt = geoHelper.newQuatOrientationENU(null);
        platformAtt.setDescription("Platform attitude in local reference frame");
        inputData.add("platformAtt", platformAtt);
        isEnu = true;
        
        //// PARAMETERS ////
        // mounting location
        mountLoc = geoHelper.newLocationVectorXYZ(null, SWEConstants.NIL_UNKNOWN, "m");
        mountLoc.setDescription("Device location in platform reference frame");
        mountLoc.assignNewDataBlock();
        paramData.add("mountLoc", mountLoc);
        
        // mounting orientation 
        mountOrient = geoHelper.newQuatOrientation(null, SWEConstants.NIL_UNKNOWN);
        mountOrient.setDescription("Device orientation in platform reference frame");
        mountOrient.assignNewDataBlock();
        mountOrient.getData().setDoubleValue(0, 1.0); // set to identity
        paramData.add("mountOrient", mountOrient);
        
        //// OUTPUTS ////
        rotMatrix = geoHelper.newMatrix(3, 3);
        outputData.add("rotMatrix", rotMatrix);
        
        posMatrix = geoHelper.newMatrix(4, 4);
        outputData.add("posMatrix", posMatrix);
    }

    
    public void init() throws SMLException
    {
        transforms = new GeoTransforms(Ellipsoid.WGS84);
        pointing = new NadirPointing(transforms);
        locEcefPlat = new Vect3d();
        locPlatMount = new Vect3d();
        att = new Quat4d();
        rotEcefEnu = new Mat3d();
        rotEnuPlat = new Mat3d();
        rotPlatMount = new Mat3d();
        posMat = new Mat4d().setIdentity();
        
        // precompute mount position vector and matrix
        DataBlock locData = mountLoc.getData();
        locPlatMount.x = locData.getDoubleValue(0);
        locPlatMount.y = locData.getDoubleValue(1);
        locPlatMount.z = locData.getDoubleValue(2);
                
        DataBlock attData = mountOrient.getData();
        att.s = attData.getDoubleValue(0); // scalar is first
        att.x = attData.getDoubleValue(1);
        att.y = attData.getDoubleValue(2);
        att.z = attData.getDoubleValue(3);
        att.toRotationMatrix(rotPlatMount);
    }
    
    
    // TODO implement method to merge with XML defined inputs
    // this would enable overriding ENU to NED and ECEF to LLA
   
    
    public void execute() throws SMLException
    {
        DataBlock locData = platformLoc.getData();
        locEcefPlat.x = locData.getDoubleValue(0);
        locEcefPlat.y = locData.getDoubleValue(1);
        locEcefPlat.z = locData.getDoubleValue(2);
        
        DataBlock attData = platformAtt.getData();
        att.s = attData.getDoubleValue(0); // scalar is first
        att.x = attData.getDoubleValue(1);
        att.y = attData.getDoubleValue(2);
        att.z = attData.getDoubleValue(3);
        
        // deal with LatLon coordinates if needed
        if (isLatLon)
        {
            // flip X/Y
            double oldx = locEcefPlat.x;
            locEcefPlat.x = locEcefPlat.y;
            locEcefPlat.y = oldx;
            
            // transform to ECEF
            transforms.LLAtoECEF(locEcefPlat, locEcefPlat);
        }
        
        // get rotation matrix from ECEF to ENU
        if (isEnu)
            pointing.getRotationMatrixENUToECEF(locEcefPlat, rotEcefEnu);
        else
            pointing.getRotationMatrixNEDToECEF(locEcefPlat, rotEcefEnu);
        
        // get rotation matrix from ENU to local frame
        att.toRotationMatrix(rotEnuPlat);
        
        // combine 3 rotations
        rotEcefEnu.mul(rotEnuPlat).mul(rotPlatMount);
        
        // write out rotation matrix
        VecMathHelper.fromMat3d(rotEcefEnu, rotMatrix.getData());
        
        // write out 4x4 transform matrix
        posMat.set(rotEcefEnu);
        posMat.setTranslation(locEcefPlat.add(locPlatMount));
        VecMathHelper.fromMat4d(posMat, posMatrix.getData());
    }
}
