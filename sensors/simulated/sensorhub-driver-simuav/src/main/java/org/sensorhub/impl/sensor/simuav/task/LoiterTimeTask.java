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

import java.time.Instant;
import org.sensorhub.api.command.CommandStatus;
import org.sensorhub.api.command.CommandStatusEvent;
import org.sensorhub.api.command.ICommandData;
import org.sensorhub.api.command.ICommandStatus;
import org.sensorhub.api.command.ICommandStatus.CommandStatusCode;
import org.sensorhub.impl.sensor.simuav.VehicleControl;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;
import org.vast.util.TimeExtent;
import net.opengis.swe.v20.DataComponent;


public class LoiterTimeTask extends UavTask
{
    long startTime;
    long duration;
    
    
    public static DataComponent getParams()
    {
        var swe = new GeoPosHelper();
        return swe.createRecord()
            .definition(CMD_URI_PREFIX + "LoiterCommand")
            .label("Loiter")
            .description("The loiter command tasks the vehicle to loiter in place for a specific time.")
            .addField("time", swe.createQuantity()
                .definition(SWEHelper.getPropertyUri("Duration"))
                .label("Loiter Duration")
                .uomCode("s"))
            .build();
    }
    
    public LoiterTimeTask(VehicleControl controlInput, ICommandData cmd)
    {
        super(controlInput, cmd);
    }
    
    
    @Override
    public ICommandStatus init()
    {
        // first check current state and reject early if needed
        var currentState = sim.getSimulatorState();
        if (currentState.landed && !sim.hasPendingTask(AutoTakeOffTask.class))
            return CommandStatus.rejected(cmd.getID(), "Vehicle hasn't received a takeoff command");
        
        // read command data
        var params = cmd.getParams();
        this.duration = (long)(params.getDoubleValue(0)*1000);
        
        // check parameters
        if (duration <= 0.0)
            return CommandStatus.rejected(cmd.getID(), "Loiter duration must be greather than 0");
        
        return CommandStatus.accepted(cmd.getID());
    }
    
    
    public boolean update()
    {
        long now = System.currentTimeMillis();
        var simState = sim.getSimulatorState().clone();
        
        // compute total distance the first time
        if (startTime == 0)
            this.startTime = now;
        
        // update simulation state
        sim.updateSimulatorState(simState);
        
        boolean done = now - startTime >= duration;
        ICommandStatus status = null;
        if (done)
        {
            status = new CommandStatus.Builder()
                .withCommand(cmd.getID())
                .withStatusCode(CommandStatusCode.COMPLETED)
                .withExecutionTime(TimeExtent.period(
                    Instant.ofEpochMilli(startTime),
                    Instant.ofEpochMilli(now)))
                .build();
        }
        
        // publish event if new status was generated
        if (status != null)
        {
            controlInput.getEventHandler().publish(
                new CommandStatusEvent(controlInput, 100L, status));
        }
        
        return done;
    }
}
