/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2022 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.service.discovery.servlet.handler;

import com.botts.impl.service.discovery.servlet.context.RequestContext;

import java.io.IOException;

/**
 * Interface specification for resource handlers
 */
public interface IResourceHandler {

    /**
     * Provides the valid names for endpoints to map to the given handler
     *
     * @return the valid names for endpoints to map to the given handler
     */
    String[] getNames();

    /**
     * Handles GET requests
     *
     * @param ctx The request context
     * @throws IOException       If an IO Exception has occurred
     * @throws SecurityException If current user does not have permissions for the resource and operation
     */
    void doGet(RequestContext ctx) throws IOException, SecurityException;


    /**
     * Handles POST requests
     *
     * @param ctx The request context
     * @throws IOException       If an IO Exception has occurred
     * @throws SecurityException If current user does not have permissions for the resource and operation
     */
    void doPost(RequestContext ctx) throws IOException, SecurityException;


    /**
     * Handles PUT requests
     *
     * @param ctx The request context
     * @throws IOException       If an IO Exception has occurred
     * @throws SecurityException If current user does not have permissions for the resource and operation
     */
    void doPut(RequestContext ctx) throws IOException, SecurityException;


    /**
     * Handles DELETE requests
     *
     * @param ctx The request context
     * @throws IOException       If an IO Exception has occurred
     * @throws SecurityException If current user does not have permissions for the resource and operation
     */
    void doDelete(RequestContext ctx) throws IOException, SecurityException;


    /**
     * Registers a resource handler as a subresource to another resource handler
     *
     * @param resource the resource handler to register
     */
    void addSubResource(IResourceHandler resource);


    /**
     * Registers a resource handler as a subresource to another resource handler
     *
     * @param resource the resource handler to register
     * @param names    the valid names for endpoints to map to the given handler
     */
    void addSubResource(IResourceHandler resource, String... names);
}
