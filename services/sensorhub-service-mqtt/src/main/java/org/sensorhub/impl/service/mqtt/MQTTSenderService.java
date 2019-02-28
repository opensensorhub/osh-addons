/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.mqtt;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.IDataProducerModule;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.module.AbstractModule;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.service.mqtt.MQTTSenderConfig.MQTTDataSourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>
 * Main module implementing MQTT service
 * </p>
 *
 * <p>Copyright (c) 2015 Sensia Software LLC</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Oct 15, 2015
 */
public class MQTTSenderService extends AbstractModule<MQTTSenderConfig>
{
    static final Logger log = LoggerFactory.getLogger(MQTTSenderService.class);
    
    MqttAsyncClient client;
    List<MQTTDataHandler> handlers = new ArrayList<MQTTDataHandler>();
    
    
    public MQTTSenderService()
    {
    }    


    @Override
    public void start() throws SensorHubException
    {
        try
        {
            this.client = new MqttAsyncClient(config.brokerUrl, "sensorhub", new MemoryPersistence());
        }
        catch (MqttException e)
        {
            e.printStackTrace();
        }
        
        // init one handler for each stream
        ModuleRegistry reg = SensorHub.getInstance().getModuleRegistry();
        for (MQTTDataSourceConfig sourceConfig: config.dataSources)
        {
            IDataProducerModule<?> module = (IDataProducerModule<?>)reg.getModuleById(sourceConfig.streamSourceID);
                        
            // get outputs w/o hidden outputs
            Set<String> outputNames = module.getAllOutputs().keySet();
            if (sourceConfig.hiddenOutputs != null)
            {
                for (String output: sourceConfig.hiddenOutputs)
                    outputNames.remove(output);
            }
            
            // start handler for each output
            for (String output: outputNames)
            {
                IStreamingDataInterface outInterface = module.getAllOutputs().get(output);
                String topic = sourceConfig.topicPrefix + "/" + output; 
                MQTTDataHandler handler = new MQTTDataHandler(client, topic, outInterface);
                handlers.add(handler);                
            }
        }
        
        // connect client
        try
        {
            MqttConnectOptions connectOpts = new MqttConnectOptions();
            client.connect(connectOpts, null, new IMqttActionListener()
            {
                @Override
                public void onFailure(IMqttToken token, Throwable e)
                {
                    log.error("Error while connecting to MQTT broker " + config.brokerUrl, e);
                }

                @Override
                public void onSuccess(IMqttToken arg0)
                {
                    try
                    {
                        for (MQTTDataHandler handler: handlers)
                            handler.start();
                    }
                    catch (SensorHubException e)
                    {
                        e.printStackTrace();
                    }                    
                }
            });
        }
        catch (MqttException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    

    @Override
    public void stop() throws SensorHubException
    {
        for (MQTTDataHandler handler: handlers)
            handler.stop();
        
        try
        {
            if (client != null)
                client.disconnect();
        }
        catch (MqttException e)
        {
            e.printStackTrace();
        }
    }
    

    @Override
    public void cleanup() throws SensorHubException
    {
        
    }
}
