package org.sensorhub.impl.service.federation.oshconnect;

/**
 * Port of oshconnect.streamableresource.StreamableModes.
 *
 * <ul>
 *   <li>{@code PUSH}: this client publishes outbound messages only.</li>
 *   <li>{@code PULL}: this client subscribes to inbound messages only.</li>
 *   <li>{@code BIDIRECTIONAL}: both publish and subscribe.</li>
 * </ul>
 */
public enum StreamableModes
{
    PUSH("push"),
    PULL("pull"),
    BIDIRECTIONAL("bidirectional");

    private final String value;

    StreamableModes(String value)
    {
        this.value = value;
    }

    public String value()
    {
        return value;
    }
}
