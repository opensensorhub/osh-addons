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

package org.vast.physics;

import org.sensorhub.algo.geoloc.GeoTransforms;


public abstract class AbstractOrbitPredictor implements OrbitPredictor
{
    GeoTransforms geoConv;
    
    
	public AbstractOrbitPredictor()
	{
		this.geoConv = new GeoTransforms();
	}
	
	
	public abstract MechanicalState getECIState(double time);
    	
	
	/* (non-Javadoc)
	 * @see org.vast.physics.OrbitPredictor#getECFState(double)
	 */
	@Override
    public MechanicalState getECEFState(double time)
	{
		MechanicalState state = getECIState(time);
				
		// convert position & velocity to ECEF
		geoConv.ECItoECEF(time, state.linearPosition, state.linearPosition, false);
		geoConv.ECItoECEF(time, state.linearVelocity, state.linearVelocity, true);
		
		return state;
	}

	
	/* (non-Javadoc)
	 * @see org.vast.physics.OrbitPredictor#getECITrajectory(double, double, double)
	 */
	@Override
    public MechanicalState[] getECITrajectory(double startTime, double stopTime, double step)
	{
		int numPoints = (int) ((stopTime - startTime) / step);
		MechanicalState[] trajectory = new MechanicalState[numPoints];
		
		double time = startTime;
		for (int p = 0; p < numPoints; p++)
		{
			MechanicalState state = getECIState(time);
			trajectory[p] = state;
			time += step;
		}
		
		return trajectory;
	}

	
	/* (non-Javadoc)
	 * @see org.vast.physics.OrbitPredictor#getECFTrajectory(double, double, double)
	 */
	@Override
    public MechanicalState[] getECEFTrajectory(double startTime, double stopTime, double step)
	{
		int numPoints = (int) ((stopTime - startTime) / step) + 1;
		MechanicalState[] trajectory = new MechanicalState[numPoints];
		
		double time = startTime;
		for (int p = 0; p < numPoints; p++)
		{
			MechanicalState state = getECEFState(time);
			trajectory[p] = state;				
			time += step;
		}
		
		return trajectory;
	}

}