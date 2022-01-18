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
import org.sensorhub.algo.vecmath.Vect3d;
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


public class WaypointTask extends UavTask
{
    GeoTransforms geo = new GeoTransforms();
    
    double destLat;
    double destLon;
    double destAlt;
    double speed;
    
    long lastReportTime;
    long startTime;
    double startLat;
    double startLon;
    double startAlt;
    double dist = Double.NaN;
    
    
    public static DataComponent getParams()
    {
        var swe = new GeoPosHelper();
        return swe.createRecord()
            .definition(CMD_URI_PREFIX + "WaypointCommand")
            .label("Go To Waypoint")
            .description("The waypoint command requests the vehicle to go to a specified waypoint with a desired travel velocity")
            .addField("position", swe.createLocationVectorLLA()
                .label("Waypoint location"))
            .addField("velocity", swe.createQuantity()
                .definition(SWEHelper.getQudtUri("Speed"))
                .label("Travel Velocity")
                .uomCode("m/s"))
            .build();
    }
    
    public WaypointTask(VehicleControl controlInput, ICommandData cmd)
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
        this.destLat = Math.toRadians(params.getDoubleValue(0));
        this.destLon = Math.toRadians(params.getDoubleValue(1));
        this.destAlt = params.getDoubleValue(2);
        this.speed = params.getDoubleValue(3);
        
        // check parameters
        if (speed <= 0.0)
            return CommandStatus.rejected(cmd.getID(), "Speed must be greather than 0");
        
        return CommandStatus.accepted(cmd.getID());
    }
    
    
    public boolean update()
    {
        long now = System.currentTimeMillis();
        var simState = sim.getSimulatorState().clone();
        
        // compute total distance the first time
        if (startTime == 0)
        {
            this.startTime = now;
            this.startLat = Math.toRadians(simState.lat);
            this.startLon = Math.toRadians(simState.lon);
            this.startAlt = simState.alt;
            
            this.dist = geo.computeGreatCircleDistanceHaversine(
                startLat, startLon, destLat, destLon);
        }
        
        // update lat/lon location
        long dt = now - startTime;
        double fHorz = dist != 0.0 ? Math.min(speed * dt/1000. / dist, 1.0) : 1.0;
        if (dist != 0.0)
        {
            var newLatLon = new Vect3d();
            geo.computeIntermediatePoint(
                startLat, startLon, destLat, destLon,
                dist, fHorz, newLatLon);
            
            simState.lat = Math.toDegrees(newLatLon.y);
            simState.lon = Math.toDegrees(newLatLon.x);
        }
        
        // update altitude
        double dAlt = destAlt - startAlt;
        double fVert = dAlt != 0.0 ? Math.min(speed * dt/1000. / Math.abs(dAlt), 1.0) : 1.0;
        if (dAlt != 0.0)
        {
            simState.alt = startAlt + dAlt * fVert;
        }
        
        // update simulation state
        sim.updateSimulatorState(simState);
        
        // send status report if needed
        var progress = (int)Math.floor(Math.min(fHorz, fVert)*100.);
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
        else if (now - lastReportTime > 5000)
        {
            status = new CommandStatus.Builder()
                .withCommand(cmd.getID())
                .withStatusCode(CommandStatusCode.EXECUTING)
                .withProgress(progress)
                .build();
        }
        
        // publish event if new status was generated
        if (status != null)
        {
            lastReportTime = now;
            controlInput.getEventHandler().publish(
                new CommandStatusEvent(controlInput, 100L, status));
        }
        
        return progress == 100;
    }
}
