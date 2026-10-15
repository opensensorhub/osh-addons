package org.sensorhub.impl.service.federation;

import org.sensorhub.impl.service.federation.oshconnect.MqttCommClient;

/**
 * Port of broker.registry.PumpEntry — one active pump/forwarder tracked by the
 * reconcile loop so it can be individually torn down (its thread interrupted,
 * its MQTT topic unsubscribed, its commander mirror unmapped/deleted).
 *
 * The Python version carries a {@code threading.Event}; the Java equivalent is
 * the pump {@link Thread} itself — interrupting it is the per-stream stop signal.
 */
public class PumpEntry
{
    public final String key;              // node-qualified remote stream key
    public final String kind;             // "obs" or "cmd"
    public final Thread thread;           // the pump/forwarder thread (interrupt to stop)
    public final MqttCommClient mqttClient; // shared node client; unsubscribe topic only
    public final String subscribeTopic;   // topic actually subscribed
    public final String commanderId;      // commander-side datastream/control-stream id
    public final String systemUrn;
    public final String schemaSig;        // for drift detection
    public int lastSeenCycle;
    public int absentCycles;

    public PumpEntry(String key, String kind, Thread thread, MqttCommClient mqttClient,
                     String subscribeTopic, String commanderId, String systemUrn,
                     String schemaSig, int lastSeenCycle)
    {
        this.key = key;
        this.kind = kind;
        this.thread = thread;
        this.mqttClient = mqttClient;
        this.subscribeTopic = subscribeTopic;
        this.commanderId = commanderId;
        this.systemUrn = systemUrn;
        this.schemaSig = schemaSig;
        this.lastSeenCycle = lastSeenCycle;
        this.absentCycles = 0;
    }
}
