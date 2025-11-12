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

import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.*;
import org.eclipse.jetty.util.security.Constraint;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.service.AbstractHttpServiceModule;
import org.sensorhub.impl.service.HttpServer;
import org.vast.util.Asserts;

import java.util.Arrays;

public class FileServer extends AbstractHttpServiceModule<FileServerConfig> {

    FileServerSecurity security;
    Handler fileServerHandler;
    HandlerCollection serverHandlers;

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

        // File resource handler
        FileHandler fileResourceHandler = new FileHandler(security, getLogger());
        fileResourceHandler.setDirectoriesListed(false);
        fileResourceHandler.setEtags(true);

        // Context handler
        ContextHandler fileResourceContext = new ContextHandler();
        fileResourceContext.setContextPath(config.staticDocsRootUrl);
        fileResourceContext.setHandler(fileResourceHandler);
        fileResourceContext.setResourceBase(config.staticDocsRootDir);

        serverHandlers = (HandlerCollection) server.getJettyServer().getHandler();

        if (Arrays.stream(serverHandlers.getHandlers()).anyMatch(handler -> handler == fileServerHandler)) {
            reportError("File server handler already registered to Jetty server", new IllegalStateException());
            return;
        }

        ConstraintSecurityHandler jettySecurityHandler = (ConstraintSecurityHandler) server.getServletHandler().getSecurityHandler();

        if (config.securityConfig.requireAuth && jettySecurityHandler != null) {
            ConstraintSecurityHandler fileServerSecurityHandler = createSecurityHandler(fileResourceContext, jettySecurityHandler);
            fileServerHandler = fileServerSecurityHandler;
        } else {
            fileServerHandler = fileResourceContext;
        }

        fileServerHandler.setServer(server.getJettyServer());

        try {
            fileServerHandler.start();
        } catch (Exception e) {
            reportError("Error starting file server", e);
            return;
        }

        serverHandlers.addHandler(fileServerHandler);

        getLogger().info("Static resources being served at {} from {}", config.staticDocsRootUrl, config.staticDocsRootDir);
    }

    @Override
    protected void doStop() throws SensorHubException {
        super.doStop();
        if (serverHandlers != null && fileServerHandler != null) {
            serverHandlers.removeHandler(fileServerHandler);
        }
    }

    @Override
    public void cleanup() throws SensorHubException {
        super.cleanup();
        if (securityHandler != null)
            securityHandler.unregister();
    }

    private void addServletSecurity(ConstraintSecurityHandler securityHandler, ConstraintSecurityHandler jettySecurityHandler) {
        if (securityHandler != null) {
            Constraint constraint = new Constraint();
            constraint.setRoles(new String[]{Constraint.ANY_AUTH});
            constraint.setAuthenticate(config.securityConfig.requireAuth);
            ConstraintMapping cm = new ConstraintMapping();
            cm.setConstraint(constraint);
            cm.setPathSpec(config.staticDocsRootUrl);
            cm.setMethodOmissions(new String[]{"OPTIONS"}); // disable auth on OPTIONS requests (needed for CORS)
            jettySecurityHandler.addConstraintMapping(cm);
            securityHandler.addConstraintMapping(cm);
        }
    }

    private ConstraintSecurityHandler createSecurityHandler(ContextHandler fileResourceContext, ConstraintSecurityHandler jettySecurityHandler) {
        ConstraintSecurityHandler fileSecurityHandler = new ConstraintSecurityHandler();
        fileSecurityHandler.setAuthenticator(jettySecurityHandler.getAuthenticator());
        fileSecurityHandler.setLoginService(jettySecurityHandler.getLoginService());
        fileSecurityHandler.setHandler(fileResourceContext);
        addServletSecurity(fileSecurityHandler, jettySecurityHandler);
        return fileSecurityHandler;
    }

}
