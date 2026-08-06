package org.sensorhub.impl.service.federation;

import java.util.List;
import java.util.Map;

import org.sensorhub.impl.service.federation.environment.EnvironmentData;
import org.sensorhub.impl.service.federation.events.EventHandler;
import org.sensorhub.impl.service.federation.nodes.CommanderNode;
import org.sensorhub.impl.service.federation.nodes.RemoteNode;
import org.sensorhub.impl.service.federation.oshconnect.ControlStream;
import org.sensorhub.impl.service.federation.oshconnect.Datastream;
import org.sensorhub.impl.service.federation.oshconnect.System;

/**
 * Shared state accessors for the broker mixins. In the Python broker each mixin
 * reaches {@code self.command_nodes}, {@code self.ds_map}, etc. directly because
 * at runtime {@code self} is always the composed {@code OSHDataBroker}. Java
 * interfaces can't hold state, so the mixins read it through this context, which
 * {@link OSHDataBroker} implements.
 */
public interface BrokerContext
{
    EventHandler getEventHandler();

    /** Mirror of {@code self._env}. */
    EnvironmentData getEnv();

    List<CommanderNode> getCommandNodes();

    List<RemoteNode> getRemoteNodes();

    /** Mirror of {@code self.ds_map}: remote datastream id -> commander Datastream. */
    Map<String, Datastream> getDsMap();

    /** Mirror of {@code self.cs_map}: commander cs id -> (remote System, remote ControlStream). */
    Map<String, Map.Entry<System, ControlStream>> getCsMap();

    /**
     * Long-running daemon threads spawned by the broker (observation pumps and
     * command-forwarding loops). Tracked so the service can interrupt them on
     * shutdown — the Python broker had no teardown, but an OSH module can be
     * stopped/restarted within a live JVM and must not leak forwarders.
     */
    List<Thread> getWorkerThreads();

    /** Whether binary (e.g. video) datastreams are federated via swe+binary passthrough. */
    boolean isBinaryDatastreamsEnabled();
}
