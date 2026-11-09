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
                    mqttConnector = new ConSysApiMqttConnector(servlet, endPoint);
                    mqttServer.registerHandler(endPoint, mqttConnector);
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
        
        // stop MQTT extension
        if (mqttConnector != null && mqttServer != null)
        {
            mqttServer.unregisterHandler(endPoint, mqttConnector);
                getLogger().info("CONSYS API MQTT handler unregistered");
                mqttConnector.stop();
                mqttConnector = null;
        }
    }
}
