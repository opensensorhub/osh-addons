/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2022 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.service.discovery.servlet.handler;

import com.botts.impl.service.discovery.servlet.context.RequestContext;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract request handler
 *
 * @author Nick Garay
 * @since May 19, 2022
 */
public abstract class BaseHandler implements IResourceHandler {

    protected static final String UNSUPPORTED_OP_ERROR = "Unsupported operation on resource";

    protected static final String INVALID_RESOURCE_ERROR = "Invalid resource";

    protected static final String PLAIN_TEXT_MIME_TYPE = "text/plain";
    protected static final String APPLICATION_JSON_MIME_TYPE = "application/json";
    protected static final String TEXT_HTML_MIME_TYPE = "text/html";

    /**
     * The map of resource handlers
     * <p>
     * Each handler is mapped to its endpoint id
     */
    private final Map<String, IResourceHandler> subResources = new HashMap<>();

    /**
     * Constructor
     */
    public BaseHandler() {

        super();
    }

    @Override
    public void addSubResource(IResourceHandler handler) {

        addSubResource(handler, handler.getNames());
    }

    @Override
    public void addSubResource(IResourceHandler handler, String... names) {

        for (String name : names) {

            subResources.put(name, handler);
        }
    }

    /**
     * Retrieves a sub resource handler by using the current context to identify the
     * handler to return
     *
     * @param ctx A context object holding the information for the current request being processed
     * @return A resource handler for the resource retrieved from the request context
     * @throws IOException if the resource is not found or invalid
     */
    protected IResourceHandler getSubResource(RequestContext ctx) throws IOException {

        if (ctx == null || ctx.isEndOfPath()) {

            throw new IOException("Missing resource name");
        }

        String resourceName = ctx.popNextPathElt();

        IResourceHandler resource = subResources.get(resourceName);

        if (resource == null) {

            throw new IOException("Invalid resource name: '" + resourceName + "'");
        }

        return resource;
    }
}
