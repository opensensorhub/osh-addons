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

import org.sensorhub.impl.process.geoloc.Ellipsoid;
import org.sensorhub.vecmath.Vect3d;


/**
 * <p><b>Title:</b><br/>
 * Ray Intersect Ellipsoid
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Intersect the surface of an ellipsoid with a ray.
 *          Ellipsoid is our 'External Data'.  The ellipsoid
 *          is assumed to have its center at the origin.
 *          Three doubles are used to define the ellipsoid's
 *          intersections with the x,y, & z axes respectively.
 *          The intersecting ray should consist of a three-tuple
 *          defining the ray vertex, and another three-tuple
 *          defining the ray direction.
 *          NOTE:   When initializing InputKeys vector, the user
 *                  must declare them in the following order:
 *                  vertexX, vertexY, vertexZ, directionX, dirY, dirZ
 *          This is necessary because you can call this transform
 *          using any number of different Cartesian coordinate frames.
 *          The transform won't be able to figure out how to index
 *          the columns of the source node otherwise.
 *          The same holds true for output, except we're only worried
 *          about position, so the keys must be declared in this order:
 *                  vertexX, vertexY, vertexZ
 *          This illustrates an 'issue' with using the DataKey
 *          scheme in the first place.
 * </p>
 *
 * <p>Copyright (c) 2005</p>
 * @author Pete Conway, Tony Cook, Alexandre Robin
 * @dateMar 3, 1998
 */
public class RayIntersectEllipsoid
{
	boolean foundFlag = false;
    double[] R = new double[3]; 


	//constructor looking for ellipsoid dimensions
	public RayIntersectEllipsoid(double xI, double yI, double zI)
	{
        R[0] = xI;
        R[1] = yI;
        R[2] = zI;
	}


	public RayIntersectEllipsoid(double[] intercept)
	{
        R[0] = intercept[0];
        R[1] = intercept[1];
        R[2] = intercept[2];
	}


	public RayIntersectEllipsoid(Ellipsoid datum)
	{
		R[0] = datum.getEquatorRadius();
        R[1] = datum.getEquatorRadius();
        R[2] = datum.getPolarRadius();
	}
	

	public RayIntersectEllipsoid(Ellipsoid datum, double metersAboveEllipsoid)
	{
		R[0] = datum.getEquatorRadius() + metersAboveEllipsoid;
        R[1] = datum.getEquatorRadius() + metersAboveEllipsoid;
        R[2] = datum.getPolarRadius() + metersAboveEllipsoid;
	}


	public double[] getIntersection(Vect3d vertex, Vect3d direction)
	{
		foundFlag = true;

		double[] result = new double[3];
		double[] U0 = new double[3];
        double[] U1 = new double[3];
		double[] P0 = new double[3];
		double[] P1 = new double[3];
		
		// get ray direction in ec coordinates
        U0[0] = direction.x;
        U0[1] = direction.y;
        U0[2] = direction.z;
        
        // get ray origin in ec coordinate
        P0[0] = vertex.x;
        P0[1] = vertex.y;
        P0[2] = vertex.z;
        
        // scale vectors using ellipsoid radius
        for (int i=0; i<3; i++)
        {
            P1[i] = P0[i] / R[i];
            U1[i] = U0[i] / R[i];
        }
        
        // computes polynomial coefficients (at^2 + bt + c = 0)
        double a = 0.0;
        double b = 0.0;
        double c = -1.0;
        for (int i=0; i<3; i++)
        {
            a += U1[i]*U1[i];
            b += P1[i]*U1[i];
            c += P1[i]*P1[i];
        }

        // computes discriminant
        double dscrm = b * b - a * c;
        double scalar = 0.0;
        
        // case of no valid solution
        if (dscrm < 0.0 || c < 0.0)
        {
            // set max ray length to geocentric distance
            scalar = Math.sqrt(c/a);
            //System.err.println("No intersection found");
            foundFlag = false;
        }
        
        // case of P exactly on ellipsoid surface
        else if (c == 0.0)
        {
            for (int i=0; i<3; i++)
                result[i] = P0[i];
            return result;
        }
        
        // always use smallest solution
        else if (b > 0.0)
        {
            scalar = (-b + Math.sqrt(dscrm)) / a;
        }
        else
        {
            scalar = (-b - Math.sqrt(dscrm)) / a;
        }
                    
        // assign new values to intersection point output
        for (int i=0; i<3; i++)
            result[i] = P0[i] + U0[i]*scalar;

        return result; 
	}


    public boolean getFoundFlag()
    {
        return foundFlag;
    }
}
