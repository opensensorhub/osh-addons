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
 * Ellipsoid (i.e. Datum) used for coordinate transformations.
 * </p>
 * <p>
 * <b>This class is immutable and thus thread-safe</b>
 * </p>
 *
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Aug 10, 2015
 */
public class Ellipsoid
{
    private double equatorRadius;
    private double polarRadius;
    private double f;
    private double e2;
    private Vect3d deltaOrigin;

    
    private Ellipsoid()
    {
        equatorRadius = Double.NaN;
        polarRadius = Double.NaN;
        f = Double.NaN;
        deltaOrigin = new Vect3d();
    }
    
    
    public double getEquatorRadius()
    {
        return equatorRadius;
    }


    public double getPolarRadius()
    {
        return polarRadius;
    }


    public double getFlattening()
    {
        return f;
    }


    public double getE2()
    {
        return e2;
    }


    public Vect3d getDeltaOrigin()
    {
        return deltaOrigin;
    }


    public static class Builder
    {
        Ellipsoid obj = new Ellipsoid();
        
        public Builder setEquatorRadius(final double re)
        {
            obj.equatorRadius = re;
            return this;
        }        
        
        public Builder setPolarRadius(final double rp)
        {
            obj.polarRadius = rp;
            return this;
        }
        
        public Builder setDeltaOrigin(final Vect3d deltaOrigin)
        {
            obj.deltaOrigin.set(deltaOrigin);
            return this;          
        }
        
        public Builder setFlattening(final double f)
        {
            obj.f = f;
            return this;
        }
        
        public Ellipsoid build()
        {
            if (Double.isNaN(obj.equatorRadius))
                throw new IllegalStateException("Equator radius must be set");
            
            if (Double.isNaN(obj.polarRadius) && Double.isNaN(obj.f))
                throw new IllegalStateException("Either polar radius or flattening must be set");
            
            // compute either polar radius or flattening
            if (Double.isNaN(obj.polarRadius))
                obj.polarRadius = obj.equatorRadius * (1. - obj.f);
            else
                obj.f = 1. - obj.polarRadius / obj.equatorRadius;
            
            // precompute e2
            obj.e2 = 2*obj.f - obj.f*obj.f;
            
            return obj;
        }
    }
    
    
    ///// COMMON ELLIPSOIDAL DATUMS //////
    
    public static Ellipsoid WGS84 = new Builder()
        .setEquatorRadius(6378137.0)
        .setFlattening(1. / 298.257223563)
        .build();
    
    
    public static Ellipsoid GRS80 = new Builder()
        .setEquatorRadius(6378137.0)
        .setFlattening(1. / 298.257222101)
        .build();
}
