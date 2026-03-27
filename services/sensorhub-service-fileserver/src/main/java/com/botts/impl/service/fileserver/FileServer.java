/*******************************************************************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
 Developer are Copyright (C) 2025 the Initial Developer. All Rights Reserved.

 ******************************************************************************/

package com.botts.impl.service.fileserver;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.EnumSet;

import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.util.security.Constraint;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.security.ISecurityManager;
import org.sensorhub.impl.service.AbstractHttpServiceModule;
import org.sensorhub.impl.service.HttpServer;
import org.sensorhub.impl.service.OshLoginService;
import org.vast.util.Asserts;

public class FileServer extends AbstractHttpServiceModule<FileServerConfig> {

    FileServerSecurity security;
    Handler fileServerHandler;

    @Override
    public void setConfiguration(FileServerConfig config) {
        super.setConfiguration(config);
        this.securityHandler = this.security = new FileServerSecurity(this, config.securityConfig.enableAccessControl);
    }

    @Override
    protected void doStart() throws SensorHubException {
        super.doStart();

        HttpServer server = (HttpServer) httpServer;
        Asserts.checkNotNull(config.staticDocsRootUrl);
        Asserts.checkNotNull(config.staticDocsRootDir);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath(config.staticDocsRootUrl);
        context.setResourceBase(config.staticDocsRootDir);
        context.setWelcomeFiles(new String[]{"index.html"});

        SessionHandler sessionHandler = context.getSessionHandler();
        sessionHandler.getSessionCookieConfig().setPath("/");
        sessionHandler.setSessionCookie("OSH_JSESSIONID_ROOT");

        // Security Handler to manage authentication
        ConstraintSecurityHandler mainJettySecurityHandler = (ConstraintSecurityHandler) server.getServletHandler().getSecurityHandler();
        if (config.securityConfig.requireAuth && mainJettySecurityHandler != null) {
            ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler() {
                @Override
                public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
                    String uri = request.getRequestURI();
                    boolean isStatic = uri.startsWith("/_next") || uri.startsWith("/static") ||
                                       uri.contains("/favicon.ico") || uri.contains("/error") ||
                                       uri.contains("/ca-cert") || uri.endsWith(".js") || uri.endsWith(".css");

                    boolean isBridged = OshLoginService.getBridgedUser(request, getParentHub().getSecurityManager()) != null;

                    if (getParentHub().getSecurityManager().isUninitialized() || isStatic || isBridged) {
                        if (getHandler() != null) getHandler().handle(target, baseRequest, request, response);
                    } else {
                        super.handle(target, baseRequest, request, response);
                    }
                }
            };
            securityHandler.setAuthenticator(new org.sensorhub.impl.service.BridgedAuthenticator(mainJettySecurityHandler.getAuthenticator(), getParentHub().getSecurityManager()));
            securityHandler.setLoginService(mainJettySecurityHandler.getLoginService());
            securityHandler.setIdentityService(mainJettySecurityHandler.getIdentityService());

            Constraint constraint = new Constraint();
            constraint.setRoles(new String[]{Constraint.ANY_AUTH});
            constraint.setAuthenticate(true);
            ConstraintMapping cm = new ConstraintMapping();
            cm.setConstraint(constraint);
            cm.setPathSpec("/*");
            securityHandler.addConstraintMapping(cm);

            context.setSecurityHandler(securityHandler);
        }

        // Filter for Redirection and custom Permissions
        context.addFilter(new FilterHolder(new Filter() {
            @Override public void init(FilterConfig filterConfig) throws ServletException {}
            @Override public void destroy() {}
            @Override public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
                HttpServletRequest req = (HttpServletRequest) request;
                HttpServletResponse resp = (HttpServletResponse) response;

                // 1. Mandatory redirect to setup wizard if uninitialized
                if (getParentHub().getSecurityManager().isUninitialized()) {
                    String uri = req.getRequestURI();
                    if (uri.contains("/setup") || uri.contains("/ca-cert") || uri.startsWith("/_next") || uri.startsWith("/static") || uri.startsWith("/api/auth") ||
                        uri.contains("/favicon.ico") || uri.contains("/error") || uri.contains("/PUSH") || uri.contains("/UIDL") ||
                        uri.endsWith(".js") || uri.endsWith(".css") || uri.endsWith(".png") || uri.endsWith(".svg") || uri.endsWith(".json")) {
                        chain.doFilter(request, response);
                    } else {
                        resp.sendRedirect("/sensorhub/setup/");
                    }
                    return;
                }

                // 2. Permission check
                String uri = req.getRequestURI();
                boolean isStatic = uri.startsWith("/_next") || uri.startsWith("/static") ||
                                   uri.contains("/favicon.ico") || uri.contains("/error") ||
                                   uri.contains("/ca-cert") || uri.endsWith(".js") || uri.endsWith(".css");

                if (isStatic) {
                    chain.doFilter(request, response);
                    return;
                }

                String bridgedUser = OshLoginService.getBridgedUser(req, getParentHub().getSecurityManager());
                String user = req.getRemoteUser();
                if (user == null) user = bridgedUser;
                if (user == null) user = ISecurityManager.ANONYMOUS_USER;

                try {
                    security.setCurrentUser(user);
                    boolean permitted = false;
                    try {
                        permitted = security.hasPermission(security.get);
                    } catch (Exception e) {}

                    if (!permitted) {
                        if (req.getRemoteUser() == null && bridgedUser == null) {
                            if (!req.authenticate(resp)) return;
                        } else {
                            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied");
                            return;
                        }
                    }
                    chain.doFilter(request, response);
                } finally {
                    security.clearCurrentUser();
                }
            }
        }), "/*", EnumSet.of(DispatcherType.REQUEST));

        context.addServlet(new ServletHolder("default", org.eclipse.jetty.servlet.DefaultServlet.class), "/");

        fileServerHandler = context;
        fileServerHandler.setServer(server.getJettyServer());
        try {
            fileServerHandler.start();
        } catch (Exception e) {
            throw new SensorHubException("Error starting file server", e);
        }
        server.getHandlers().addHandler(fileServerHandler);
        getLogger().info("Static resources served at {} from {}", config.staticDocsRootUrl, config.staticDocsRootDir);
    }

    @Override
    protected void doStop() throws SensorHubException {
        super.doStop();
        if (httpServer != null && fileServerHandler != null) {
            ((HttpServer)httpServer).getHandlers().removeHandler(fileServerHandler);
        }
    }

    @Override
    public void cleanup() throws SensorHubException {
        super.cleanup();
        if (securityHandler != null)
            securityHandler.unregister();
    }

}
