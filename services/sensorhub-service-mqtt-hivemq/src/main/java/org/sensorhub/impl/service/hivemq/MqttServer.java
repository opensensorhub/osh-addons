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

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import org.sensorhub.api.comm.mqtt.IMqttServer;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.module.AbstractModule;
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
    EmbeddedHiveMQ hiveMQ;
    OshExtension oshExtension;
    
    
    @Override
    protected void doStart() throws SensorHubException
    {
        oshExtension = new OshExtension(getParentHub(), this);
        
        EmbeddedHiveMQBuilder hiveMqBuilder = EmbeddedHiveMQ.builder()
            .withConfigurationFolder(Path.of("hivemq-config"))
            .withDataFolder(Path.of("hivemq-data"))
            .withExtensionsFolder(Path.of("hivemq-ext"))
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
            hiveMQ.stop().join();
    }
    
    
    @Override
    public void registerHandler(String topicPrefix, IMqttHandler handler)
    {
        oshExtension.registerHandler(topicPrefix, handler);
    }
    
    
    @Override
    public CompletableFuture<Boolean> publish(String topic, ByteBuffer payload)
    {
        return oshExtension.publish(topic, payload);
    }
    
    
    public static void main(String[] args) throws Exception
    {
        var hiveMq = new MqttServer();
        hiveMq.start();
        Thread.currentThread().join();
    }
}
