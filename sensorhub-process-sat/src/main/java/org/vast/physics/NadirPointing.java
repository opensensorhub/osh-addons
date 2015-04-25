/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License Version
 1.1 (the "License"); you may not use this file except in compliance with
 the License. You may obtain a copy of the License at
 http://www.mozilla.org/MPL/MPL-1.1.html
 
 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.
 
 The Original Code is the "SensorML DataProcessing Engine".
 
 The Initial Developer of the Original Code is the VAST team at the University of Alabama in Huntsville (UAH). <http://vast.uah.edu> Portions created by the Initial Developer are Copyright (C) 2007 the Initial Developer. All Rights Reserved. Please Contact Mike Botts <mike.botts@uah.edu> for more information.
 
 Contributor(s): 
    Alexandre Robin <robin@nsstc.uah.edu>
 
******************************* END LICENSE BLOCK ***************************/

package org.vast.physics;

import org.vast.math.*;


/**
 * <p><b>Title:</b><br/>
 * Nadir Pointing
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Static routines to compute ECEF nadir orientation
 * at a given ECEF position 
 * </p>
 *
 * <p>Copyright (c) 2005</p>
 * @author Alexandre Robin
 * @date Nov 29, 2005
 */
public class NadirPointing
{
    // TODO implement automatic yaw steering in computeMatrix
	
	
	/**
     * Compute the rotation matrix to obtain nadir/north orientation
     * @param position vector in ECEF
     * @param velocity vector in ECEF
     * @param forwardAxis number (1:x, 2:y, 3:z) to use as the forward direction (in the plane of velocity)
     * @param upAxis number (1:x, 2:y, 3:z) to use as the up direction
     * @return the 3x3 rotation matrix
     */
    public static Matrix3d getRotationMatrix(Vector3d position, Vector3d velocity, int forwardAxis, int upAxis)
    {
        Vector3d up = new Vector3d();
        
        double[] lla = MapProjection.ECFtoLLA(position.x, position.y, position.z, new Datum());
        double[] ecf = MapProjection.LLAtoECF(lla[0], lla[1], -3e3, new Datum());
        Vector3d nearPoint = new Vector3d(ecf[0], ecf[1], ecf[2]);
        
        up.sub(position, nearPoint);
        up.normalize();

        return computeMatrix(up, velocity, forwardAxis, upAxis);
    }
    
    
    protected static Matrix3d computeMatrix(Vector3d up, Vector3d velocity, int forwardAxis, int upAxis)
    {
    	Vector3d heading = new Vector3d();
        Vector3d other = new Vector3d();
    	Matrix3d rotMatrix = null;
    	
    	// deal with z down
        boolean inverseZ = false;
        if (upAxis < 0)
        {
        	inverseZ = true;
        	upAxis = -upAxis;
        }
        
        if ((forwardAxis == 1) && (upAxis == 3))
        {
            other.cross(up, velocity);
            other.normalize();
            heading.cross(other, up);
            rotMatrix = new Matrix3d(heading, other, up);
        }

        else if ((forwardAxis == 1) && (upAxis == 2))
        {
            other.cross(velocity, up);
            other.normalize();
            heading.cross(up, other);
            rotMatrix = new Matrix3d(heading, up, other);
        }

        else if ((forwardAxis == 2) && (upAxis == 1))
        {
            other.cross(up, velocity);
            other.normalize();
            heading.cross(other, up);
            rotMatrix = new Matrix3d(up, heading, other);
        }

        else if ((forwardAxis == 2) && (upAxis == 3))
        {
            other.cross(velocity, up);
            other.normalize();
            heading.cross(up, other);
            rotMatrix = new Matrix3d(other, heading, up);
        }

        else if ((forwardAxis == 3) && (upAxis == 1))
        {
            other.cross(velocity, up);
            other.normalize();
            heading.cross(up, other);
            rotMatrix = new Matrix3d(up, other, heading);
        }

        else if ((forwardAxis == 3) && (upAxis == 2))
        {
            other.cross(up, velocity);
            other.normalize();
            heading.cross(other, up);
            rotMatrix = new Matrix3d(other, up, heading);
        }
        
        if (inverseZ)
        	for (int r=0; r<3; r++)
        		rotMatrix.setElement(r, 2, -rotMatrix.getElement(r, 2));

        return rotMatrix;
    }


    /**
     * Computes a vector pointing to north from ecf position
     * @param ecfPosition position in ECEF
     * @return vector toward north pole
     */
    public static Vector3d getEcfVectorToNorth(Vector3d ecfPosition)
    {
        double polarRadius = (new Datum()).polarRadius;
        Vector3d northPole = new Vector3d(0.0, 0.0, polarRadius);
        Vector3d res = new Vector3d();
        res.sub(northPole, ecfPosition);
        return res;
    }
    
    
    /**
     * Computes the ENU rotation matrix from position in ECEF
     * @param ecfPosition position in ECEF
     * @return ENU to ECEF rotation matrix
     */
    public static Matrix3d getENURotationMatrix(Vector3d ecfPosition)
    {
    	Vector3d north = getEcfVectorToNorth(ecfPosition);
    	return getRotationMatrix(ecfPosition, north, 2, 3);
    }
    
    
    /**
     * Computes the NED rotation matrix from position in ECEF
     * @param ecfPosition position in ECEF
     * @return NED to ECEF rotation matrix
     */
    public static Matrix3d getNEDRotationMatrix(Vector3d ecfPosition)
    {
    	Vector3d north = getEcfVectorToNorth(ecfPosition);
    	return getRotationMatrix(ecfPosition, north, 1, -3);
    }
}
