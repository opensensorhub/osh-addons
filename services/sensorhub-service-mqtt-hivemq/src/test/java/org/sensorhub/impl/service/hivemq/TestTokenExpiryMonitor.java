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
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import org.junit.Test;
import org.slf4j.LoggerFactory;


public class TestTokenExpiryMonitor
{
    static final Instant NOW = Instant.parse("2026-11-10T12:00:00Z");

    /* a scheduler that keeps the tasks, so the test runs them when it wants */
    static class Timer
    {
        final Runnable task;
        final long delayMs;
        final CompletableFuture<Void> future = new CompletableFuture<>();

        Timer(Runnable task, long delayMs)
        {
            this.task = task;
            this.delayMs = delayMs;
        }

        void fire()
        {
            if (!future.isCancelled())
                task.run();
        }
    }

    final List<Timer> timers = new ArrayList<>();
    final List<String> disconnected = new ArrayList<>();
    final TokenExpiryMonitor monitor = new TokenExpiryMonitor(this::schedule, disconnected::add,
        Clock.fixed(NOW, ZoneOffset.UTC), LoggerFactory.getLogger(TestTokenExpiryMonitor.class));


    Future<?> schedule(Runnable task, long delayMs)
    {
        var timer = new Timer(task, delayMs);
        timers.add(timer);
        return timer.future;
    }


    @Test
    public void testDisconnectAtExpiry()
    {
        monitor.onTokenLogin("conn1", "client1", NOW.plusSeconds(300));
        assertEquals(300_000, timers.get(0).delayMs);
        assertTrue(disconnected.isEmpty());

        timers.get(0).fire();
        assertEquals(List.of("client1"), disconnected);
        assertTrue(monitor.timers.isEmpty());
    }


    @Test
    public void testNoDisconnectWhenClientDisconnectsFirst()
    {
        monitor.onTokenLogin("conn1", "client1", NOW.plusSeconds(300));
        monitor.onDisconnect("conn1");
        assertTrue(timers.get(0).future.isCancelled());
        assertTrue(monitor.timers.isEmpty());

        // also if the timer task was already started
        timers.get(0).task.run();
        assertTrue(disconnected.isEmpty());
    }


    @Test
    public void testConnectionWithoutToken()
    {
        // a password connection has no connection ID
        monitor.onTokenLogin("conn1", "client1", NOW.plusSeconds(300));
        monitor.onDisconnect(null);
        assertEquals(1, monitor.timers.size());
    }


    @Test
    public void testSameClientIdConnectsAgain()
    {
        // the client connects again before its first connection is closed
        monitor.onTokenLogin("conn1", "client1", NOW.plusSeconds(60));
        monitor.onTokenLogin("conn2", "client1", NOW.plusSeconds(300));

        // the close of the first connection cancels its timer only
        monitor.onDisconnect("conn1");
        assertTrue(timers.get(0).future.isCancelled());
        assertFalse(timers.get(1).future.isCancelled());

        timers.get(0).fire();
        assertTrue(disconnected.isEmpty());

        timers.get(1).fire();
        assertEquals(List.of("client1"), disconnected);
    }


    @Test
    public void testMinimumDelay()
    {
        // the token expires while the CONNECT is in progress
        monitor.onTokenLogin("conn1", "client1", NOW.plusMillis(10));
        monitor.onTokenLogin("conn2", "client2", NOW.minusSeconds(1));
        assertEquals(TokenExpiryMonitor.MIN_DELAY_MS, timers.get(0).delayMs);
        assertEquals(TokenExpiryMonitor.MIN_DELAY_MS, timers.get(1).delayMs);
    }


    @Test
    public void testStop()
    {
        monitor.onTokenLogin("conn1", "client1", NOW.plusSeconds(60));
        monitor.onTokenLogin("conn2", "client2", NOW.plusSeconds(300));
        monitor.stop();

        assertTrue(monitor.timers.isEmpty());
        for (var timer: timers)
        {
            assertTrue(timer.future.isCancelled());
            timer.task.run();
        }
        assertTrue(disconnected.isEmpty());
    }
}
