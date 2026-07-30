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
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.ModuleEvent.ModuleState;
import org.sensorhub.api.service.IServiceModule;
import org.sensorhub.impl.service.consys.ConSysApiService;
import org.sensorhub.impl.service.mqtt.AbstractMqttServiceModule;


/**
 * <p>
 * Add-on service to {@link ConSysApiService} to enable MQTT support
 * </p>
 *
 * @author Alex Robin
 * @since May 9, 2023
 */
public class ConSysApiMqttService extends AbstractMqttServiceModule<ConSysApiMqttServiceConfig> implements IServiceModule<ConSysApiMqttServiceConfig>
{
    protected ConSysApiMqttConnector mqttConnector;
    protected ResourceEventPublisher resourceEventPublisher;
    protected String endPoint;


    @Override
    protected void doStart() throws SensorHubException
    {
        super.doStart();
        
        // try to attach to CONSYS API REST service
        getParentHub().getModuleRegistry().waitForModuleType(ConSysApiService.class, ModuleState.STARTED)
            .thenAccept(service -> {
                var servlet = service.getServlet();
                endPoint = service.getConfiguration().endPoint;
                
                if (mqttServer != null)
                {
                    var hasNodeId = config.nodeId != null && !config.nodeId.isBlank();
                    mqttConnector = new ConSysApiMqttConnector(servlet, endPoint, config.nodeId);
                    if (hasNodeId)
                    {
                        // With a nodeId, all topics use "{nodeId}/{resourcePath}" per OGC CS API Part 3 spec —
                        // no endpoint prefix.  One handler registration covers both data topics
                        // ("{nodeId}/systems/{id}/...:data") and resource event topics ("{nodeId}/systems/{id}").
                        mqttServer.registerHandler(config.nodeId + "/", mqttConnector);
                    }
                    else
                    {
                        // No nodeId: fall back to endpoint-prefixed topics (e.g. "/api/systems/...")
                        mqttServer.registerHandler(endPoint, mqttConnector);
                    }

                    // Start proactive CloudEvents publisher if a nodeId is configured
                    if (hasNodeId)
                    {
                        var eventBus   = getParentHub().getEventBus();
                        var idEncoders = getParentHub().getIdEncoders();
                        var db         = getParentHub().getDatabaseRegistry().getFederatedDatabase();
                        var csApiBaseUrl = service.getPublicEndpointUrl();

                        resourceEventPublisher = new ResourceEventPublisher(
                            mqttServer, config.nodeId, csApiBaseUrl,
                            eventBus, db, idEncoders, getLogger());
                        resourceEventPublisher.start();
                        getLogger().info("CloudEvents resource event publisher started on nodeId '{}'", config.nodeId);
                    }
                    else
                    {
                        getLogger().warn("nodeId not configured — CloudEvents Resource Event Topics will not be published");
                    }

                    getLogger().info("CONSYS API MQTT handler registered");
                    setState(ModuleState.STARTED);
                }
            })
            .orTimeout(30, TimeUnit.SECONDS)
            .exceptionally(e -> {
                reportError("Could not attach to CONSYS API service", e);
                return null;
            });
    }
    
    
    @Override
    protected void doStop() throws SensorHubException
    {
        super.doStop();

        // Stop CloudEvents publisher first
        if (resourceEventPublisher != null)
        {
            resourceEventPublisher.stop();
            resourceEventPublisher = null;
        }

        // Stop MQTT connector
        if (mqttConnector != null && mqttServer != null)
        {
            var hasNodeId = config.nodeId != null && !config.nodeId.isBlank();
            if (hasNodeId)
                mqttServer.unregisterHandler(config.nodeId + "/", mqttConnector);
            else
                mqttServer.unregisterHandler(endPoint, mqttConnector);
            getLogger().info("CONSYS API MQTT handler unregistered");
            mqttConnector.stop();
            mqttConnector = null;
        }
    }
}
