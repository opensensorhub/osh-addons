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


public abstract class AbstractOrbitPredictor implements OrbitPredictor
{

	public AbstractOrbitPredictor()
	{
		super();
	}
	
	
	/* (non-Javadoc)
	 * @see org.vast.physics.OrbitPredictor#getECFState(double)
	 */
	public MechanicalState getECFState(double time)
	{
		MechanicalState state = getECIState(time);
		double eciX = state.linearPosition.x;
		double eciY = state.linearPosition.y;
		double eciZ = state.linearPosition.z;
		double eciVx = state.linearVelocity.x;
		double eciVy = state.linearVelocity.y;
		double eciVz = state.linearVelocity.z;
		
		// convert position
		double[] ecf = MapProjection.ECItoECF(time, eciX, eciY, eciZ, false);
		state.linearPosition.x = ecf[0];
		state.linearPosition.y = ecf[1];
		state.linearPosition.z = ecf[2];
				
		// convert velocity
		double[] ecfVel = MapProjection.ECItoECF(time, eciVx, eciVy, eciVz, true);
		state.linearVelocity.x = ecfVel[0];
		state.linearVelocity.y = ecfVel[1];
		state.linearVelocity.z = ecfVel[2];
		
		return state;
	}

	
	/* (non-Javadoc)
	 * @see org.vast.physics.OrbitPredictor#getECITrajectory(double, double, double)
	 */
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
	public MechanicalState[] getECFTrajectory(double startTime, double stopTime, double step)
	{
		int numPoints = (int) ((stopTime - startTime) / step) + 1;
		MechanicalState[] trajectory = new MechanicalState[numPoints];
		
		double time = startTime;
		for (int p = 0; p < numPoints; p++)
		{
			MechanicalState state = getECFState(time);
			trajectory[p] = state;				
			time += step;
		}
		
		return trajectory;
	}

}