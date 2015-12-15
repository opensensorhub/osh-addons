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
 * Coordinate transformations between various earth reference frames
 * </p>
 * <p>
 * <b>This class is NOT thread-safe</b>
 * </p>
 *
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Aug 9, 2015
 */
public class GeoTransforms
{
    private final static double RTD = 180. / Math.PI;
    private final static double DTR = Math.PI / 180.;
    private final static double EARTH_OMEGA = 7.2921158553e-5;
    
    private Ellipsoid datum;
    
    
    public GeoTransforms()
    {
        this.datum = Ellipsoid.WGS84;
    }
    
    
    public GeoTransforms(Ellipsoid datum)
    {
        this.datum = datum;
    }
    
    
    /**
     * Converts from Lat/Lon/Alt to ECEF coordinates.</br>
     * Note that this is safe for aliasing (i.e. ecef and lla can be the same object)
     * @param lla vector containing LLA location with x=lon, y=lat, z=alt and angles in radians
     * @param ecef vector to receive resulting ECEF coordinates (must not be null)
     * @return reference to computed ecef vector for chaining other operations
     */
    public final Vect3d LLAtoECEF(Vect3d lla, Vect3d ecef)
    {    
        double a = datum.getEquatorRadius();
        double e2 = datum.getE2();
        
        double lat = lla.y;
        double lon = lla.x;
        double alt = lla.z;

        double sinLat = Math.sin(lat);
        double cosLat = Math.cos(lat);
        double N = a / Math.sqrt(1.0 - e2 * sinLat * sinLat);

        ecef.x = (N + alt) * cosLat * Math.cos(lon);
        ecef.y = (N + alt) * cosLat * Math.sin(lon);
        ecef.z = (N * (1.0 - e2) + alt) * sinLat;
        
        return ecef;
    }
        
    
    /**
     * Converts from ECEF to Lat/Lon/Alt coordinates.<br/>
     * Note that this is safe for aliasing (i.e. ecef and lla can be the same object).<br/>
     * Order of coordinates in resulting vector is x=lon, y=lat, z=alt and angles in radians
     * @param ecef vector containing ECEF location
     * @param lla vector to receive resulting LLA coordinates (must not be null)
     * @return reference to computed lla vector for chaining other operations
     */
    public final Vect3d ECEFtoLLA(Vect3d ecef, Vect3d lla)
    {
        // method from Peter Dana
        // conversion is not exact, it provides cm accuracy for altitudes up to 1000km
        double a = datum.getEquatorRadius();
        double b = datum.getPolarRadius();
        double e2 = datum.getE2();
        
        double x = ecef.x;
        double y = ecef.y;
        double z = ecef.z;
        
        double longitude = Math.atan2(y, x);
        double ePrimeSquared = (a*a - b*b)/(b*b);
        double p = Math.sqrt(x*x + y*y);
        
        if (p == 0.0) // case of lat = 90° or -90°
        {
            lla.x = 0.0;
            lla.y = Math.signum(z) * Math.PI/2;
            lla.z = z - Math.signum(z)*b;
        }
        else
        {
            double theta = Math.atan((z*a)/(p*b));
            double sineTheta = Math.sin(theta);
            double cosTheta = Math.cos(theta);
            double top = z + ePrimeSquared * b * sineTheta * sineTheta * sineTheta;
            double bottom = p - e2 * a * cosTheta * cosTheta * cosTheta;
            double geodeticLat = Math.atan2(top, bottom);
            double sineLat = Math.sin(geodeticLat);
            double N = a / Math.sqrt( 1 - e2 * sineLat * sineLat);        
            double altitude = (p / Math.cos(geodeticLat)) -  N;
    
            // maintain longitude btw -PI and PI
            if (longitude > Math.PI)
            	longitude -= 2*Math.PI;
            
            else if (longitude < -Math.PI)
            	longitude += 2*Math.PI;
            
            lla.x = longitude;
            lla.y = geodeticLat;
            lla.z = altitude;
        }
        
        return lla;
    }
    
    
    /**
     * Converts from ECEF to ECI coordinates.<br/>
     * Note that this is safe for aliasing (i.e. ecef and eci can be the same object).<br/>
     * @param julianTime julian time (seconds past 1970 epoch) at which to compute the transformation
     * @param ecef vector containing ECEF location or velocity
     * @param eci vector to receive resulting ECI coordinates (must not be null)
     * @param isVelocity true if vector is a velocity (in this case, we need to account for the earth angular velocity)
     * @return reference to provided eci vector for chaining other operations
     */
    public final Vect3d ECEFtoECI(double julianTime, Vect3d ecef, Vect3d eci, boolean isVelocity)
    {
        double x = ecef.x;
        double y = ecef.y;
        double z = ecef.z;        
        double GHA = computeGHA(julianTime);
    	
        // rotate around Z
        double c = Math.cos(-GHA);
        double s = Math.sin(-GHA);
        double xo = c * x + s * y;
        double yo = -s * x + c * y;
        
        // if velocity, account for angular velocity of the earth
        if (isVelocity)
        {
	        // compute velocity cross earthOmega [0,0,0,7.292e-5]	        
	        double dXo = yo * EARTH_OMEGA; // - Z * 0.0;
	        double dYo = - xo * EARTH_OMEGA; // + Z * 0.0; 
	        xo += dXo;
	        yo += dYo;
        }        

        eci.x = xo;
        eci.y = yo;
        eci.z = z;
        return eci;
    }
    
    
    /**
     * Converts from ECI to ECEF coordinates.<br/>
     * Note that this is safe for aliasing (i.e. eci and ecef can be the same object).<br/>
     * @param julianTime julian time (seconds past 1970 epoch) at which to compute the transformation
     * @param eci vector containing ECI location or velocity
     * @param ecef vector to receive resulting ECEF coordinates (must not be null)
     * @param isVelocity true if vector is a velocity (in this case, we need to account for the earth angular velocity)
     * @return reference to provided ecef vector for chaining other operations
     */
    public final Vect3d ECItoECEF(double julianTime, Vect3d eci, Vect3d ecef, boolean isVelocity)
    {
        double x = eci.x;
        double y = eci.y;
        double z = eci.z;        
        double GHA = computeGHA(julianTime);

        // rotate around Z
        double c = Math.cos(GHA);
        double s = Math.sin(GHA);
        double xo = c * x + s * y;
        double yo = -s * x + c * y;
                
        // if velocity, account for angular velocity of the earth
        if (isVelocity)
        {
	        // compute velocity cross earthOmega [0,0,0,7.292e-5]	        
	        double dXo = yo * EARTH_OMEGA; // - Z * 0.0;
	        double dYo = - xo * EARTH_OMEGA; // + Z * 0.0; 
	        xo -= dXo;
	        yo -= dYo;
        }

        ecef.x = xo;
        ecef.y = yo;
        ecef.z = z;
        return ecef;
    }
    
    
    /**
     * Compute Greenwhich Hour Angle.<br/>
     * This gives the earth rotation angle around Z axis with respect to ECI frame.
     * @param julianTime julian time (seconds past 1970 epoch) at which to compute the angle
     * @return rotation angle in radians
     */
    public final double computeGHA(double julianTime)
    {
    	//Compute Greenwhich Hour Angle (GHA)
        /* System generated locals */
        double d__1, d__2, d__3;

        /* Local variables */
        double tsec, tday, gmst, t, omega, tfrac, tu, dat;

        /*     INPUT IS TIME "secondsSince1970" IN SECONDS AND "TDAY" */
        /*     WHICH IS WHOLE DAYS FROM 1970 JAN 1 0H */
        /*     THE OUTPUT IS GREENWICH HOUR ANGLE IN DEGREES */
        /*     XOMEGA IS ROTATION RATE IN DEGREES/SEC */

        /*     FOR COMPATABILITY */
        tday = (double) ((int) (julianTime / 86400.));
        tsec = julianTime - tday*86400;

        /*     THE NUMBER OF DAYS FROM THE J2000 EPOCH */
        /*     TO 1970 JAN 1 0H UT1 IS -10957.5 */
        t = tday - (float) 10957.5;
        tfrac = tsec / 86400.;
        dat = t;
        tu = dat / 36525.;

        /* Computing 2nd power */
        d__1 = tu;

        /* Computing 3rd power */
        d__2 = tu;
        d__3 = d__2;
        gmst = tu * 8640184.812866 + 24110.54841 + d__1 * d__1 * .093104 - d__3 * (d__2 * d__2) * 6.2e-6;

        /*     COMPUTE THE EARTH'S ROTATION RATE */
        /* Computing 2nd power */
        d__1 = tu;
        omega = tu * 5.098097e-6 + 86636.55536790872 - d__1 * d__1 * 5.09e-10;

        /*     COMPUTE THE GMST AND GHA */
        //  da is earth nutation - currently unused
        double da = 0.0;
        gmst = gmst + omega * tfrac + da * RTD * 86400. / 360.;
        gmst = gmst % 86400;
        if (gmst < 0.)
            gmst += 86400.;
        gmst = gmst / 86400. * 360.;

        //  returns gha in radians
        gmst = gmst * DTR;
        
        return gmst;
    }
}
