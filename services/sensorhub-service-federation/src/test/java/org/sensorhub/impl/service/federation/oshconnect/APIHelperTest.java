package org.sensorhub.impl.service.federation.oshconnect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;


/**
 * The MQTT topic root and the HTTP API root are configured independently and
 * are normalized differently: a leading slash is significant for the topic root
 * (it is how a node publishing without a nodeId is addressed) but not for a
 * path segment.
 */
public class APIHelperTest
{
    @Test
    public void mqttRootKeepsLeadingSlashAndDropsTrailing()
    {
        assertEquals("axis1", APIHelper.normalizeMqttRoot("axis1"));
        assertEquals("site1/osh", APIHelper.normalizeMqttRoot("site1/osh/"));
        // significant: this is the endpoint-prefixed form used when a node has no nodeId
        assertEquals("/api", APIHelper.normalizeMqttRoot("/api"));
        assertEquals("axis1", APIHelper.normalizeMqttRoot("  axis1  "));
    }


    @Test
    public void blankMqttRootFallsBackToTheDefault()
    {
        assertNull(APIHelper.normalizeMqttRoot(null));
        assertNull(APIHelper.normalizeMqttRoot("   "));
        assertNull(APIHelper.normalizeMqttRoot("/"));
    }


    @Test
    public void apiRootDropsARedundantServerRootSegment()
    {
        // the config field ships as "sensorhub/api" while the URL is built as
        // /{serverRoot}/{apiRoot}, so taking it literally would double the segment
        assertEquals("api", APIHelper.normalizeApiRoot("sensorhub/api", "sensorhub"));
        assertEquals("api", APIHelper.normalizeApiRoot("/sensorhub/api/", "sensorhub"));
        assertEquals("api", APIHelper.normalizeApiRoot("sensorhub", "sensorhub"));
        assertEquals("api", APIHelper.normalizeApiRoot("api", "sensorhub"));
    }


    @Test
    public void apiRootKeepsANonDefaultPath()
    {
        assertEquals("csapi", APIHelper.normalizeApiRoot("csapi", "sensorhub"));
        assertEquals("api", APIHelper.normalizeApiRoot(null, "sensorhub"));
    }


    @Test
    public void pathSegmentTrimsSlashesAndFallsBack()
    {
        assertEquals("osh", APIHelper.normalizePathSegment("/osh/", "sensorhub"));
        assertEquals("sensorhub", APIHelper.normalizePathSegment(null, "sensorhub"));
        assertEquals("sensorhub", APIHelper.normalizePathSegment("  ", "sensorhub"));
    }
}
