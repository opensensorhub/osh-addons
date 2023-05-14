/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License Version
 1.1 (the "License"); you may not use this file except in compliance with
 the License. You may obtain a copy of the License at
 http://www.mozilla.org/MPL/MPL-1.1.html
 
 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.
 
 The Original Code is the "SensorML DataProcessing Engine".
 
 The Initial Developer of the Original Code is the VAST team at the
 University of Alabama in Huntsville (UAH). <http://vast.uah.edu>
 Portions created by the Initial Developer are Copyright (C) 2007
 the Initial Developer. All Rights Reserved.

 Please Contact Mike Botts <mike.botts@uah.edu> for more information.
 
 Contributor(s): 
    Alexandre Robin <robin@nsstc.uah.edu>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.algo.sat.orbit;

import java.io.IOException;


/**
 * <p>
 * Helper class to compute satellite orbit position/velocity using
 * two-line elements data and the SGP4 propagator.
 * </p>
 *
 * @author Alexandre Robin
 * @since Feb 25, 2003
 */
public class TLEOrbitPredictor extends AbstractOrbitPredictor
{
    protected final TLEProvider tleProvider;
    protected final SGP4Propagator propagator;
    protected final String satID;
    protected final double orbitCycle;
    
    
    public TLEOrbitPredictor(int satID, TLEProvider tleProvider)
    {
        this(satID, tleProvider, Double.NaN);
    }
    
    
    public TLEOrbitPredictor(int satID, TLEProvider tleProvider, double cycleInDays)
    {
        this.satID = String.format("%5d", satID);
        this.tleProvider = tleProvider;
        this.propagator = new SGP4Propagator();
        this.orbitCycle = cycleInDays;
    }
    
    
    @Override
    public MechanicalState getECIState(double time, MechanicalState result)
    {
        try
        {
            TLEInfo tle;
            synchronized (tleProvider) {
                tle = tleProvider.getClosestTLE(satID, time);
            }
            return propagator.getECIOrbitalState(time, tle, result);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }


    @Override
    public double getCycleInDays()
    {
        return this.orbitCycle;
    }
}
