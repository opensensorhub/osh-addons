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
 * Routines to compute geocentric pointing orientation at a given ECEF location 
 * </p>
 * <p>
 * <b>This class is NOT thread-safe</b>
 * </p>
 *
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @date Aug 15, 2015
 */
public class GeocentricPointing extends NadirPointing
{
    
    /**
     * Computes the rotation matrix to transform from local geocentric oriented frame to ECEF
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
        upDir.set(ecefLoc);
        upDir.normalize();

        // compute rot matrix
        computeMatrix(upDir, forwardDir, forwardAxis, upAxis, rotMatrix);
    }
}
