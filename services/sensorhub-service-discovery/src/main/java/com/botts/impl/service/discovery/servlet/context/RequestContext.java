/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2022 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.service.discovery.servlet.context;

import com.botts.impl.service.discovery.DiscoveryServlet;
import com.google.common.base.Strings;
import org.sensorhub.api.security.IPermission;
import org.sensorhub.impl.module.ModuleSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Class to hold current state of a request on the servlet
 */
public class RequestContext {

    /**
     * Logger
     */
    private static final Logger logger = LoggerFactory.getLogger(RequestContext.class);

    /**
     * The request content type
     */
    private final String requestContentType;

    /**
     * Handle to the servlet
     */
    private final HttpServlet servlet;

    /**
     * Handle to the servlet's security handler
     */
    private final ModuleSecurity securityHandler;

    /**
     * The http request object
     */
    private final HttpServletRequest request;

    /**
     * The http response object
     */
    private final HttpServletResponse response;

    /**
     * The original path of the request
     */
    private final Collection<String> originalPath;

    /**
     * The path's elements, what the servlet will act on as resource identifiers
     */
    private final Deque<String> path;

    /**
     * The map of query parameters received with the request
     */
    private final Map<String, String[]> queryParams;

    /**
     * The content type of the response
     */
    private String responseContentType;

    public RequestContext(HttpServlet servlet, ModuleSecurity securityHandler, HttpServletRequest request, HttpServletResponse response) {

        this.servlet = servlet;
        this.securityHandler = securityHandler;
        this.request = request;
        this.response = response;
        this.path = splitPath(request.getPathInfo());
        this.originalPath = new ArrayList<>(this.path);
        this.queryParams = request.getParameterMap();
        this.requestContentType = request.getContentType();
        this.response.setContentType(this.responseContentType);
    }

    /**
     * Reports if the path has been wholly consumed, thus at a leaf
     *
     * @return true if there are no more elements in the path
     */
    public boolean isEndOfPath() {

        return path.isEmpty();
    }

    /**
     * Returns the next element in the path
     *
     * @return the next element in the path
     */
    public String popNextPathElt() {

        String pathElement = null;

        if (!path.isEmpty()) {

            pathElement = path.pollFirst();
        }

        return pathElement;
    }

    /**
     * Retrieves the writer for the response object, allows servlet to write its response back to the
     * requester
     *
     * @return Handle to the output writer
     * @throws IOException If there is an issue retrieving the writer
     */
    public PrintWriter getWriter() throws IOException {

        return response.getWriter();
    }

    /**
     * Retrieves the reader for the request object, allows servlet to read data provided by
     * requester
     *
     * @return Handle to the input reader
     * @throws IOException If there is an issue retrieving the reader
     */
    public BufferedReader getReader() throws IOException {

        return request.getReader();
    }

    /**
     * Checks a given permission against those registered with the security handle
     *
     * @param permission the permission to validate
     * @throws SecurityException if the current user does not have the requested permission
     */
    public void checkPermission(IPermission permission) throws SecurityException {

        securityHandler.checkPermission(permission);
    }

    /**
     * Content type of the request
     *
     * @return Content type of the request
     */
    public String getRequestContentType() {

        return requestContentType;
    }

    /**
     * The original path of the request
     *
     * @return the original path of the request
     */
    public Collection<String> getOriginalPath() {

        return originalPath;
    }

    public String getFullPath() {

        return request.getPathInfo();
    }

    /**
     * Sets the content type for the response to be written
     *
     * @param responseContentType the content type for the response to be written
     */
    public void setResponseContentType(String responseContentType) {

        this.responseContentType = responseContentType;
    }

    /**
     * Retrieves the specified query parameters values
     *
     * @param parameterId The id of the query parameter
     * @return null if no such parameter is defined, otherwise returns the associated value(s)
     */
    public String getParameter(String parameterId) {

        return queryParams.get(parameterId)[0];
    }

    /**
     * Processes the path into elements in order to identify resources
     *
     * @param path The path received in the request
     * @return A deque array of path elements to be consumed
     */
    private Deque<String> splitPath(String path) {

        Deque<String> pathElements = new ArrayDeque<>();

        if (path != null) {

            String[] elements = path.split("/");

            for (String element : elements) {

                if (!Strings.isNullOrEmpty(element)) {

                    pathElements.add(element);
                }
            }
        }

        return pathElements;
    }

    public HttpServlet getServlet() {

        return servlet;
    }
}
