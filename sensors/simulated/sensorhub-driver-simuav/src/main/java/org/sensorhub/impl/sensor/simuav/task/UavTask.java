/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2022 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.simuav.task;

import org.sensorhub.api.command.ICommandData;
import org.sensorhub.api.command.ICommandStatus;
import org.sensorhub.impl.sensor.simuav.SimUavDriver;
import org.sensorhub.impl.sensor.simuav.UavControl;


public abstract class UavTask
{
    protected static final String CMD_URI_PREFIX = "urn:x-ogc:uxs:messages:";
    
    UavControl<SimUavDriver> controlInput;
    SimUavDriver sim;
    ICommandData cmd;


    protected UavTask(UavControl<SimUavDriver> controlInput, ICommandData cmd)
    {
        this.controlInput = controlInput;
        this.sim = controlInput.getParentProducer();
        this.cmd = cmd;
    }
    
    
    public abstract ICommandStatus init();
    
    
    /*
     * Let the task update the simulation state.
     * This will return true when task is complete
     */
    public abstract boolean update();


    public ICommandData getCommand()
    {
        return cmd;
    }
}
