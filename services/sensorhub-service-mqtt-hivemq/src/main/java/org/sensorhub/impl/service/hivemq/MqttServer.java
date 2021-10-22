/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.hivemq;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import org.sensorhub.api.comm.mqtt.IMqttServer;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.module.AbstractModule;
import com.google.common.io.ByteStreams;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.embedded.EmbeddedHiveMQ;
import com.hivemq.embedded.EmbeddedHiveMQBuilder;
import com.hivemq.migration.meta.PersistenceType;


/**
 * <p>
 * MQTT service implementation based on an embedded HiveMQ server
 * </p>
 *
 * @author Alex Robin
 * @since Apr 14, 2021
 */
public class MqttServer extends AbstractModule<MqttServerConfig> implements IMqttServer
{
    private static final String CONFIG_FILE_NAME = "config.xml";
    
    EmbeddedHiveMQ hiveMQ;
    OshExtension oshExtension;
    
    
    @Override
    protected void doStart() throws SensorHubException
    {
        oshExtension = new OshExtension(getParentHub(), this);
        
        // if config is not provided, create config folder
        File configFolder = new File(config.configFolder);
        if (!configFolder.exists())
            configFolder.mkdirs();
        
        // if no config was provided, copy default config
        File configFile = new File(configFolder, CONFIG_FILE_NAME);
        if (!configFile.exists())
        {
            try (var outFile = new FileOutputStream(configFile))
            {
                var defaultConfig = getClass().getResourceAsStream(CONFIG_FILE_NAME);
                ByteStreams.copy(defaultConfig, outFile);
            }
            catch (IOException e)
            {
                throw new SensorHubException("Error creating config file", e);
            }
        }
        
        // init embedded HiveMQ server
        EmbeddedHiveMQBuilder hiveMqBuilder = EmbeddedHiveMQ.builder()
            .withConfigurationFolder(Path.of(config.configFolder))
            .withDataFolder(Path.of(config.dataFolder))
            .withExtensionsFolder(Path.of(config.configFolder))
            .withEmbeddedExtension(oshExtension);
        
        try
        {
            this.hiveMQ = hiveMqBuilder.build();
            InternalConfigurations.PAYLOAD_PERSISTENCE_TYPE.set(PersistenceType.FILE);
            InternalConfigurations.RETAINED_MESSAGE_PERSISTENCE_TYPE.set(PersistenceType.FILE);
            hiveMQ.start().join();
        }
        catch (final Exception e)
        {
            throw new SensorHubException("Cannot start embedded HiveMQ server", e);
        }
    }
    
    
    @Override
    protected void doStop() throws SensorHubException
    {
        if (hiveMQ != null)
        {
            try
            {
                hiveMQ.close();
                hiveMQ = null;
            }
            catch (Exception e)
            {
                throw new SensorHubException("Error closing embedded HiveMQ server", e);
            }
        }
    }
    
    
    @Override
    public void registerHandler(String topicPrefix, IMqttHandler handler)
    {
        oshExtension.registerHandler(topicPrefix, handler);
    }
    
    
    @Override
    public void unregisterHandler(String topicPrefix, IMqttHandler handler)
    {
        oshExtension.unregisterHandler(topicPrefix, handler);
    }
    
    
    @Override
    public CompletableFuture<Boolean> publish(String topic, ByteBuffer payload)
    {
        return oshExtension.publish(topic, payload);
    }
}
