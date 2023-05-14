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
    Alexandre Robin <alexandre.robin@spotimage.fr>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.algo.sat.orbit;

import org.sensorhub.algo.vecmath.Vect3d;


/**
 * <p>
 * Helper class to compute heliosynchronous satellite
 * position/velocity on nominal orbit 
 * </p>
 *
 * @author Alexandre Robin
 * @since Feb 25, 2008
 */
public class HelioSyncOrbitPredictor extends AbstractOrbitPredictor
{
    protected static final double TWO_PI = 2 * Math.PI;
    protected static final double OMEGA_SUM = TWO_PI / (365.24219 * 86400);
    
    protected double ascNodeTime;
    protected double ascNodeLong;
    protected int orbitCycle;
    protected int numOrbits;
    protected double nodalPeriod;
    protected double keplerPeriod;
    protected double orbitRadius;
    protected double orbitInclination;
    
    
    public HelioSyncOrbitPredictor(double ascNodeTime, double ascNodeLong, int orbitCycle, int numOrbits)
    {
        this.ascNodeTime = ascNodeTime;
        this.ascNodeLong = ascNodeLong;
        this.orbitCycle = orbitCycle;
        this.numOrbits = numOrbits;
        
        this.nodalPeriod = ((double)orbitCycle * 86400) / (double)numOrbits;
        this.orbitRadius = 7200000;
        this.orbitInclination = 98.7 * Math.PI/180;
    }
    
    
    @Override
    public MechanicalState getECIState(double time, MechanicalState state)
    {
        if (state == null)
            state = MechanicalState.withPosOrder1();
        
        Vect3d ecfPos = new Vect3d(orbitRadius, 0.0, 0.0);
        double dT = time - ascNodeTime;
        
        // pos on orbit plane
        ecfPos.rotateZ(TWO_PI * dT / nodalPeriod);
        
        // inclination
        ecfPos.rotateX(orbitInclination);
        
        // heliosynchronous rotation
        ecfPos.rotateZ(ascNodeLong + dT * OMEGA_SUM);
        
        state.epochTime = time;
        state.linearPosition.set(ecfPos);
        state.linearVelocity.setToZero();
        
        return state;
    }


    public double getCycleInDays()
    {
        return this.orbitCycle;
    }
}
