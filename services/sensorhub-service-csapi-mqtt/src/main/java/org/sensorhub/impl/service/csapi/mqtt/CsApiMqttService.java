/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.csapi.mqtt;

import java.util.ArrayList;
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
public class CsApiMqttService extends AbstractMqttServiceModule<CsApiMqttServiceConfig> implements IServiceModule<CsApiMqttServiceConfig>
{
    protected volatile CsApiMqttConnector mqttConnector;


    @Override
    protected void doInit() throws SensorHubException
    {
        super.doInit();

        try
        {
            TopicMapper.checkNodeId(config.nodeId);
        }
        catch (IllegalArgumentException e)
        {
            throw new SensorHubException("Invalid configuration: " + e.getMessage());
        }
    }


    @Override
    protected void doStart() throws SensorHubException
    {
        super.doStart();

        // try to attach to CONSYS API REST service
        getParentHub().getModuleRegistry().waitForModuleType(ConSysApiService.class, ModuleState.STARTED)
            .thenAccept(service -> {
                var servlet = service.getServlet();

                if (mqttServer != null)
                {
                    // format subtopics: standard formats and the API's custom formats
                    var formats = new ArrayList<>(TopicMapper.STANDARD_FORMATS);
                    for (var f: service.getConfiguration().customFormats)
                        formats.add(f.mimeType);
                    var topics = new TopicMapper(config.nodeId, formats);
                    mqttConnector = new CsApiMqttConnector(servlet, topics);
                    mqttServer.registerHandler(topics.getPrefix(), mqttConnector);

                    // AsyncAPI service description at {apiRoot}/asyncapi/mqtt
                    var version = new CsApiMqttServiceDescriptor().getModuleVersion();
                    servlet.getRootHandler().addSubResource(new AsyncApiHandler(version, () -> config,
                        () -> mqttConnector != null ? mqttConnector.topics : null));
                    getLogger().info("CS API MQTT handler registered on '{}', format subtopics {}", topics.getPrefix(), topics.formats.keySet());
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
        
        // stop MQTT extension
        if (mqttConnector != null && mqttServer != null)
        {
            // the connector keeps the prefix it was registered with
            mqttServer.unregisterHandler(mqttConnector.topics.getPrefix(), mqttConnector);
            getLogger().info("CS API MQTT handler unregistered");
            mqttConnector.stop();
            mqttConnector = null;
        }
    }
}
