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


/**
 * <p>
 * Interface for all satellite orbit predictors
 * </p>
 *
 * @author Alexandre Robin
 * @date 21 avr. 08
 */
public interface OrbitPredictor
{
    
    /**
     * @return The orbit cycle in days if any or NaN if none exists
     */
    public double getCycleInDays();
    
    
    /**
     * Gets the satellite ECI state at a given point in time
     * @param time in seconds past 01/01/1970 (unix time)
     * @param result State object to populate with new values (or null to create a new object)
     * @return New platform state in ECI frame
     */
    public MechanicalState getECIState(double time, MechanicalState result);


    /**
     * Gets the satellite ECEF state at a given point in time
     * @param time in seconds past 01/01/1970 (unix time)
     * @param result State object to populate with new values (or null to create a new object).
     * @return New platform state in ECEF frame
     */
    public MechanicalState getECEFState(double time, MechanicalState result);

}