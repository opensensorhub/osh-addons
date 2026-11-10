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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import org.slf4j.Logger;


/**
 * <p>
 * Disconnects a client when the access token of its connection expires.
 * Each timer belongs to one connection, not to a client ID: a client can
 * connect again with the same client ID before its earlier connection is
 * closed.
 * </p>
 */
class TokenExpiryMonitor
{
    // a connection is not ready to be disconnected until the CONNECT is complete
    static final long MIN_DELAY_MS = 1000;

    interface Scheduler
    {
        Future<?> schedule(Runnable task, long delayMs);
    }

    interface Disconnector
    {
        void disconnect(String clientId);
    }

    final Scheduler scheduler;
    final Disconnector disconnector;
    final Clock clock;
    final Logger log;
    final Map<String, Future<?>> timers = new ConcurrentHashMap<>();


    TokenExpiryMonitor(Scheduler scheduler, Disconnector disconnector, Clock clock, Logger log)
    {
        this.scheduler = scheduler;
        this.disconnector = disconnector;
        this.clock = clock;
        this.log = log;
    }


    /**
     * Start the timer of a connection that logged in with a token
     * @param connId unique ID of the connection
     * @param clientId MQTT client ID of the connection
     * @param expiry expiry time of the token
     */
    void onTokenLogin(String connId, String clientId, Instant expiry)
    {
        var delayMs = Math.max(MIN_DELAY_MS, Duration.between(clock.instant(), expiry).toMillis());
        var timer = scheduler.schedule(() -> {
            // the timer is removed when its connection closes, so the client
            // ID still belongs to this connection
            if (timers.remove(connId) != null)
            {
                log.debug("Token of client {} expired: disconnecting", clientId);
                disconnector.disconnect(clientId);
            }
        }, delayMs);
        timers.put(connId, timer);
    }


    /**
     * Cancel the timer of a connection that closed
     * @param connId unique ID of the connection, or null if it has no timer
     */
    void onDisconnect(String connId)
    {
        if (connId == null)
            return;

        var timer = timers.remove(connId);
        if (timer != null)
            timer.cancel(false);
    }


    void stop()
    {
        for (var connId: timers.keySet())
            onDisconnect(connId);
    }
}
