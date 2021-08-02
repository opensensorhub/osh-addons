/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sweapi;

import org.sensorhub.api.comm.mqtt.IMqttServer;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.ModuleEvent.ModuleState;


/**
 * <p>
 * Extension of {@link SWEApiService} to enable the MQTT extension
 * </p>
 *
 * @author Alex Robin
 * @since Jul 28, 2021
 */
public class SWEApiWithMqttService extends SWEApiService
{
    protected SWEApiMqttConnector mqttConnector;
    
    
    @Override
    protected void doStart() throws SensorHubException
    {
        super.doStart();
        
        // also enable MQTT extension if an MQTT server is available
        if (config instanceof SWEApiWithMqttServiceConfig && ((SWEApiWithMqttServiceConfig)config).enableMqtt)
        {
            getParentHub().getModuleRegistry().waitForModuleType(IMqttServer.class, ModuleState.STARTED)
                .thenAccept(mqtt -> {
                    if (mqtt != null)
                    {
                        mqttConnector = new SWEApiMqttConnector(servlet, config.endPoint);
                        mqtt.registerHandler(config.endPoint, mqttConnector);
                        getLogger().info("SensorWeb API MQTT handler registered");
                    }
                });
        }
    }
    
    
    @Override
    protected void doStop()
    {
        super.doStop();
        
        // also stop MQTT extension if it was enabled
        if (mqttConnector != null)
        {
            getParentHub().getModuleRegistry().waitForModuleType(IMqttServer.class, ModuleState.STARTED)
                .thenAccept(mqtt -> {
                    if (mqtt != null)
                    {
                        mqtt.unregisterHandler(config.endPoint, mqttConnector);
                        getLogger().info("SensorWeb API MQTT handler unregistered");
                        mqttConnector.stop();
                        mqttConnector = null;
                    }
                });
        }
    }
}
