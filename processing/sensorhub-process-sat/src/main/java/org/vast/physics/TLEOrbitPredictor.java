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


/**
 * <p><b>Title:</b><br/>
 * OrbitPredictor
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Helper class to compute satellite orbit position/velocity
 * </p>
 *
 * <p>Copyright (c) 2005</p>
 * @author Alexandre Robin
 * @since Feb 25, 2003
 */
public class TLEOrbitPredictor extends AbstractOrbitPredictor
{
	protected TLEProvider tleProvider;
	protected SGP4Propagator propagator;
	protected double orbitCycle = Double.NaN;
	
	
	public TLEOrbitPredictor(TLEProvider tleProvider)
	{
	    this.tleProvider = tleProvider;
	    this.propagator = new SGP4Propagator();
	}
	
	
	public TLEOrbitPredictor(TLEProvider tleProvider, double cycleInDays)
    {
        this(tleProvider);
        this.orbitCycle = cycleInDays;
    }
	
	
	/* (non-Javadoc)
	 * @see org.vast.physics.OrbitPredictor#getECIState(double)
	 */
	public MechanicalState getECIState(double time)
	{
		try
		{
			TLEInfo tle = tleProvider.getClosestTLE(time);			
			MechanicalState state = propagator.getECIOrbitalState(time, tle);			
			return state;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}


    public double getCycleInDays()
    {
        return this.orbitCycle;
    }
}
