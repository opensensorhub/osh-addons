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
    @Test
    public void testRender()
    {
        var mapper = new TopicMapper("node1", TopicMapper.STANDARD_FORMATS);
        var doc = new AsyncApiHandler("3.0.0", null, () -> mapper).render(mapper, "http://host.example:8181/sensorhub/api");

        assertFalse("unfilled placeholder", doc.contains("${"));
        assertTrue(doc.contains("version: 3.0.0"));
        assertTrue(doc.contains("host: host.example:1883"));
        assertTrue(doc.contains("address: 'node1/systems/{systemId}/datastreams/{dataStreamId}/observations:data/{format}'"));
        assertTrue(doc.contains("href: 'http://host.example:8181/sensorhub/api/datastreams'"));
        assertTrue(doc.contains("enum: [geo-json, json, om-json, sml-json, sml-xml, swe-binary, swe-csv, swe-json, swe-xml]"));
        assertTrue(doc.contains("- `swe-csv`: `application/swe+csv`"));
    }


    @Test
    public void testBrokerAddress()
    {
        var mapper = new TopicMapper("node1", TopicMapper.STANDARD_FORMATS);
        var doc = new AsyncApiHandler("3.0.0", "broker.example:2883", () -> mapper).render(mapper, "http://host.example/api");
        assertTrue(doc.contains("host: broker.example:2883"));
    }


    @Test
    public void testRenderHtml()
    {
        var mapper = new TopicMapper("node1", TopicMapper.STANDARD_FORMATS);
        var handler = new AsyncApiHandler("3.0.0", null, () -> mapper);
        var apiRoot = "http://host.example:8181/sensorhub/api";
        var html = handler.renderHtml(mapper, apiRoot, handler.render(mapper, apiRoot));

        assertFalse("unfilled placeholder", html.contains("${"));
        assertTrue(html.contains("unpkg.com/@asyncapi/react-component@" + AsyncApiHandler.VIEWER_VERSION + "'"));
        // fallback content
        assertTrue(html.contains("href=\"" + apiRoot + "/static/css/bootstrap.min.css\""));
        assertTrue(html.contains("href=\"" + apiRoot + "/asyncapi/mqtt?f=yaml\""));
        assertTrue(html.contains("<code>host.example:1883</code>"));
        assertTrue(html.contains("asyncapi: 3.0.0"));
        assertEquals("a &lt;b&gt; &amp; &quot;c&quot;", AsyncApiHandler.escapeHtml("a <b> & \"c\""));
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
}
