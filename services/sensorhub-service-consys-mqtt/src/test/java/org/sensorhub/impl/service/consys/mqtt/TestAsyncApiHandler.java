/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2026 GeoRobotix Innovative Research. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.consys.mqtt;

import static org.junit.Assert.*;
import org.junit.Test;


public class TestAsyncApiHandler
{
    static final String API_ROOT = "http://host.example:8181/sensorhub/api";
    final TopicMapper mapper = new TopicMapper("node1", TopicMapper.STANDARD_FORMATS);
    final ConSysApiMqttServiceConfig config = new ConSysApiMqttServiceConfig();
    final AsyncApiHandler handler = new AsyncApiHandler("3.0.0", () -> config, () -> mapper);


    @Test
    public void testRender()
    {
        var doc = handler.render(mapper, API_ROOT);

        assertFalse("unfilled placeholder", doc.contains("${"));
        assertTrue(doc.contains("version: 3.0.0"));
        assertTrue(doc.contains("address: 'node1/systems/{systemId}/datastreams/{dataStreamId}/observations:data/{format}'"));
        assertTrue(doc.contains("href: 'http://host.example:8181/sensorhub/api/datastreams'"));
        assertTrue(doc.contains("enum: [geo-json, json, om-json, sml-json, sml-xml, swe-binary, swe-csv, swe-json, swe-xml]"));
        assertTrue(doc.contains("- `swe-csv`: `application/swe+csv`"));

        // Resource Events: topics, media type and the event types published
        assertTrue(doc.contains("address: 'node1/systems/{systemId}/datastreams/{dataStreamId}'"));
        assertTrue(doc.contains("contentType: application/cloudevents+json"));
        for (var type: new String[] {"system", "datastream", "controlstream"})
            assertTrue(doc.contains("org.ogc.api.csapi." + type + ".create, org.ogc.api.csapi." + type + ".update, org.ogc.api.csapi." + type + ".delete"));

        // Batch Resource Events of observations, with the configured window
        assertTrue(doc.contains("address: 'node1/systems/{systemId}/datastreams/{dataStreamId}/observations'"));
        assertTrue(doc.contains("enum: [org.ogc.api.csapi.observation.create]"));
        assertTrue(doc.contains("window of 1000 ms"));
        config.batchEventWindowMs = 250;
        assertTrue(handler.render(mapper, API_ROOT).contains("window of 250 ms"));
    }


    @Test
    public void testTcpServers()
    {
        var doc = handler.render(mapper, API_ROOT);
        assertTrue(doc.contains("  mqtt5:\n    host: 'host.example:1883'\n    protocol: mqtt\n    protocolVersion: '5'"));
        assertTrue(doc.contains("  mqtt311:\n    host: 'host.example:1883'\n    protocol: mqtt\n    protocolVersion: '3.1.1'"));
        assertFalse(doc.contains("protocol: ws"));

        // config is read for each request
        config.brokerAddress = " broker.example:2883 ";
        doc = handler.render(mapper, API_ROOT);
        assertEquals(2, count(doc, "host: 'broker.example:2883'"));
    }


    @Test
    public void testWebSocketServers()
    {
        config.webSocketAddress = "broker.example:8083/mqtt";
        var doc = handler.render(mapper, API_ROOT);
        assertTrue(doc.contains("  mqtt5-ws:\n    host: 'broker.example:8083'\n    pathname: '/mqtt'\n    protocol: ws\n    protocolVersion: '5'"));
        assertTrue(doc.contains("  mqtt311-ws:\n    host: 'broker.example:8083'\n    pathname: '/mqtt'\n    protocol: ws\n    protocolVersion: '3.1.1'"));

        config.webSocketAddress = "wss://broker.example";
        doc = handler.render(mapper, API_ROOT);
        assertEquals(2, count(doc, "protocol: wss"));
        assertFalse(doc.contains("pathname"));
    }


    @Test
    public void testRenderHtml()
    {
        var html = handler.renderHtml(mapper, API_ROOT, handler.render(mapper, API_ROOT));

        assertFalse("unfilled placeholder", html.contains("${"));
        assertTrue(html.contains("data-viewer-url=\"" + config.asyncApiViewerUrl + "\""));
        // fallback content
        assertTrue(html.contains("href=\"" + API_ROOT + "/static/css/bootstrap.min.css\""));
        assertTrue(html.contains("href=\"" + API_ROOT + "/asyncapi/mqtt?f=yaml\""));
        assertTrue(html.contains("over TCP at <code>host.example:1883</code>."));
        assertTrue(html.contains("asyncapi: 3.0.0"));
        assertEquals("a &lt;b&gt; &amp; &quot;c&quot;", AsyncApiHandler.escapeHtml("a <b> & \"c\""));

        // WebSocket address, no viewer
        config.webSocketAddress = "broker.example:8083/mqtt";
        config.asyncApiViewerUrl = null;
        html = handler.renderHtml(mapper, API_ROOT, handler.render(mapper, API_ROOT));
        assertTrue(html.contains("data-viewer-url=\"\""));
        assertFalse(html.contains("unpkg"));
        assertTrue(html.contains("and WebSocket at <code>broker.example:8083/mqtt</code>."));
    }


    @Test
    public void testFormatSelection() throws Exception
    {
        assertTrue(AsyncApiHandler.isHtml(null, true));
        assertFalse(AsyncApiHandler.isHtml(null, false));
        assertTrue(AsyncApiHandler.isHtml("html", false));
        assertTrue(AsyncApiHandler.isHtml("text/html", false));
        assertFalse(AsyncApiHandler.isHtml("yaml", true));
        assertFalse(AsyncApiHandler.isHtml("application/asyncapi+yaml", true));
        assertThrows(Exception.class, () -> AsyncApiHandler.isHtml("json", false));
    }


    static int count(String s, String part)
    {
        return s.split(java.util.regex.Pattern.quote(part), -1).length - 1;
    }
}
