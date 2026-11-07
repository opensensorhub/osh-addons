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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
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
    
    // SHA-256 of earlier default configs, without whitespace. They disable wildcards.
    static final Set<String> OLD_DEFAULT_CONFIGS = Set.of(
        "4508192cfda8841d6193ae7c30343c1ebfef1919dfc0b172ab839c9c9fddb157",
        "42f5b9f635f4b759de3eae362f8735b7bd0abe872318980ee6ba309057e86b3a");
    static final Pattern WILDCARDS_DISABLED = Pattern.compile(
        "<wildcard-subscriptions>\\s*<enabled>\\s*false\\s*</enabled>");
    
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
        // an unchanged earlier default config is replaced, to enable wildcards
        File configFile = new File(configFolder, CONFIG_FILE_NAME);
        boolean oldDefault = configFile.exists() && isOldDefaultConfig(configFile.toPath());
        if (!configFile.exists() || oldDefault)
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
            
            if (oldDefault)
                getLogger().info("Replaced earlier default HiveMQ config {} with the new default (wildcard subscriptions enabled)", configFile);
        }
        else if (wildcardsDisabled(configFile.toPath()))
        {
            getLogger().warn("Wildcard subscriptions are disabled in {}. "
                + "Set <wildcard-subscriptions><enabled>true</enabled> to allow them.", configFile);
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
    
    
    /** @return true if the file is an earlier default config that the user did not change */
    static boolean isOldDefaultConfig(Path file)
    {
        try
        {
            var text = Files.readString(file, StandardCharsets.UTF_8).replaceAll("\\s", "");
            var hash = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return OLD_DEFAULT_CONFIGS.contains(HexFormat.of().formatHex(hash));
        }
        catch (IOException | NoSuchAlgorithmException e)
        {
            return false;
        }
    }
    
    
    static boolean wildcardsDisabled(Path file)
    {
        try
        {
            return WILDCARDS_DISABLED.matcher(Files.readString(file, StandardCharsets.UTF_8)).find();
        }
        catch (IOException e)
        {
            return false;
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


    @Override
    public CompletableFuture<Boolean> publish(String topic, ByteBuffer payload, ByteBuffer correlData)
    {
        return oshExtension.publish(topic, payload, correlData);
    }
}
