/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.consys.mqtt;

import java.util.concurrent.TimeUnit;
import org.sensorhub.api.comm.mqtt.IMqttServer;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.ModuleEvent.ModuleState;
import org.sensorhub.api.service.IServiceModule;
import org.sensorhub.impl.module.AbstractModule;
import org.sensorhub.impl.service.consys.ConSysApiService;


/**
 * <p>
 * Add-on service to {@link ConSysApiService} to enable MQTT support
 * </p>
 *
 * @author Alex Robin
 * @since May 9, 2023
 */
public class ConSysApiMqttService extends AbstractModule<ConSysApiMqttServiceConfig> implements IServiceModule<ConSysApiMqttServiceConfig>
{
    protected ConSysApiMqttConnector mqttConnector;
    protected String endPoint;


    @Override
    protected void doStart() throws SensorHubException
    {
        super.doStart();
        this.startAsync = true;
        
        // try to attach to SWE API service
        getParentHub().getModuleRegistry().waitForModuleType(ConSysApiService.class, ModuleState.STARTED)
            .thenAccept(service -> {
                var servlet = service.getServlet();
                endPoint = service.getConfiguration().endPoint;
                
                // wait for MQTT server to be available
                getParentHub().getModuleRegistry().waitForModuleType(IMqttServer.class, ModuleState.STARTED)
                    .thenAccept(mqtt -> {
                        if (mqtt != null)
                        {
                            mqttConnector = new ConSysApiMqttConnector(servlet, endPoint);
                            mqtt.registerHandler(endPoint, mqttConnector);
                            getLogger().info("CONSYS API MQTT handler registered");
                            setState(ModuleState.STARTED);
                        }
                    })
                    .orTimeout(10, TimeUnit.SECONDS)
                    .exceptionally(e -> {
                        reportError("No MQTT server available", e);
                        return null;
                    });
            })
            .orTimeout(10, TimeUnit.SECONDS)
            .exceptionally(e -> {
                reportError("Cannot attach to CONSYS API service", e);
                return null;
            });
        
        
    }
    
    
    @Override
    protected void doStop() throws SensorHubException
    {
        super.doStop();
        
        // stop MQTT extension
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
