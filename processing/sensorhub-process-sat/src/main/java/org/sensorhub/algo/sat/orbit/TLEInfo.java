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
 * TLEInfo
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Class for storing and retrieving two-line element data
 * </p>
 *
 * <p>Copyright (c) 2005</p>
 * @author Mike Botts, Alexandre Robin
 * @since 10/14/98
 */
public class TLEInfo
{
    protected int satID;
    protected String satName;   
    protected double tleTime; // julian time = seconds after 01/01/1970
    protected double meanAnomaly; // rad
    protected double rightAscension; // rad
    protected double eccentricity; // no unit
    protected double argOfPerigee; // rad
    protected double inclination; // rad
    protected double meanMotion; // rad/s
    protected double bstar; // 1 / earth radii
    protected int revNumber;


    public String getSatelliteName()
    {
        return satName;
    }


    public int getSatelliteID()
    {
        return satID;
    }


    public double getTleTime()
    {
        return tleTime;
    }


    public double getMeanAnomaly()
    {
        return meanAnomaly;
    }


    public double getRightAscension()
    {
        return rightAscension; // right ascension
    }


    public double getEccentricity()
    {
        return eccentricity;
    }


    public double getArgumentOfPerigee()
    {
        return argOfPerigee; // argument of the perigee
    }


    public double getInclination()
    {
        return inclination; // inclination
    }


    public double getMeanMotion()
    {
        return meanMotion; // revolutions per day
    }


    public double getBStar()
    {
        return bstar; // bstar
    }


    public int getRevNumber()
    {
        return revNumber;
    }


    public void setSatelliteName(String satName)
    {
        this.satName = satName;
    }


    public void setSatelliteID(int satID)
    {
        this.satID = satID;
    }


    public void setTleTime(double tleTime)
    {
        this.tleTime = tleTime;
    }


    public void setMeanAnomaly(double value)
    {
        this.meanAnomaly = value;
    }


    public void setRightAscension(double value)
    {
        this.rightAscension = value; // right ascension
    }


    public void setEccentricity(double value)
    {
        this.eccentricity = value;
    }


    public void setArgumentOfPerigee(double value)
    {
        this.argOfPerigee = value; // argument of the perigee
    }


    public void setInclination(double value)
    {
        this.inclination = value; // inclination
    }


    public void setMeanMotion(double value)
    {
        this.meanMotion = value; // revolutions per day
    }


    public void setBStar(double value)
    {
        this.bstar = value; // bstar
    }
    
    
    public void setRevNumber(int revNumber)
    {
        this.revNumber = revNumber;
    }
}
