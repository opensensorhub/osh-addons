/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.simuav;

import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.simuav.feasibility.WaypointTaskFeasibility;
import org.sensorhub.impl.sensor.simuav.task.UavTask;
import org.sensorhub.utils.NamedThreadFactory;
import org.vast.sensorML.SMLHelper;
import net.opengis.sensorml.v20.PhysicalSystem;


/**
 * <p>
 * Simulated UAV with tasking capability.
 * </p>
 *
 * @author Alex Robin
 * @since Jan 14, 2022
 */
public class SimUavDriver extends AbstractSensorModule<SimUavConfig>
{
    PositionOutput posOutput;
    AttitudeOutput attOutput;
    StateOutput stateOutput;
    GimbalOutput gimbalOutput;
    StatusOutput statusOutput;
    
    volatile SimulatorState currentState;
    ScheduledExecutorService scheduler;
    Queue<UavTask> taskQueue = new LinkedBlockingQueue<>();
    ScheduledFuture<?> taskHandlerFuture;
    
    
    @Override
    protected void doInit() throws SensorHubException
    {
        super.doInit();
        
        // generate identifiers
        generateUniqueID("urn:osh:system:simuav:", config.serialNumber);
        generateXmlID("SIMULATED_UAV_", config.serialNumber);
        
        // init outputs
        posOutput = new PositionOutput(this);
        addOutput(posOutput, false);
        stateOutput = new StateOutput(this);
        addOutput(stateOutput, false);
        
        // init control inputs
        var control1 = new VehicleControl(this);
        addControlInput(control1);
        var mission1 = new MissionControl(this, control1);
        addControlInput(mission1);
        addControlInput(new WaypointTaskFeasibility(this));
        taskQueue.clear();
        
        // set initial state
        currentState = new SimulatorState();
        currentState.landed = true;
        currentState.armed = false;
        currentState.lat = config.initLocation.lat;
        currentState.lon = config.initLocation.lon;
        currentState.alt = config.initLocation.alt;
    }
    
    
    public void queueTask(UavTask task)
    {
        taskQueue.add(task);
    }


    public SimulatorState getSimulatorState()
    {
        return currentState;
    }
    
    
    public void updateSimulatorState(SimulatorState newState)
    {
        // update state atomically
        this.currentState = newState;
    }
    
    
    public Queue<UavTask> getTaskQueue()
    {
        return taskQueue;
    }
    
    
    public boolean hasPendingTask(Class<? extends UavTask> type)
    {
        return taskQueue.stream().anyMatch(t -> t.getClass().equals(type));
    }


    @Override
    protected void updateSensorDescription()
    {
        synchronized (sensorDescLock)
        {
            super.updateSensorDescription();
            
            if (!sensorDescription.isSetDescription())
                sensorDescription.setDescription("Simulated UAV with tasking support");
            
            var sml = new SMLHelper();
            sml.edit((PhysicalSystem)sensorDescription)
                .addIdentifier(sml.identifiers.serialNumber(config.serialNumber))
                .addCharacteristicList("operating_specs", sml.characteristics.operatingCharacteristics()
                    .add("voltage", sml.characteristics.operatingVoltageRange(110., 250., "V"))
                    .add("temperature", sml.conditions.temperatureRange(-20., 90., "Cel"))
                    .build());
        }
    }


    @Override
    protected void doStart() throws SensorHubException
    {
        scheduler = Executors.newScheduledThreadPool(1, new NamedThreadFactory("UavSim"));
        
        // start all outputs
        for (var output: getOutputs().values())
            ((UavOutput<?>)output).start();
        
        // start command process thread
        taskHandlerFuture = scheduler.scheduleAtFixedRate(new Runnable() {
            UavTask prevTask;
            public void run()
            {
                // update current task
                var task = taskQueue.peek();
                if (task != null && task != prevTask)
                    getLogger().info("Starting {} task (command={})", task.getClass().getSimpleName(), task.getCommand().getID());
                var done = task != null ? task.update() : false;
                prevTask = task;
                
                // if this task is done, go to next task
                if (done)
                    taskQueue.poll();
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
    }
    

    @Override
    protected void doStop() throws SensorHubException
    {
        if (taskHandlerFuture != null)
        {
            taskHandlerFuture.cancel(true);
            taskHandlerFuture = null;
        }
        
        for (var output: getOutputs().values())
            ((UavOutput<?>)output).stop();
        
        if (scheduler != null)
        {
            scheduler.shutdown();
            scheduler = null;
        }
    }
    

    @Override
    public void cleanup() throws SensorHubException
    {
       
    }
    
    
    @Override
    public boolean isConnected()
    {
        return true;
    }
}

