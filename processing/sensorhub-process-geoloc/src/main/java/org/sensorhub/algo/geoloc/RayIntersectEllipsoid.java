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

import org.sensorhub.algo.vecmath.Vect3d;


/**
 * <p>
 * Computes the intersection between a ray given its origin and direction
 * and a perfect ellipsoid. This is usually used in ECEF frame to compute
 * intersection of remote sensor look directions with the earth ellipsoid.
 * </p>
 * <p>
 * <b>This class is NOT thread-safe</b>
 * </p>
 *
 * @author Pete Conway, Tony Cook, Alexandre Robin
 * @since 1998
 */
public class RayIntersectEllipsoid
{
    Ellipsoid datum;
    double rx, ry, rz;
    
        
    public RayIntersectEllipsoid(double rx, double ry, double rz)
    {
        this.rx = rx;
        this.ry = ry;
        this.rz = rz;
    }
    
    
	public RayIntersectEllipsoid(Ellipsoid datum)
	{
		this(datum, 0.0);
	}
	

	public RayIntersectEllipsoid(Ellipsoid datum, double metersAboveRefEllipsoid)
	{
	    this.datum = datum;
	    setHeightAdjustment(metersAboveRefEllipsoid);
	}
	
	
	public void setHeightAdjustment(double metersAboveRefEllipsoid)
	{
	    if (datum != null)
	    {
	        rx = ry = datum.getEquatorRadius() + metersAboveRefEllipsoid;
	        rz = datum.getPolarRadius() + metersAboveRefEllipsoid;
	    }
	}


	/**
	 * Computes the intersection
	 * @param vertex origin of the ray
	 * @param dir direction of the ray (must be a unit vector since no normalization is done)
	 * @param result intersection location
	 * @return true if intersection was found, false otherwise
	 */
	public boolean computeIntersection(Vect3d vertex, Vect3d dir, Vect3d result)
	{
		// scale vectors using ellipsoid radius
        double px = vertex.x / rx;
        double py = vertex.y / ry;
        double pz = vertex.z / rz;
        double ux = dir.x / rx;
        double uy = dir.y / ry;
        double uz = dir.z / rz;
        
        // computes polynomial coefficients (at^2 + bt + c = 0)
        double a = ux*ux + uy*uy + uz*uz;
        double b = px*ux + py*uy + pz*uz;
        double c = -1.0 + px*px + py*py + pz*pz;

        // computes discriminant
        double dscrm = b * b - a * c;
        double scalar = 0.0;
        boolean found = true;
        
        // case of no valid solution
        if (dscrm < 0.0)
        {
            // set max ray length to geocentric distance
            scalar = Math.sqrt(c/a);
            found = false;
        }
        
        // case of P exactly on ellipsoid surface
        else if (c == 0.0)
        {
            result.set(vertex);
            return true;
        }
        
        // always use smallest positive solution
        else
        {
            double sqrtDscrm = Math.sqrt(dscrm);
            if (b >= 0.0 || c < 0.0) // if origin is inside ellipsoid
                scalar = (-b + sqrtDscrm) / a;
            else
                scalar = (-b - sqrtDscrm) / a;
        }
                    
        // assign new values to intersection output
        result.scale(dir, scalar);
        result.add(vertex);
        
        return found; 
	}
}
