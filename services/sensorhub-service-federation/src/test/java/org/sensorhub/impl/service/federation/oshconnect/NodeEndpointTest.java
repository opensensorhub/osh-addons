package org.sensorhub.impl.service.federation.oshconnect;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;


/**
 * Node identity decides whether a configured "remote" is really the commander,
 * which is what keeps a self-federating config inert instead of looping.
 */
public class NodeEndpointTest
{
    /** MQTT is not started, so no broker connection is attempted. */
    private static Node node(String address, int port)
    {
        return new Node("http", address, port, null, null, false, 1883);
    }


    @Test
    public void sameAddressAndPortIsTheSameNode()
    {
        assertTrue(Node.sameEndpoint(node("10.10.0.26", 18282), node("10.10.0.26", 18282)));
    }


    @Test
    public void addressIsComparedCaseInsensitively()
    {
        assertTrue(Node.sameEndpoint(node("Commander.Local", 18282), node("commander.local", 18282)));
    }


    @Test
    public void aDifferentPortIsADifferentNode()
    {
        assertFalse(Node.sameEndpoint(node("10.10.0.26", 18282), node("10.10.0.26", 18283)));
    }


    @Test
    public void differentHostsAreNotTheSameNode()
    {
        assertFalse(Node.sameEndpoint(node("10.10.0.26", 18282), node("10.10.0.27", 18282)));
    }


    @Test
    public void localhostSpellingsResolveToTheSameNode()
    {
        // the case a plain string compare would miss
        assertTrue(Node.sameEndpoint(node("localhost", 18282), node("127.0.0.1", 18282)));
    }


    @Test
    public void unresolvableHostsDoNotThrow()
    {
        // failing "not the same" only loses the short-circuit; throwing would break discovery
        assertFalse(Node.sameEndpoint(node("no-such-host.invalid", 18282), node("10.10.0.26", 18282)));
    }


    @Test
    public void nullsAreNeverTheSameNode()
    {
        assertFalse(Node.sameEndpoint(null, node("10.10.0.26", 18282)));
        assertFalse(Node.sameEndpoint(node("10.10.0.26", 18282), null));
    }
}
