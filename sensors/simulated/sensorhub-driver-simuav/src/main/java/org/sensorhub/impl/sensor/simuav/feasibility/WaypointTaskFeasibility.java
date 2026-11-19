/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2022 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.simuav.feasibility;

import java.util.concurrent.CompletableFuture;
import org.sensorhub.algo.geoloc.GeoTransforms;
import org.sensorhub.api.command.CommandResult;
import org.sensorhub.api.command.CommandStatus;
import org.sensorhub.api.command.ICommandData;
import org.sensorhub.api.command.ICommandStatus;
import org.sensorhub.impl.sensor.simuav.SimUavDriver;
import org.sensorhub.impl.sensor.simuav.task.WaypointTask;
import org.vast.swe.SWEHelper;
import org.vast.util.TimeExtent;
import net.opengis.swe.v20.DataComponent;


public class WaypointTaskFeasibility extends UavFeasibility<SimUavDriver>
{
    DataComponent commandParamsStruct;
    DataComponent commandResultStruct;
    GeoTransforms geo = new GeoTransforms();
    
    
    public WaypointTaskFeasibility(SimUavDriver driver)
    {
        super("waypoint_feasibility", driver);
        
        this.commandParamsStruct = WaypointTask.getParams().copy();
        commandParamsStruct.setLabel("Waypoint Task Feasibility");
        commandParamsStruct.setDescription("To check the feasibility of the WAYPOINT command");
        
        var swe = new SWEHelper();
        this.commandResultStruct = swe.createRecord()
            .name("result")
            .addField("reachable", swe.createBoolean()
                .label("Reachability Flag")
                .description("True if the waypoint can be reached, false otherwise"))
            .addField("time_to_waypoint", swe.createQuantity()
                .label("Time to Waypoint")
                .description("Estimated travel time to reach waypoint")
                .uomCode("s"))
            .addField("battery_remaining", swe.createQuantity()
                .label("Battery Remaining")
                .description("Estimated battery remaining at waypoint")
                .uom("%"))
            .build();
    }
    
    
    @Override
    public DataComponent getCommandDescription()
    {
        return commandParamsStruct;
    }


    @Override
    public DataComponent getResultDescription()
    {
        return commandResultStruct;
    }
    

    @Override
    public CompletableFuture<ICommandStatus> submitCommand(ICommandData command)
    {
        var simState = getParentProducer().getSimulatorState();
        var execTime = TimeExtent.currentTime();
        
        var cmd = command.getParams();
        var destLat = Math.toRadians(cmd.getDoubleValue(0));
        var destLon = Math.toRadians(cmd.getDoubleValue(1));
        //var destAlt = cmd.getDoubleValue(2);
        var speed = cmd.getDoubleValue(3);
        
        var dist = geo.computeGreatCircleDistanceHaversine(
            simState.lat, simState.lon, destLat, destLon);
        
        // generate result
        var output = commandResultStruct.createDataBlock();
        double maxDist = 10e6;
        if (dist < maxDist)
        {
            output.setBooleanValue(0, true);
            output.setDoubleValue(1, Math.round(dist/speed));
            output.setDoubleValue(2, Math.round(dist/maxDist*100));
        }
        else
        {
            output.setBooleanValue(0, false);
            output.setDoubleValue(1, Double.NaN);
            output.setDoubleValue(2, Double.NaN);
        }
        
        var result = CommandResult.withData(output);
        var status = CommandStatus.completed(command.getID(), execTime, result);
        return CompletableFuture.completedFuture(status);
    }

}
