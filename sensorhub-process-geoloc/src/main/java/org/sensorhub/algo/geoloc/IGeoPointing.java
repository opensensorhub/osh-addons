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


public interface IGeoPointing
{
    
    /**
     * Computes rotation matrix to go from ENU coordinates to ECEF coordinates
     * @param ecefLoc ECEF location at which to compute pointing matrix
     * @param rotM 3x3 matrix used to store the result
     */
    public void getRotationMatrixENUToECEF(Vect3d ecefLoc, Mat3d rotM);
    
    
    /**
     * Computes rotation matrix to go from NED coordinates to ECEF coordinates
     * @param ecefLoc ECEF location at which to compute pointing matrix
     * @param rotM 3x3 matrix used to store the result
     */
    public void getRotationMatrixNEDToECEF(Vect3d ecefLoc, Mat3d rotM);
}
