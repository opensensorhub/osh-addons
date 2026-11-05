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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.sensorhub.impl.service.consys.InvalidRequestException;
import org.sensorhub.impl.service.consys.ServiceErrors;
import org.sensorhub.impl.service.consys.resource.IResourceHandler;
import org.sensorhub.impl.service.consys.resource.RequestContext;
import org.sensorhub.impl.service.consys.resource.ResourceFormat;


/**
 * <p>
 * Serves the AsyncAPI 3.0 description of the MQTT binding at
 * {@code {apiRoot}/asyncapi/mqtt}, as a service description of
 * OGC API - Publish-Subscribe. The document is a template filled in with the
 * node ID, the API root, the broker host and the format names.
 * </p><p>
 * The response is YAML, or an HTML page that shows the YAML for {@code f=html}
 * and for browsers. The HTML page uses the same layout and static files as
 * the CS API HTML pages.
 * </p>
 */
class AsyncApiHandler implements IResourceHandler
{
    static final String NAME = "asyncapi";
    static final String BINDING = "mqtt";
    static final String MEDIA_TYPE = "application/asyncapi+yaml";
    static final String SHORT_YAML = "yaml";
    static final String TEMPLATE = "asyncapi-mqtt.yaml";
    static final String HTML_TEMPLATE = "asyncapi.html";
    static final int DEFAULT_MQTT_PORT = 1883;
    static final String[][] MQTT_VERSIONS = {{"5", "MQTT 5"}, {"3.1.1", "MQTT 3.1.1"}};

    final String template;
    final String htmlTemplate;
    final String version;
    final Supplier<ConSysApiMqttServiceConfig> config;
    final Supplier<TopicMapper> topics;


    /**
     * @param version the module version
     * @param config the module config, read for each request
     * @param topics the active topic mapper, or null when the service is stopped
     */
    AsyncApiHandler(String version, Supplier<ConSysApiMqttServiceConfig> config, Supplier<TopicMapper> topics)
    {
        this.template = readResource(TEMPLATE);
        this.htmlTemplate = readResource(HTML_TEMPLATE);
        this.version = version != null ? version : "unknown";
        this.config = config;
        this.topics = topics;
    }


    static String readResource(String name)
    {
        try (InputStream is = AsyncApiHandler.class.getResourceAsStream(name))
        {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("Cannot read " + name, e);
        }
    }


    @Override
    public String[] getNames()
    {
        return new String[] {NAME};
    }


    @Override
    public void doGet(RequestContext ctx) throws InvalidRequestException, IOException, SecurityException
    {
        var mapper = topics.get();
        if (ctx.isEndOfPath() || !BINDING.equals(ctx.popNextPathElt()) || !ctx.isEndOfPath() || mapper == null)
            throw ServiceErrors.notFound();

        var html = isHtml(ctx.getParameter("f"), ctx.isBrowserHtmlRequest());
        var doc = render(mapper, ctx.getApiRootURL());
        if (html)
            doc = renderHtml(mapper, ctx.getApiRootURL(), doc);

        ctx.setResponseContentType(html ? ResourceFormat.HTML.getMimeType() : MEDIA_TYPE);
        ctx.getOutputStream().write(doc.getBytes(StandardCharsets.UTF_8));
    }


    /**
     * @return true for f=html, or for a browser when there is no f parameter;
     *         false for f=yaml or the AsyncAPI YAML media type
     */
    static boolean isHtml(String f, boolean isBrowser) throws InvalidRequestException
    {
        if (f == null)
            return isBrowser;
        if (ResourceFormat.SHORT_HTML.equals(f) || ResourceFormat.HTML.getMimeType().equals(f))
            return true;
        if (SHORT_YAML.equals(f) || MEDIA_TYPE.equals(f))
            return false;
        throw ServiceErrors.unsupportedFormat(f);
    }


    String render(TopicMapper mapper, String apiRoot)
    {
        var formatList = mapper.formats.entrySet().stream()
            .map(e -> "- `" + e.getKey() + "`: `" + e.getValue() + "`")
            .collect(Collectors.joining("\n    "));

        return template
            .replace("${nodeId}", nodeId(mapper))
            .replace("${apiRoot}", apiRoot)
            .replace("${version}", version)
            .replace("${servers}", servers(apiRoot))
            .replace("${formatEnum}", String.join(", ", mapper.formats.keySet()))
            .replace("${formatList}", formatList);
    }


    /**
     * HTML page that shows the document with the AsyncAPI viewer from
     * unpkg.com. If the viewer cannot load, the page shows the YAML text
     * with the CS API page style.
     */
    String renderHtml(TopicMapper mapper, String apiRoot, String doc)
    {
        return htmlTemplate
            .replace("${viewerUrl}", escapeHtml(trim(config.get().asyncApiViewerUrl)))
            .replace("${apiRoot}", escapeHtml(apiRoot))
            .replace("${yamlUrl}", escapeHtml(apiRoot + "/" + NAME + "/" + BINDING + "?f=" + SHORT_YAML))
            .replace("${brokerList}", brokerList(apiRoot))
            .replace("${nodeId}", escapeHtml(nodeId(mapper)))
            .replace("${doc}", escapeHtml(doc));
    }


    static String nodeId(TopicMapper mapper)
    {
        var prefix = mapper.getPrefix();
        return prefix.substring(0, prefix.length() - 1);
    }


    /** The TCP broker address from the config, or the HTTP host and the default MQTT port */
    String brokerHost(String apiRoot)
    {
        var address = trim(config.get().brokerAddress);
        if (!address.isEmpty())
            return address;
        var host = URI.create(apiRoot).getHost();
        return (host != null ? host : "localhost") + ":" + DEFAULT_MQTT_PORT;
    }


    /** HTML list of the advertised broker addresses */
    String brokerList(String apiRoot)
    {
        var list = "TCP at <code>" + escapeHtml(brokerHost(apiRoot)) + "</code>";
        var ws = trim(config.get().webSocketAddress);
        if (!ws.isEmpty())
            list += " and WebSocket at <code>" + escapeHtml(ws) + "</code>";
        return list;
    }


    /** Server Objects: TCP, and WebSocket if the config has a WebSocket address */
    String servers(String apiRoot)
    {
        var sb = new StringBuilder();
        var tcpHost = brokerHost(apiRoot);
        for (var v: MQTT_VERSIONS)
            appendServer(sb, "mqtt" + v[0].replace(".", ""), tcpHost, null, "mqtt", v, "TCP");

        var ws = trim(config.get().webSocketAddress);
        if (!ws.isEmpty())
        {
            var protocol = "ws";
            if (ws.startsWith("wss://"))
                protocol = "wss";
            ws = ws.replaceFirst("^wss?://", "");
            var slash = ws.indexOf('/');
            var host = slash < 0 ? ws : ws.substring(0, slash);
            var path = slash < 0 ? null : ws.substring(slash);
            for (var v: MQTT_VERSIONS)
                appendServer(sb, "mqtt" + v[0].replace(".", "") + "-" + protocol, host, path, protocol, v, protocol.equals("wss") ? "secure WebSocket" : "WebSocket");
        }
        return sb.toString();
    }


    static void appendServer(StringBuilder sb, String name, String host, String path, String protocol, String[] mqttVersion, String transport)
    {
        sb.append("  ").append(name).append(":\n")
          .append("    host: '").append(host.replace("'", "''")).append("'\n");
        if (path != null)
            sb.append("    pathname: '").append(path.replace("'", "''")).append("'\n");
        sb.append("    protocol: ").append(protocol).append('\n')
          .append("    protocolVersion: '").append(mqttVersion[0]).append("'\n")
          .append("    description: ").append(mqttVersion[1]).append(" over ").append(transport).append('.');
        if ("5".equals(mqttVersion[0]))
            sb.append(" Rejections carry a reason code and reason string.");
        sb.append('\n');
    }


    static String trim(String s)
    {
        return s != null ? s.trim() : "";
    }


    static String escapeHtml(String s)
    {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }


    @Override
    public void doPost(RequestContext ctx) throws InvalidRequestException
    {
        throw ServiceErrors.unsupportedOperation("Read-only resource");
    }


    @Override
    public void doPut(RequestContext ctx) throws InvalidRequestException
    {
        throw ServiceErrors.unsupportedOperation("Read-only resource");
    }


    @Override
    public void doDelete(RequestContext ctx) throws InvalidRequestException
    {
        throw ServiceErrors.unsupportedOperation("Read-only resource");
    }


    @Override
    public void addSubResource(IResourceHandler resource)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public void addSubResource(IResourceHandler resource, String... names)
    {
        throw new UnsupportedOperationException();
    }
}
