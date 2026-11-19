/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2022 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.service.discovery.servlet.handler;

import com.botts.impl.service.discovery.servlet.context.RequestContext;

import java.io.IOException;

/**
 * Handles requests on the servlet by routing to the specific handler
 *
 * @author Nick Garay
 * @since May 19, 2022
 */
public class RootHandler extends BaseHandler {

    /**
     * Constructor
     */
    public RootHandler() {
    }

    @Override
    public void doGet(RequestContext context) throws IOException, SecurityException {

        if (context.isEndOfPath()) {

            throw new IOException(INVALID_RESOURCE_ERROR + ": " + context.getOriginalPath());

        } else {

            IResourceHandler resource = getSubResource(context);

            resource.doGet(context);
        }
    }

    @Override
    public void doPost(RequestContext ctx) throws IOException, SecurityException {

        IResourceHandler resource = getSubResource(ctx);

        resource.doPost(ctx);
    }

    @Override
    public void doPut(RequestContext ctx) throws IOException, SecurityException {

        IResourceHandler resource = getSubResource(ctx);

        resource.doPut(ctx);
    }

    @Override
    public void doDelete(RequestContext ctx) throws IOException, SecurityException {

        IResourceHandler resource = getSubResource(ctx);

        resource.doDelete(ctx);
    }

    @Override
    public String[] getNames() {

        return new String[0];
    }
}
