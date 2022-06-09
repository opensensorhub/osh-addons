/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2022 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.hivemq;

import java.io.IOException;
import java.net.InetSocketAddress;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.websocket.api.WebSocketBehavior;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.server.WebSocketServerFactory;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.slf4j.Logger;
import org.vast.util.Asserts;


/**
 * <p>
 * Simple servlet used to expose an "MQTT over websocket" proxy at the desired
 * endpoint.
 * </p>
 *
 * @author Alex Robin
 * @since Jun 9, 2022
 */
@SuppressWarnings("serial")
public class WebSocketProxyServlet extends HttpServlet
{
    final static String MQTT_SUB_PROTOCOL = "mqtt";
    
    final InetSocketAddress mqttServerAddress;
    final Logger log;
    WebSocketServletFactory wsFactory;


    public WebSocketProxyServlet(InetSocketAddress mqttServerAddress, Logger logger)
    {
        this.log = Asserts.checkNotNull(logger, Logger.class);
        this.mqttServerAddress = Asserts.checkNotNull(mqttServerAddress, InetSocketAddress.class);
    }
    
    
    @Override
    public void init(ServletConfig config) throws ServletException
    {
        super.init(config);

        // create websocket factory
        try
        {
            WebSocketPolicy wsPolicy = new WebSocketPolicy(WebSocketBehavior.SERVER);
            wsFactory = new WebSocketServerFactory(getServletContext(), wsPolicy);
            wsFactory.start();
        }
        catch (Exception e)
        {
            throw new ServletException("Cannot initialize websocket factory", e);
        }
    }


    @Override
    public void destroy()
    {
        // destroy websocket factory
        try
        {
            if (wsFactory != null)
                wsFactory.stop();
        }
        catch (Exception e)
        {
            log.error("Cannot stop websocket factory", e);
        }
    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        // check if we have an upgrade request for websockets
        if (wsFactory.isUpgradeRequest(req, resp))
        {
            wsFactory.acceptWebSocket(new WebSocketCreator() {
                @Override
                public Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse resp)
                {
                    try
                    {
                        if (!req.getSubProtocols().contains(MQTT_SUB_PROTOCOL))
                        {
                            resp.sendError(405, "Only 'mqtt' sub-protocol is supported by this endpoint");
                            return null;
                        }
                        
                        resp.setAcceptedSubProtocol(MQTT_SUB_PROTOCOL);
                        return new WebSocketProxy(mqttServerAddress, log);
                    }
                    catch (IOException e)
                    {
                        log.error("Error creating websocket", e);
                        return null;
                    }
                }
            }, req, resp);
            
            return;
        }
        
        resp.sendError(405, "Only websocket requests are accepted on this endpoint");
    }
}
