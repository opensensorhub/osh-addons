/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.mqtt;

import org.sensorhub.api.comm.mqtt.IMqttServer;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.event.Event;
import org.sensorhub.api.event.IEventListener;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.api.module.ModuleEvent.ModuleState;
import org.sensorhub.impl.module.AbstractModule;


/**
 * <p>
 * Helper class to implement services that are attached to the embedded MQTT
 * server. This class automatically handles deployments and re-deployments when
 * the embedded MQTT server stops and starts.
 * </p>
 * 
 * @param <ConfigType> Type of config
 *
 * @author Alex Robin
 * @since Sep 22, 2025
 */
public abstract class AbstractMqttServiceModule<ConfigType extends ModuleConfig> extends AbstractModule<ConfigType> implements IEventListener
{
    protected IMqttServer mqttServer;
    
    
    @Override
    public void start() throws SensorHubException
    {
        mqttServer = getParentHub().getModuleRegistry().getModuleByType(IMqttServer.class);
        if (mqttServer == null)
            throw new SensorHubException("MQTT server module is not loaded");

        // subscribe to server lifecycle events
        ((IModule<?>)mqttServer).registerListener(this);

        reportStatus("Waiting for MQTT server...");
        // we actually start in the handleEvent() method when
        // a STARTED event is received from HTTP server
    }
    
    
    @Override
    public void stop() throws SensorHubException
    {
        // stop listening to http server events
        if (mqttServer != null)
            ((IModule<?>)mqttServer).unregisterListener(this);
        
        super.stop();        
        mqttServer = null;
    }
    
    
    @Override
    public void handleEvent(Event e)
    {
        // catch MQTT server lifecycle events
        if (e instanceof ModuleEvent && e.getSource() == mqttServer)
        {
            ModuleState newState = ((ModuleEvent) e).getNewState();

            // start when HTTP server is started
            if (newState == ModuleState.STARTED)
            {
                try
                {
                    doStart();
                    setState(ModuleState.STARTED);
                    clearStatus();
                }
                catch (Exception ex)
                {
                    reportError("Service could not start", ex);
                }
            }

            // stop when HTTP server is stopped
            else if (newState == ModuleState.STOPPED)
            {
                try
                {
                    doStop();
                    setState(ModuleState.STOPPED);
                }
                catch (SensorHubException ex)
                {
                    reportError("Service could not stop", ex);
                }
            }

            // fully stop when HTTP server module is removed
            else if (((ModuleEvent) e).getType() == ModuleEvent.Type.UNLOADED)
            {
                try
                {
                    stop();
                }
                catch (SensorHubException ex)
                {
                    reportError("Service could not stop", ex);
                }
            }
        }
    }


    public IMqttServer getMqttServer()
    {
        return mqttServer;
    }    
}
