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


/**
 * <p><b>Title:</b><br/>
 * MapProjection
 * </p>
 *
 * <p><b>Description:</b><br/>
 * TODO MapProjection type description
 * </p>
 *
 * <p>Copyright (c) 2005</p>
 * @author Alexandre Robin
 * @date Oct 18, 2005
 */
public class MapProjection
{
	protected final static double RTD = 180. / Math.PI;
    protected final static double DTR = Math.PI / 180.;
	public final static double MAX_SINLAT = 0.9999;
    public final static double MAX_Y = Math.PI - 0.0001;
    
    
    public final static double [] LLAtoMerc(double lon, double lat, double alt)
    {
        double sinLat = Math.sin(lat);
        
        sinLat = Math.min(MAX_SINLAT, sinLat);
        sinLat = Math.max(-MAX_SINLAT, sinLat);
            
        double y = 0.5 * Math.log((1 + sinLat) / (1 - sinLat));
        return new double [] {lon, y, alt};
    }
    
    
    public final static double [] MerctoLLA(double x, double y, double alt)
    {
        double lat;
        
        if (Math.abs(y) < MAX_Y)
            lat = Math.PI/2 - 2 * Math.atan(Math.exp(-y));
        else
            lat = Math.signum(y)*Math.PI/2;
        
        return new double [] {x, lat, alt};
    }
    
    
    public final static double [] LLAtoECF(double lon, double lat, double altitude, Datum datum)
    {
        if (datum == null)
            datum = new Datum();
        
        double a = datum.equatorRadius;
        double e2 = datum.e2;

        double sinLat = Math.sin(lat);
        double cosLat = Math.cos(lat);
        double N = a / Math.sqrt(1.0 - e2 * sinLat * sinLat);

        double x = (N + altitude) * cosLat * Math.cos(lon);
        double y = (N + altitude) * cosLat * Math.sin(lon);
        double z = (N * (1.0 - e2) + altitude) * sinLat;

        return new double [] {x, y, z};
    }


    public final static double [] ECFtoLLA(double x, double y, double z, Datum datum)
    {
        if (datum == null)
            datum = new Datum();
        
        // Method from Peter Dana
        double a = datum.equatorRadius;
        double b = datum.polarRadius;
        double longitude = Math.atan2(y, x);
        double ePrimeSquared = (a*a - b*b)/(b*b);
        double p = Math.sqrt(x*x + y*y);
        double theta = Math.atan((z*a)/(p*b));
        double sineTheta = Math.sin(theta);
        double cosTheta = Math.cos(theta);
        double e2 = datum.e2;
        double top = z + ePrimeSquared * b * sineTheta * sineTheta * sineTheta;
        double bottom = p - e2 * a * cosTheta * cosTheta * cosTheta;
        double geodeticLat = Math.atan(top/bottom);
        double sineLat = Math.sin(geodeticLat);
        double N = a / Math.sqrt( 1 - e2 * sineLat * sineLat);
        double altitude = (p / Math.cos(geodeticLat)) -  N;

        // maintain longitude btw -PI and PI
        if (longitude > Math.PI)
        	longitude -= 2*Math.PI;
        
        else if (longitude < -Math.PI)
        	longitude += 2*Math.PI;
        
        return new double [] {longitude, geodeticLat, altitude};
    }
    
    
    public final static double [] ECFtoECI(double julianTime, double x, double y, double z, boolean isVelocity)
    {
    	double GHA = computeGHA(julianTime);

        //Rotate around Z
        double c = Math.cos(-GHA);
        double s = Math.sin(-GHA);
        double xo = c * x + s * y;
        double yo = -s * x + c * y;
        
        // if velocity, account for angular velocity of the earth
        if (isVelocity)
        {
	        // compute velocity cross earthOmega [0,0,0,7.292e-5]	        
	        double dXo = yo * 7.2921158553e-5; // - Z * 0.0;
	        double dYo = - xo * 7.2921158553e-5; // + Z * 0.0; 
	        xo += dXo;
	        yo += dYo;
        }        

        return new double[] {xo, yo, z};
    }
    
    
    public final static double [] ECItoECF(double julianTime, double x, double y, double z, boolean isVelocity)
    {
    	double GHA = computeGHA(julianTime);

        //Rotate around Z
        double c = Math.cos(GHA);
        double s = Math.sin(GHA);
        double xo = c * x + s * y;
        double yo = -s * x + c * y;
                
        // if velocity, account for angular velocity of the earth
        if (isVelocity)
        {
	        // compute velocity cross earthOmega [0,0,0,7.292e-5]	        
	        double dXo = yo * 7.2921158553e-5; // - Z * 0.0;
	        double dYo = - xo * 7.2921158553e-5; // + Z * 0.0; 
	        xo -= dXo;
	        yo -= dYo;
        }

        return new double[] {xo, yo, z};
    }
    
    
    public final static double computeGHA(double julianTime)
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
