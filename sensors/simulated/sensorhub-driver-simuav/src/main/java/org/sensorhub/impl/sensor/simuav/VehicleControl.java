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

import java.util.concurrent.CompletableFuture;
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.command.CommandStatus;
import org.sensorhub.api.command.CommandData;
import org.sensorhub.api.command.CommandException;
import org.sensorhub.api.command.ICommandStatus;
import org.sensorhub.api.command.ICommandStatus.CommandStatusCode;
import org.sensorhub.api.command.ICommandData;
import org.sensorhub.impl.sensor.simuav.task.AutoTakeOffTask;
import org.sensorhub.impl.sensor.simuav.task.UavTask;
import org.sensorhub.impl.sensor.simuav.task.WaypointTask;
import org.vast.data.DataBlockMixed;
import org.vast.swe.helper.GeoPosHelper;
import static org.sensorhub.impl.sensor.simuav.VehicleControl.CommandType.*;


public class VehicleControl extends UavControl<SimUavDriver>
{
    DataChoice commandData;
    
    
    enum CommandType
    {
        /*AUTO_LAND,*/ AUTO_TAKEOFF, WAYPOINT, LOITER 
    }
    
    
    protected VehicleControl(SimUavDriver driver)
    {
        super("vehicle_control", driver);
        
        // choice of motion commands
        var swe = new GeoPosHelper();
        this.commandData = swe.createChoice()
            .label("Vehicle Commands")
            .build();
        
        // auto takeoff
        commandData.addItem(AUTO_TAKEOFF.toString(), AutoTakeOffTask.getParams());
        
        // goto waypoint
        commandData.addItem(WAYPOINT.toString(), WaypointTask.getParams());
        
        /*// loiter
        commandData.addItem(LOITER.toString(), swe.createRecord()
            .definition(SimUavDriver.CMD_URI_PREFIX + "LoiterCommand")
            .label("Loiter")
            .description("The loiter command allows the operator to define a loiter pattern for a vehicle.")
            .addField("bearing", swe.createQuantity()
                .definition(SWEHelper.getPropertyUri("TrueBearing"))
                .label("Loiter Bearing")
                .uomCode("deg"))
            .addField("length", swe.createQuantity()
                .definition(SWEHelper.getQudtUri("Length"))
                .label("Loiter Length")
                .uomCode("m"))
            .addField("radius", swe.createQuantity()
                .definition(SWEHelper.getQudtUri("Radius"))
                .label("Loiter Radius")
                .uomCode("m"))
            .addField("speed", swe.createQuantity()
                .definition(SWEHelper.getQudtUri("Speed"))
                .label("Loiter Speed")
                .uomCode("m/s"))
            .addField("approachSpeed", swe.createQuantity()
                .definition(SWEHelper.getQudtUri("Speed"))
                .label("Approach Speed")
                .uomCode("m/s"))
            .addField("entryPoint", swe.createLocationVectorLLA()
                .label("Entry Point Location"))
            .addField("loiterPoint", swe.createLocationVectorLLA()
                .label("Loiter Point Location"))
            .addField("loiterType", swe.createCategory()
                .definition(SimUavDriver.CMD_URI_PREFIX + "LoiterType")
                .label("Loiter Type")
                .addAllowedValues("CIRCULAR", "FIGURE8", "HOVER", "RACETRACK"))
            .addField("loiterDirection", swe.createCategory()
                .definition(SimUavDriver.CMD_URI_PREFIX + "LoiterDirection")
                .label("Loiter Direction")
                .addAllowedValues("CLOCKWISE", "COUNTERCLOCKWISE"))
            .build());*/
    }


    @Override
    public DataComponent getCommandDescription()
    {
        return commandData;
    }


    @Override
    public CompletableFuture<ICommandStatus> submitCommand(ICommandData command)
    {
        try
        {
            var cmdIdx = command.getParams().getIntValue();
            var cmdCode = CommandType.values()[cmdIdx];
            
            getLogger().info("Received {} command", cmdCode);
            getLogger().debug("{}", command);
            
            // extract only command data w/o choice index
            command = CommandData.Builder.from(command)
                .withParams(((DataBlockMixed)command.getParams()).getUnderlyingObject()[1])
                .build();
            
            // send command to simulator
            switch (cmdCode)
            {
                case AUTO_TAKEOFF:
                    return submitTask(new AutoTakeOffTask(this, command));
                    
                case WAYPOINT:
                    return submitTask(new WaypointTask(this, command));
                    
                case LOITER:
                
                default:
                    var status = CommandStatus.rejected(command.getID(), "Unsupported command type");
                    return CompletableFuture.completedFuture(status);
            }
        }
        catch (IllegalArgumentException e)
        {
            var status = CommandStatus.rejected(command.getID(), "Invalid command type");
            return CompletableFuture.completedFuture(status);
        }
    }
    
    
    CompletableFuture<ICommandStatus> submitTask(UavTask task)
    {
        ICommandStatus initStatus;
        
        if (parentSensor.taskQueue.size() > 100)
            initStatus = CommandStatus.rejected(task.getCommand().getID(), "Too many commands received");
        
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
