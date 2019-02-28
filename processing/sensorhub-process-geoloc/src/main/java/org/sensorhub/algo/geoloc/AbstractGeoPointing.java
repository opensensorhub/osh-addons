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
 * Abtract base for classes computing pointing orientation
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Aug 11, 2015
 */
public abstract class AbstractGeoPointing implements IGeoPointing
{
    Ellipsoid datum;
    Vect3d northPole;
    private Vect3d heading = new Vect3d();
    private Vect3d other = new Vect3d();
    
    
    protected AbstractGeoPointing()
    {
        this(Ellipsoid.WGS84);
    }
    
    
    protected AbstractGeoPointing(Ellipsoid datum)
    {
        this.datum = datum;
        northPole = new Vect3d(0.0, 0.0, datum.getPolarRadius());
    }
    
    
    protected Mat3d computeMatrix(Vect3d up, Vect3d forward, int forwardAxis, int upAxis, Mat3d rotMatrix)
    {
        // deal with z down
        boolean inverseZ = false;
        if (upAxis < 0)
        {
            inverseZ = true;
            upAxis = -upAxis;
        }
        
        if ((forwardAxis == 1) && (upAxis == 3))
        {
            other.cross(up, forward);
            other.normalize();
            heading.cross(other, up);
            rotMatrix.setCols(heading, other, up);
        }

        else if ((forwardAxis == 1) && (upAxis == 2))
        {
            other.cross(forward, up);
            other.normalize();
            heading.cross(up, other);
            rotMatrix.setCols(heading, up, other);
        }

        else if ((forwardAxis == 2) && (upAxis == 1))
        {
            other.cross(up, forward);
            other.normalize();
            heading.cross(other, up);
            rotMatrix.setCols(up, heading, other);
        }

        else if ((forwardAxis == 2) && (upAxis == 3))
        {
            other.cross(forward, up);
            other.normalize();
            heading.cross(up, other);
            rotMatrix.setCols(other, heading, up);
        }

        else if ((forwardAxis == 3) && (upAxis == 1))
        {
            other.cross(forward, up);
            other.normalize();
            heading.cross(up, other);
            rotMatrix.setCols(up, other, heading);
        }

        else if ((forwardAxis == 3) && (upAxis == 2))
        {
            other.cross(up, forward);
            other.normalize();
            heading.cross(other, up);
            rotMatrix.setCols(other, up, heading);
        }
        
        if (inverseZ)
        {
            rotMatrix.m02 = -rotMatrix.m02;
            rotMatrix.m12 = -rotMatrix.m12;
            rotMatrix.m22 = -rotMatrix.m22;
        }
        
        return rotMatrix;
    }
    
    
    /**
     * Computes a vector pointing to geographic north from ECEF location
     * @param ecefLoc position in ECEF
     * @param northDir vector that can hold result
     * @return vector toward north pole expressed in ECEF frame
     */
    public Vect3d getEcefVectorToNorth(Vect3d ecefLoc, Vect3d northDir)
    {
        northDir.sub(northPole, ecefLoc);
        return northDir;
    }
}
