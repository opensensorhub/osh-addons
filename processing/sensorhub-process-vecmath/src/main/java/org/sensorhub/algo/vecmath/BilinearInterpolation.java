/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.algo.vecmath;


/**
 * <p>
 * Class for computing bi-linear interpolation for a function value known
 * at the four surrounding corners.
 * </p>
 * <p>
 * <b>This class is NOT thread-safe</b>
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Nov 13, 2015
 */
public class BilinearInterpolation
{
    Vect3d c11, c12, c21, c22;


    public BilinearInterpolation()
    {

    }


    public void setCorners(Vect3d c11, Vect3d c12, Vect3d c21, Vect3d c22)
    {
        this.c11 = c11;
        this.c12 = c12;
        this.c21 = c21;
        this.c22 = c22;
    }


    public double interpolate(double x, double y)
    {
        double result = 0.0;
        double delX = c21.x - c11.x;
        double delY = c12.y - c11.y;
        double delXY = delX * delY;

        double term1 = c11.z * (c21.x - x) * (c22.y - y);
        double term2 = c21.z * (x - c11.x) * (c22.y - y);
        double term3 = c12.z * (c21.x - x) * (y - c11.y);
        double term4 = c22.z * (x - c11.x) * (y - c11.y);

        result = (term1 + term2 + term3 + term4) / delXY;

        return result;
    }
}
