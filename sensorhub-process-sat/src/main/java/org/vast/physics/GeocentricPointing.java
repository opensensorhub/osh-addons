/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License Version
 1.1 (the "License"); you may not use this file except in compliance with
 the License. You may obtain a copy of the License at
 http://www.mozilla.org/MPL/MPL-1.1.html
 
 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.
 
 The Original Code is the "SensorML DataProcessing Engine".
 
 The Initial Developer of the Original Code is Spotimage S.A.
 Portions created by the Initial Developer are Copyright (C) 2007
 the Initial Developer. All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alexandre.robin@spotimage.fr>
 
******************************* END LICENSE BLOCK ***************************/

package org.vast.physics;

import org.vast.math.*;


/**
 * <p><b>Title:</b><br/>
 * Geocentric Pointing
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Static routines to compute ECEF geocentric orientation
 * at a given ECEF position 
 * </p>
 *
 * <p>Copyright (c) 2008</p>
 * @author Alexandre Robin
 * @date Apr 23, 2008
 */
public class GeocentricPointing extends NadirPointing
{
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
        Vector3d up = position.copy();       
        up.normalize();
        return computeMatrix(up, velocity, forwardAxis, upAxis);        
    }
}
