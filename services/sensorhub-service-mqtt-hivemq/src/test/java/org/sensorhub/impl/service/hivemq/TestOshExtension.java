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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.Assert.*;
import java.nio.ByteBuffer;
import java.security.AccessControlException;
import org.junit.Test;
import org.sensorhub.api.comm.mqtt.IMqttServer;
import org.sensorhub.api.comm.mqtt.IMqttServer.IMqttHandler;
import org.sensorhub.api.comm.mqtt.InvalidTopicException;


public class TestOshExtension
{
    static final String TOPIC = "api/systems/1/datastreams/2/observations:data";

    final List<String> removedFromBroker = new ArrayList<>();
    final OshExtension ext = new OshExtension(null, new MqttServer()) {
        @Override
        void removeBrokerSubscription(String clientId, String topic)
        {
            removedFromBroker.add(clientId + " " + topic);
        }
    };
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


    @Test
    public void testExactSubscriptionIsSentWithoutCheck() throws Exception
    {
        assertFalse(ext.isExactSubscription("client1", TOPIC));
        ext.subscribeToTopic("user", "client1", TOPIC);
        assertTrue(ext.isExactSubscription("client1", TOPIC));
        assertFalse(ext.isExactSubscription("client2", TOPIC));

        // the filter of a wildcard subscription is not a topic of a message
        ext.subscribeToTopic("user", "client1", "api/systems/+");
        assertFalse(ext.isExactSubscription("client1", "api/systems/1"));
        assertTrue(handler.delivers.isEmpty());

        // unsubscribe removes the exact topic
        ext.unsubscribeFromTopic("user", "client1", TOPIC);
        assertFalse(ext.isExactSubscription("client1", TOPIC));
    }


    @Test
    public void testWildcardMatchAsksHandler() throws Exception
    {
        handler.deliver = true;
        assertTrue(ext.canDeliverToWildcard("user1", "client1", "api/systems/1"));
        handler.deliver = false;
        assertFalse(ext.canDeliverToWildcard("user1", "client1", "api/systems/2"));
        assertEquals(java.util.List.of("user1 api/systems/1", "user1 api/systems/2"), handler.delivers);
    }


    @Test
    public void testWildcardMatchWithoutHandler()
    {
        assertFalse(ext.canDeliverToWildcard("user", "client1", "other/topic"));
        assertTrue(handler.delivers.isEmpty());
    }


    @Test
    public void testWildcardMatchHandlerError()
    {
        handler.failDeliver = true;
        assertFalse(ext.canDeliverToWildcard("user", "client1", "api/systems/1"));
    }


    @Test
    public void testWildcardsNeedHandlerSupport() throws Exception
    {
        handler.wildcards = false;
        assertThrows(InvalidTopicException.class, () -> ext.subscribeToTopic("user", "client1", "api/systems/+"));
        assertThrows(InvalidTopicException.class, () -> ext.subscribeToTopic("user", "client1", "api/#"));
        assertEquals(0, handler.subscribes);

        handler.wildcards = true;
        assertTrue(ext.subscribeToTopic("user", "client1", "api/systems/+"));
        assertEquals(1, handler.subscribes);
    }


    @Test
    public void testDefaultHandlerSendsNothingToWildcards()
    {
        IMqttHandler legacy = new IMqttHandler() {
            public void onPublish(String userID, String topic, ByteBuffer payload, ByteBuffer correlData) {}
            public void onSubscribe(String userID, String topic, IMqttServer server) {}
            public void onUnsubscribe(String userID, String topic, IMqttServer server) {}
        };
        ext.registerHandler("legacy/", legacy);
        assertFalse(ext.canDeliverToWildcard("user", "client1", "legacy/a"));
        assertThrows(InvalidTopicException.class, () -> ext.subscribeToTopic("user", "client1", "legacy/+"));
    }


    @Test
    public void testRestartedHandlerGetsExistingSubscriptions() throws Exception
    {
        assertTrue(ext.subscribeToTopic("user1", "client1", TOPIC));
        assertTrue(ext.subscribeToTopic("user2", "client2", TOPIC));

        // the handler module restarts while HiveMQ keeps the subscriptions
        ext.unregisterHandler("api/", handler);
        var restarted = new CountingHandler();
        ext.registerHandler("api/", restarted);

        assertEquals(2, restarted.subscribes);
        assertEquals(Set.of("user1", "user2"), restarted.users);
        assertTrue(ext.clientTopics.get("client1").contains(TOPIC));
        assertTrue(removedFromBroker.isEmpty());

        // a new SUBSCRIBE to the same topic does not count the client twice
        assertTrue(ext.subscribeToTopic("user1", "client1", TOPIC));
        assertEquals(2, restarted.subscribes);
    }


    @Test
    public void testFailedRestoreRemovesSubscription() throws Exception
    {
        assertTrue(ext.subscribeToTopic("user", "client1", TOPIC));

        // the user lost the permission while the handler was stopped
        ext.unregisterHandler("api/", handler);
        var restarted = new CountingHandler();
        restarted.allow = false;
        ext.registerHandler("api/", restarted);

        assertEquals(1, restarted.subscribes);
        assertFalse(ext.clientTopics.get("client1").contains(TOPIC));
        assertEquals(List.of("client1 " + TOPIC), removedFromBroker);
    }


    @Test
    public void testRestoreOnlyTopicsOfNewHandler() throws Exception
    {
        var other = new CountingHandler();
        ext.registerHandler("other/", other);
        assertTrue(ext.subscribeToTopic("user", "client1", TOPIC));
        assertTrue(ext.subscribeToTopic("user", "client1", "other/topic"));

        ext.unregisterHandler("api/", handler);
        var restarted = new CountingHandler();
        ext.registerHandler("api/", restarted);

        assertEquals(1, restarted.subscribes);
        assertEquals(1, other.subscribes);
    }


    static class CountingHandler implements IMqttHandler
    {
        volatile boolean allow = true;
        volatile boolean deliver = true;
        volatile boolean failDeliver;
        volatile boolean wildcards = true;
        int subscribes, unsubscribes;
        final Set<String> users = new HashSet<>();
        final java.util.List<String> delivers = new java.util.ArrayList<>();

        @Override
        public boolean supportsWildcards()
        {
            return wildcards;
        }

        @Override
        public boolean canDeliver(String userID, String topic)
        {
            if (failDeliver)
                throw new IllegalStateException("check failed");
            delivers.add(userID + " " + topic);
            return deliver;
        }

        @Override
        public void onSubscribe(String userID, String topic, IMqttServer server)
        {
            subscribes++;
            users.add(userID);
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
