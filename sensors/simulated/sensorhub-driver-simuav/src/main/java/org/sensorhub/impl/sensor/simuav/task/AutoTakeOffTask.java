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
import org.sensorhub.algo.geoloc.GeoTransforms;
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


public class AutoTakeOffTask extends UavTask
{
    GeoTransforms geo = new GeoTransforms();
    
    double takeOffAlt;
    double verticalSpeed = 1.0; // in m/s
    
    long lastReportTime;
    long startTime;
    double startAlt;
    
    
    public static DataComponent getParams()
    {
        var swe = new GeoPosHelper();
        return swe.createRecord()
            .definition(CMD_URI_PREFIX + "AutoTakeOffCommand")
            .label("Auto TakeOff")
            .description("This command indicates that the operator has requested the UAV to perform auto" +
                         " take-off as defined by the vehicle manufacturer.")
            .addField("height", swe.createQuantity()
                .definition(SWEHelper.getPropertyUri("HeightAboveGround"))
                .label("TakeOff Height")
                .uom("m"))
            .build();
    }
    
    public AutoTakeOffTask(VehicleControl controlInput, ICommandData cmd)
    {
        super(controlInput, cmd);
    }
    
    
    @Override
    public ICommandStatus init()
    {
        // first check current state and reject early if needed
        var currentState = sim.getSimulatorState();
        if (!currentState.landed || sim.hasPendingTask(AutoTakeOffTask.class))
            return CommandStatus.rejected(cmd.getID(), "Vehicle has already received a takeoff command");
        
        // read command data
        var params = cmd.getParams();
        this.takeOffAlt = params.getDoubleValue(0);
        
        return CommandStatus.accepted(cmd.getID());
    }
    
    
    public boolean update()
    {
        long now = System.currentTimeMillis();
        var simState = sim.getSimulatorState().clone();
        
        // init on first call
        if (startTime == 0)
        {
            this.startTime = now;
            this.startAlt = simState.alt;
            simState.landed = false;
        }
        
        // update simulation state
        long dt = now - startTime;
        double f = Math.min(verticalSpeed * dt/1000. / takeOffAlt, 1.0);
        simState.alt = startAlt + takeOffAlt * f;
        simState.vz = f < 1.0 ? verticalSpeed : 0.0;
        
        // update simulation state
        sim.updateSimulatorState(simState);
        
        // send status report if needed
        var progress = (int)(f*100);
        ICommandStatus status = null;
        if (progress == 100)
        {
            status = new CommandStatus.Builder()
                .withCommand(cmd.getID())
                .withStatusCode(CommandStatusCode.COMPLETED)
                .withExecutionTime(TimeExtent.period(
                    Instant.ofEpochMilli(startTime),
                    Instant.ofEpochMilli(now)))
                .build();
        }
        else if (progress == 0)
        {
            status = new CommandStatus.Builder()
                .withCommand(cmd.getID())
                .withStatusCode(CommandStatusCode.EXECUTING)
                .build();
        }
        
        // publish event if new status was generated
        if (status != null)
        {
            lastReportTime = now;
            controlInput.getEventHandler().publish(
                new CommandStatusEvent(controlInput, 100L, status));
        }
        
        return f == 1.0;
    }
}
