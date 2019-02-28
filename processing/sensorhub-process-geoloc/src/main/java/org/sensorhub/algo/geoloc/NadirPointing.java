/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.algo.geoloc;

import org.sensorhub.algo.vecmath.Mat3d;
import org.sensorhub.algo.vecmath.Vect3d;


/**
 * <p>
 * Routines to compute nadir pointing orientation at a given ECEF location 
 * </p>
 * <p>
 * <b>This class is NOT thread-safe</b>
 * </p>
 *
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @date Aug 9, 2015
 */
public class NadirPointing extends AbstractGeoPointing
{
    GeoTransforms geoConv;
    Vect3d lla = new Vect3d();
    Vect3d upDir = new Vect3d();
    Vect3d northDir = new Vect3d();
    
    // TODO implement automatic yaw steering in computeMatrix
	
	
    public NadirPointing()
    {
        super();
        geoConv = new GeoTransforms(datum);
    }
    
    
    public NadirPointing(Ellipsoid datum)
    {
        super(datum);
        geoConv = new GeoTransforms(datum);
    }
    
    
    public NadirPointing(GeoTransforms geoTransforms)
    {
        geoConv = geoTransforms;
    }
    
    
	/**
     * Computes the rotation matrix to transform from local nadir oriented frame to ECEF
     * @param ecefLoc location in ECEF
     * @param forwardDir forward direction vector in ECEF.
     *        (this doesn't have to be the exact forward direction but rather form a plane with the up direction in which the forward axis will be placed)
     * @param forwardAxis number (1:x, 2:y, 3:z) to use as the forward direction (in the plane of velocity)
     * @param upAxis number (1:x, 2:y, 3:z) to use as the up direction
	 * @param rotMatrix matrix used to store the result
     */
    public void getRotationMatrixLocalToECEF(final Vect3d ecefLoc, final Vect3d forwardDir, int forwardAxis, int upAxis, Mat3d rotMatrix)
    {
        // compute up direction
        getUpDirection(ecefLoc, upDir);

        // compute rot matrix
        computeMatrix(upDir, forwardDir, forwardAxis, upAxis, rotMatrix);
    }
    
    
    @Override
    public void getRotationMatrixENUToECEF(Vect3d ecefLoc, Mat3d rotM)
    {
    	getEcefVectorToNorth(ecefLoc, northDir);
    	getRotationMatrixLocalToECEF(ecefLoc, northDir, 2, 3, rotM);
    }
    
    
    @Override
    public void getRotationMatrixNEDToECEF(Vect3d ecefLoc, Mat3d rotM)
    {
    	getEcefVectorToNorth(ecefLoc, northDir);
    	getRotationMatrixLocalToECEF(ecefLoc, northDir, 1, -3, rotM);
    }
    
    
    public final void getUpDirection(Vect3d ecefLoc, Vect3d upDir)
    {
        geoConv.ECEFtoLLA(ecefLoc, lla);
        lla.z += 100.0;
        geoConv.LLAtoECEF(lla, upDir);
        upDir.sub(ecefLoc);
        upDir.normalize();
    }
}
