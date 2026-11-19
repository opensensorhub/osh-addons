/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 - 2024 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.spot.control;

import org.sensorhub.api.command.ICommandData;
import org.sensorhub.api.command.ICommandStatus;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.event.IEventHandler;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.sensorhub.impl.sensor.spot.SpotSensor;

import java.util.concurrent.CompletableFuture;

/**
 * Base class for control and tasking streams.
 *
 * @author Nick Garay
 * @since Sept. 25, 2023
 */
public abstract class BaseSpotControl extends AbstractSensorControl<SpotSensor> {

    /**
     * Handle to the current command being executed
     */
    protected BigId currentCommandId;

    /**
     * Constructor
     *
     * @param name The parent sensor control
     * @param parentSensor The parent sensor module
     */
    protected BaseSpotControl(String name, SpotSensor parentSensor) {

        super(name, parentSensor);
    }

    /**
     * Retrieves the event handler
     *
     * @return The event handler
     */
    public final IEventHandler getEventHandler() {

        return eventHandler;
    }

    /**
     * Retrieves the command ID
     *
     * @return The command ID for the current command being executed
     */
    public final BigId getCommandId() {

        return currentCommandId;
    }

    @Override
    public CompletableFuture<ICommandStatus> submitCommand(ICommandData command) {

        currentCommandId = command.getID();

        return super.submitCommand(command);
    }

    /**
     * Initializes the control, setting up the ROS nodes, the executor that will manage
     * their lifecycle
     */
    public abstract void doInit();

    /**
     * Starts the service clients via the ROS executor
     */
    public abstract void doStart();

    /**
     * Stops the service clients and the ROS executor
     */
    public abstract void doStop();
}
