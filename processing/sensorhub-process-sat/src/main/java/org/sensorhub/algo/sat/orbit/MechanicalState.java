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

package org.sensorhub.algo.sat.orbit;

import org.sensorhub.algo.vecmath.Quat4d;
import org.sensorhub.algo.vecmath.Vect3d;


public class MechanicalState
{
    public double epochTime; // unix time in seconds (0 at 1970-01-01T00:00:00Z)

    public Vect3d linearPosition;
    public Vect3d linearVelocity;
    public Vect3d linearAcceleration;

    public Quat4d angularPosition;
    public Quat4d angularVelocity;
    public Quat4d angularAcceleration;


    public MechanicalState()
    {
    }
    
    
    /**
     * @return An instance with only the linear position vector
     * initialized
     */
    public static MechanicalState withPos()
    {
        var state = new MechanicalState();
        state.linearPosition = new Vect3d();
        return state;
    }
    
    
    /**
     * @return An instance with only the linear position and angular position
     * vectors initialized
     */
    public static MechanicalState withPosAtt()
    {
        var state = new MechanicalState();
        state.linearPosition = new Vect3d();
        state.angularPosition = new Quat4d();
        return state;
    }
    
    
    /**
     * @return An instance with only the linear position and linear velocity
     * vectors initialized
     */
    public static MechanicalState withPosOrder1()
    {
        var state = new MechanicalState();
        state.linearPosition = new Vect3d();
        state.linearVelocity = new Vect3d();
        return state;
    }
    
    
    /**
     * @return An instance with only the linear position, linear velocity,
     * angular position and angular velocity vectors initialized
     */
    public static MechanicalState withPosAttOrder1()
    {
        var state = new MechanicalState();
        state.linearPosition = new Vect3d();
        state.linearVelocity = new Vect3d();
        state.angularPosition = new Quat4d();
        state.angularVelocity = new Quat4d();
        return state;
    }
    
    
    /**
     * @return An instance with only the linear position, linear velocity
     * and linear acceleration vectors initialized
     */
    public static MechanicalState withPosOrder2()
    {
        var state = new MechanicalState();
        state.linearPosition = new Vect3d();
        state.linearVelocity = new Vect3d();
        state.linearAcceleration = new Vect3d();
        return state;
    }
    
    
    /**
     * @return An instance with only the linear position, linear velocity,
     * linear acceleration, angular position, angular velocity and angular
     * acceleration vectors initialized
     */
    public static MechanicalState withPosAttOrder2()
    {
        var state = new MechanicalState();
        state.linearPosition = new Vect3d();
        state.linearVelocity = new Vect3d();
        state.linearAcceleration = new Vect3d();
        state.angularPosition = new Quat4d();
        state.angularVelocity = new Quat4d();
        state.angularAcceleration = new Quat4d();
        return state;
    }
}
