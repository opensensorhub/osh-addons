/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.mqtt;

import static org.junit.Assert.*;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.comm.mqtt.IMqttServer;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.module.ModuleEvent.ModuleState;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.module.AbstractModule;
import org.sensorhub.impl.module.ModuleRegistry;


public class TestAbstractMqttServiceModule
{
    static final long TIMEOUT = 5000;

    ModuleRegistry registry;
    FakeMqttServer server;


    public static class FakeMqttServer extends AbstractModule<ModuleConfig> implements IMqttServer
    {
        @Override
        public void registerHandler(String topicPrefix, IMqttHandler handler) {}

        @Override
        public void unregisterHandler(String topicPrefix, IMqttHandler handler) {}

        @Override
        public CompletableFuture<Boolean> publish(String topic, ByteBuffer payload)
        {
            return CompletableFuture.completedFuture(true);
        }

        @Override
        public CompletableFuture<Boolean> publish(String topic, ByteBuffer payload, ByteBuffer correlData)
        {
            return CompletableFuture.completedFuture(true);
        }
    }


    public static class FakeMqttService extends AbstractMqttServiceModule<ModuleConfig>
    {
    }


    ModuleConfig config(Class<?> moduleClass, String id)
    {
        var config = new ModuleConfig();
        config.id = id;
        config.name = id;
        config.moduleClass = moduleClass.getName();
        config.autoStart = false;
        return config;
    }


    @Before
    public void setup() throws Exception
    {
        var hub = new SensorHub();
        hub.start();
        registry = hub.getModuleRegistry();

        server = (FakeMqttServer)registry.loadModule(config(FakeMqttServer.class, "mqtt-server"));
        server.init();
        server.start();
        assertTrue(server.waitForState(ModuleState.STARTED, TIMEOUT));
    }


    FakeMqttService startService() throws Exception
    {
        var service = (FakeMqttService)registry.loadModule(config(FakeMqttService.class, "mqtt-service"));
        service.init();
        service.start();
        assertTrue(service.waitForState(ModuleState.STARTED, TIMEOUT));
        return service;
    }


    @Test
    public void testNoWaitingStatusWhenServerAlreadyStarted() throws Exception
    {
        var service = startService();
        assertNull(service.getStatusMessage());
    }


    @Test
    public void testNoWaitingStatusAfterServiceRestart() throws Exception
    {
        var service = startService();

        service.stop();
        assertTrue(service.waitForState(ModuleState.STOPPED, TIMEOUT));
        service.start();
        assertTrue(service.waitForState(ModuleState.STARTED, TIMEOUT));
        assertNull(service.getStatusMessage());
    }


    @Test
    public void testWaitingStatusUntilServerStarts() throws Exception
    {
        server.stop();
        assertTrue(server.waitForState(ModuleState.STOPPED, TIMEOUT));

        var service = (FakeMqttService)registry.loadModule(config(FakeMqttService.class, "mqtt-service"));
        service.init();
        service.start();
        assertEquals("Waiting for MQTT server...", service.getStatusMessage());
        assertNotEquals(ModuleState.STARTED, service.getCurrentState());

        server.start();
        assertTrue(service.waitForState(ModuleState.STARTED, TIMEOUT));
        assertNull(service.getStatusMessage());
    }
}
