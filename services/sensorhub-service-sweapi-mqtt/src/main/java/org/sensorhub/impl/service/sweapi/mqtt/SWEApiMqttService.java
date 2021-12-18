/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sweapi.mqtt;

import org.sensorhub.api.comm.mqtt.IMqttServer;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.ModuleEvent.ModuleState;
import org.sensorhub.api.service.IServiceModule;
import org.sensorhub.impl.module.AbstractModule;
import org.sensorhub.impl.service.sweapi.SWEApiService;
import org.sensorhub.impl.service.sweapi.SWEApiServlet;


/**
 * <p>
 * Extension of {@link SWEApiService} to enable the MQTT extension
 * </p>
 *
 * @author Alex Robin
 * @since Jul 28, 2021
 */
public class SWEApiMqttService extends AbstractModule<SWEApiMqttServiceConfig> implements IServiceModule<SWEApiMqttServiceConfig>
{
    protected SWEApiServlet servlet;
    protected SWEApiMqttConnector mqttConnector;
    protected String endPoint;


    @Override
    protected void doStart() throws SensorHubException
    {
        super.doStart();
        this.startAsync = true;
        
        // try to attach to SWE API service
        getParentHub().getModuleRegistry().waitForModuleType(SWEApiService.class, ModuleState.STARTED)
            .thenAccept(service -> {
                servlet = service.getServlet();
                endPoint = service.getConfiguration().endPoint;
                
                // wait for MQTT server to be available
                getParentHub().getModuleRegistry().waitForModuleType(IMqttServer.class, ModuleState.STARTED)
                .thenAccept(mqtt -> {
                    if (mqtt != null)
                    {
                        mqttConnector = new SWEApiMqttConnector(servlet, endPoint);
                        mqtt.registerHandler(endPoint, mqttConnector);
                        getLogger().info("SensorWeb API MQTT handler registered");
                        setState(ModuleState.STARTED);
                    }
                })
                .exceptionally(e -> {
                    reportError("No MQTT server available", e);
                    return null;
                });
            })
            .exceptionally(e -> {
                reportError("Cannot attach to SWE API service", e);
                return null;
            });
    }
    
    
    @Override
    protected void doStop() throws SensorHubException
    {
        super.doStop();
        
        // also stop MQTT extension if it was enabled
        servlet = null;
        if (mqttConnector != null)
        {
            getParentHub().getModuleRegistry().waitForModuleType(IMqttServer.class, ModuleState.STARTED)
                .thenAccept(mqtt -> {
                    if (mqtt != null)
                    {
                        mqtt.unregisterHandler(endPoint, mqttConnector);
                        getLogger().info("SensorWeb API MQTT handler unregistered");
                        mqttConnector.stop();
                        mqttConnector = null;
                    }
                });
        }
    }
}
