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

package org.vast.physics;

import org.sensorhub.algo.vecmath.Vect3d;
import org.vast.util.DateTimeFormat;


/**
 * <p><b>Title:</b><br/>
 * HelioSyncOrbitPredictor
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Helper class to compute heliosynchronous satellite
 * position/velocity on nominal orbit 
 * </p>
 *
 * <p>Copyright (c) 2005</p>
 * @author Alexandre Robin
 * @since Feb 25, 2008
 */
public class HelioSyncOrbitPredictor extends AbstractOrbitPredictor
{
	protected final static double TWO_PI = 2 * Math.PI;
	protected final static double omegaSun = TWO_PI / (365.24219 * 86400);
	
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
	
	
	/* (non-Javadoc)
	 * @see org.vast.physics.OrbitPredictor#getECIState(double)
	 */
	public MechanicalState getECIState(double time)
	{
		Vect3d ecfPos = new Vect3d(orbitRadius, 0.0, 0.0);
		double dT = time - ascNodeTime;
		
		// pos on orbit plane
		ecfPos.rotateZ(TWO_PI * dT / nodalPeriod);
		
		// inclination
		ecfPos.rotateX(orbitInclination);
		
		// heliosynchronous rotation
		ecfPos.rotateZ(ascNodeLong + dT * omegaSun);
		
		MechanicalState state = new MechanicalState();
		state.julianTime = time;
		state.linearPosition = ecfPos;
		state.linearVelocity = new Vect3d();
		
		return state;
	}
	
	
	public static void main(String[] args)
	{
		try
		{
			// SPOT
			double ascNodeDate = 00; // s
			double ascNodeTime = ascNodeDate;// + 44967.551; // s
			double ascNodeLong = 0;//330.24 * Math.PI/180; // rad
			int orbitCycle = 26; // d
			int numOrbits = 369;
			
			OrbitPredictor predictor = new HelioSyncOrbitPredictor(ascNodeTime, ascNodeLong, orbitCycle, numOrbits);
			
			DateTimeFormat formatter = new DateTimeFormat();
			double startTime = formatter.parseIso("1970-01-01T00:00:00Z");
			double stopTime = formatter.parseIso("1971-01-01T04:00:00Z");
			MechanicalState[] trajectory = predictor.getECITrajectory(startTime, stopTime, 6087.804878);
						
			for (int p = 0; p < trajectory.length; p++)
			{
				System.out.println("Time: " + formatter.formatIso(trajectory[p].julianTime, 0));
				
				System.out.println("ECF Position (m): " + 
						           trajectory[p].linearPosition.x + "," +
						           trajectory[p].linearPosition.y + "," +
						           trajectory[p].linearPosition.z);
				
//				System.out.println("ECF Velocity (m/s): " + 
//				           trajectory[p].linearVelocity.x + "," +
//				           trajectory[p].linearVelocity.y + "," +
//				           trajectory[p].linearVelocity.z);
				
				System.out.println();
				
				// We could also compute nadir pointing attitude
				// see org.vast.physics.NadirPointing
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}


	public double getCycleInDays()
	{
		return this.orbitCycle;
	}
}
