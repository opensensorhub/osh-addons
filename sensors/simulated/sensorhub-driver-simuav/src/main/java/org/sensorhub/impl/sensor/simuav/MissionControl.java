/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.simuav;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.opengis.swe.v20.Count;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.command.CommandStatus;
import org.sensorhub.api.command.CommandData;
import org.sensorhub.api.command.CommandException;
import org.sensorhub.api.command.ICommandStatus;
import org.sensorhub.api.command.ICommandStatus.CommandStatusCode;
import org.sensorhub.api.command.ICommandData;
import org.sensorhub.impl.sensor.simuav.task.UavTask;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;
import org.vast.util.Asserts;


/**
 * <p>
 * Control input used to submit entire mission plans
 * </p>
 *
 * @author Alex Robin
 * @since May 18, 2023
 */
public class MissionControl extends UavControl<SimUavDriver>
{
    DataRecord missionData;
    VehicleControl vehicleControl;
    
    
    protected MissionControl(SimUavDriver driver, VehicleControl vehicleControl)
    {
        super("mission_planning", driver);
        this.vehicleControl = Asserts.checkNotNull(vehicleControl);
        
        // choice of motion commands
        var swe = new GeoPosHelper();
        this.missionData = swe.createRecord()
            .label("Mission Definition")
            .build();
        
        // mission ID
        missionData.addField("id", swe.createText()
            .definition(SWEHelper.getPropertyUri("Identifier"))
            .build());
        
        // num waypoints
        Count numWptCount;
        missionData.addField("numItems", numWptCount = swe.createCount()
            .id("NUM_ITEMS")
            .definition(SWEHelper.getPropertyUri("ArraySize"))
            .build());
        
        // goto waypoint
        missionData.addField("missionItems", swe.createArray()
            .withSizeComponent(numWptCount)
            .withElement("item", vehicleControl.getCommandDescription())
            .build());
    }


    @Override
    public DataComponent getCommandDescription()
    {
        return missionData;
    }


    @Override
    public CompletableFuture<ICommandStatus> submitCommand(ICommandData command)
    {
        try
        {
            var cmdMsg = getCommandDescription().copy();
            cmdMsg.setData(command.getParams());
            
            var missionId = cmdMsg.getComponent("id").getData().getStringValue(0);
            var missionItems = cmdMsg.getComponent("missionItems");
            var numItems = missionItems.getComponentCount();
            
            getLogger().info("Received mission {} with {} items", missionId, numItems);
            getLogger().debug("{}", command);
            
            // first validate commands
            List<ICommandData> commands = new ArrayList<>();
            for (int i = 0; i < numItems; i++)
            {
                var item = missionItems.getComponent(i);
                var cmd = new CommandData(i+1, item.getData());
                vehicleControl.validateCommand(cmd);
                commands.add(cmd);
            }
            
            // send each command to vehicle control
            for (var cmd: commands)
            {
                var status = vehicleControl.submitCommand(cmd);
                if (status.isDone() && status.get().getStatusCode() == CommandStatusCode.REJECTED)
                    return status;
            }
            
            var status = CommandStatus.accepted(command.getID());
            return CompletableFuture.completedFuture(status);
        }
        catch (Exception e)
        {
            var status = CommandStatus.rejected(command.getID(), "Invalid mission plan");
            return CompletableFuture.completedFuture(status);
        }
    }
    
    
    CompletableFuture<ICommandStatus> submitTask(UavTask task)
    {
        ICommandStatus initStatus;
        
        if (parentSensor.taskQueue.size() > 100)
            initStatus = CommandStatus.rejected(task.getCommand().getID(), "Too many commands received");
        else
            initStatus = task.init();
        
        if (initStatus.getStatusCode() != CommandStatusCode.REJECTED)
            parentSensor.queueTask(task);
        
        return CompletableFuture.completedFuture(initStatus);
    }


    @Override
    public void validateCommand(ICommandData command) throws CommandException
    {
        
    }

}
