/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2022 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.simuav;


public class SimulatorState implements Cloneable
{
    public boolean armed;
    public boolean landed;
    public double lat, lon, alt;
    public double heading, pitch, roll;
    public double gimbalYaw, gimbalPitch, gimbalRoll;
    public double batt;
    public double temp;
    
    
    public SimulatorState clone()
    {
        try { return (SimulatorState)super.clone(); }
        catch (CloneNotSupportedException e) { return null; }
    }
}
