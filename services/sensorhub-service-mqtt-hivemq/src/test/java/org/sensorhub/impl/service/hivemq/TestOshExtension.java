/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2026 GeoRobotix Innovative Research. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.hivemq;

import static org.junit.Assert.*;
import java.nio.ByteBuffer;
import java.security.AccessControlException;
import org.junit.Test;
import org.sensorhub.api.comm.mqtt.IMqttServer;
import org.sensorhub.api.comm.mqtt.IMqttServer.IMqttHandler;


public class TestOshExtension
{
    static final String TOPIC = "api/systems/1/datastreams/2/observations:data";

    final OshExtension ext = new OshExtension(null, new MqttServer());
    final CountingHandler handler = new CountingHandler();


    public TestOshExtension()
    {
        ext.registerHandler("api/", handler);
    }


    @Test
    public void testRejectedSubscribeIsCheckedOnRetry() throws Exception
    {
        handler.allow = false;
        assertThrows(AccessControlException.class, () -> ext.subscribeToTopic("user", "client1", TOPIC));
        assertThrows(AccessControlException.class, () -> ext.subscribeToTopic("user", "client1", TOPIC));
        assertEquals(2, handler.subscribes);
        assertFalse(ext.clientTopics.get("client1").contains(TOPIC));

        // accepted once the user gets the permission
        handler.allow = true;
        assertTrue(ext.subscribeToTopic("user", "client1", TOPIC));
        assertEquals(3, handler.subscribes);
        assertTrue(ext.clientTopics.get("client1").contains(TOPIC));
    }


    @Test
    public void testRepeatedSubscribeCallsHandlerOnce() throws Exception
    {
        assertTrue(ext.subscribeToTopic("user", "client1", TOPIC));
        assertTrue(ext.subscribeToTopic("user", "client1", TOPIC));
        assertEquals(1, handler.subscribes);

        // other clients are checked separately
        assertTrue(ext.subscribeToTopic("user", "client2", TOPIC));
        assertEquals(2, handler.subscribes);
        assertEquals(0, handler.unsubscribes);
    }


    @Test
    public void testTopicWithoutHandler() throws Exception
    {
        assertFalse(ext.subscribeToTopic("user", "client1", "other/topic"));
        assertEquals(0, handler.subscribes);
    }


    static class CountingHandler implements IMqttHandler
    {
        volatile boolean allow = true;
        int subscribes, unsubscribes;

        @Override
        public void onSubscribe(String userID, String topic, IMqttServer server)
        {
            subscribes++;
            if (!allow)
                throw new AccessControlException("not allowed");
        }

        @Override
        public void onUnsubscribe(String userID, String topic, IMqttServer server)
        {
            unsubscribes++;
        }

        @Override
        public void onPublish(String userID, String topic, ByteBuffer payload, ByteBuffer correlData)
        {
        }
    }
}
