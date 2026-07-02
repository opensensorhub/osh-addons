/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2022 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.service.discovery;

import com.botts.impl.service.discovery.servlet.context.RequestContext;
import com.botts.impl.service.discovery.servlet.handler.DiscoveryHandler;
import com.botts.impl.service.discovery.servlet.handler.RootHandler;
import com.botts.impl.service.discovery.servlet.handler.RulesHandler;
import com.botts.impl.service.discovery.servlet.handler.VisualizationHandler;
import org.sensorhub.api.security.ISecurityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Implementation of an {@link HttpServlet} to handle requests for data streams satisfying named rules.
 *
 * @author Nick Garay
 * @since Jan. 7, 2022
 */
public class DiscoveryServlet extends HttpServlet {

    /**
     * Logger to use
     */
    private static final Logger logger = LoggerFactory.getLogger(DiscoveryServlet.class);

    /**
     * Code for a bad request
     */
    private static final int BAD_REQUEST = 400;

    /**
     * Code for a bad request
     */
    private static final int UNAUTHORIZED = 401;

    /**
     * The configuration settings used by the servlet. Not serializable
     */
    final transient DiscoveryServiceConfig config;

    /**
     * The security handler for the servlet, used to check permissions when requests
     * are received. Not serializable
     */
    final transient DiscoveryServiceSecurity securityHandler;

    /**
     * The pool of threads to be used in processing requests
     */
    private final Executor threadPool;

    /**
     * The root handler, all requests are passed to the root handler for processing
     * the root handler then routes it to correct subresource or will throw an exception
     */
    private final RootHandler rootHandler;
    
    /** The Parent DiscoveryService that spawned this servlet
     *
     */
    public DiscoveryService parentService;

    /**
     * Constructor
     *
     * @param service         The parent service for this servlet
     * @param securityHandler The instance of the security handler to invoke in checking
     *                        permissions
     */
    public DiscoveryServlet(DiscoveryService service, DiscoveryServiceSecurity securityHandler) {

        this.config = service.getConfiguration();

        this.securityHandler = securityHandler;

        this.threadPool = service.getThreadPool();

        this.rootHandler = new RootHandler();
        this.rootHandler.addSubResource(new DiscoveryHandler(securityHandler.discoveryPermissions));
        this.rootHandler.addSubResource(new RulesHandler(securityHandler.rulesPermissions));
        this.rootHandler.addSubResource(new VisualizationHandler(securityHandler.visualizationPermissions));
        
        this.parentService = service;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {

        try {

            final AsyncContext asyncContext = request.startAsync();
            final RequestContext requestContext = new RequestContext(this, securityHandler, request, response);

            CompletableFuture.runAsync(() -> {

                try {

                    try {

                        setCurrentUser(request);

                        rootHandler.doGet(requestContext);

                    } catch (IOException e) {

                        logger.error(e.getMessage());

                        try {

                            response.sendError(BAD_REQUEST, e.getMessage());

                        } catch (IOException exception) {

                            logger.error("IOException sending error response", e);
                        }

                    } catch (SecurityException e) {

                        logger.error(e.getMessage());

                        try {

                            response.sendError(UNAUTHORIZED, e.getMessage());

                        } catch (IOException exception) {

                            logger.error("IOException sending error response", e);
                        }
                    }

                } catch (Exception e) {

                    logger.error(e.getMessage());

                } finally {

                    asyncContext.complete();

                    securityHandler.clearCurrentUser();
                }

            }, threadPool);

        } catch (IllegalStateException e) {

            logger.error(e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {

        try {
            AsyncContext asyncContext = request.startAsync();

            final RequestContext requestContext = new RequestContext(this, securityHandler, request, response);

            CompletableFuture.runAsync(() -> {

                try {

                    try {

                        setCurrentUser(request);

                        rootHandler.doPost(requestContext);

                    } catch (IOException e) {

                        logger.error(e.getMessage());

                        try {

                            response.sendError(BAD_REQUEST, e.getMessage());

                        } catch (IOException exception) {

                            logger.error("IOException sending error response", e);
                        }

                    } catch (SecurityException e) {

                        logger.error(e.getMessage());

                        try {

                            response.sendError(UNAUTHORIZED, e.getMessage());

                        } catch (IOException exception) {

                            logger.error("IOException sending error response", e);
                        }
                    }

                } catch (Exception e) {

                    logger.error(e.getMessage());

                } finally {

                    asyncContext.complete();

                    securityHandler.clearCurrentUser();
                }

            }, threadPool);

        } catch (IllegalStateException e) {

            logger.error(e.getMessage());
        }
    }

    /**
     * Stop the servlet
     */
    protected void stop() {

        logger.debug("Servlet stopped");
    }

    /**
     * Sets the current user to the user identified in the request
     *
     * @param req The HTTP request object
     */
    private void setCurrentUser(HttpServletRequest req) {

        String userID = ISecurityManager.ANONYMOUS_USER;

        if (req.getRemoteUser() != null) {

            userID = req.getRemoteUser();
        }

        securityHandler.setCurrentUser(userID);
    }

    /**
     * Returns the configuration for the servlet
     *
     * @return the configuration for the servlet
     */
    public DiscoveryServiceConfig getConfig() {

        return config;
    }
}
