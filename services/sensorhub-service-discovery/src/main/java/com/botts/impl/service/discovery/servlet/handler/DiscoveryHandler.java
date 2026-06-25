/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2022 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.service.discovery.servlet.handler;

import com.botts.impl.service.discovery.DiscoveryServlet;
import com.botts.impl.service.discovery.ResourcePermissions;
import com.botts.impl.service.discovery.engine.Constants;
import com.botts.impl.service.discovery.engine.RulesEngine;
import com.botts.impl.service.discovery.servlet.context.RequestContext;
import org.sensorhub.impl.SensorHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Handles requests for data stream discovery, invokes the rules engine and returns the discovered
 * data streams
 *
 * @author Nick Garay
 * @since May 19, 2022
 */
public class DiscoveryHandler extends BaseHandler {

    /**
     * Error message
     */
    private static final String MISSING_QUERY_PARAMETERS_ERROR = "Missing query parameters";

    /**
     *
     */
    private static final String QUERY_PARAMETER = "supporting";

    /**
     * Logger
     */
    private static final Logger logger = LoggerFactory.getLogger(DiscoveryHandler.class);

    /**
     * The valid endpoints for the resource
     */
    public static final String[] names = {"discover"};

    /**
     * The set of permissions
     */
    private final ResourcePermissions permission;

    /**
     * Constructor
     *
     * @param permission The permissions to validate against when processing the request
     */
    public DiscoveryHandler(ResourcePermissions permission) {

        this.permission = permission;
    }

    @Override
    public String[] getNames() {
        return names;
    }

    @Override
    public void doGet(RequestContext context) throws IOException, SecurityException {

        context.checkPermission(permission.read);

        if (context.isEndOfPath()) {

            List<String> ruleIds = extractRuleIds(context);

            executeDiscovery(context, ruleIds);

        } else {

            String ruleId = context.popNextPathElt();

            executeDiscovery(context, List.of(ruleId));
        }
    }

    @Override
    public void doPost(RequestContext context) throws IOException, SecurityException {

        throw new IOException(UNSUPPORTED_OP_ERROR);
    }

    @Override
    public void doPut(RequestContext context) throws IOException, SecurityException {

        throw new IOException(UNSUPPORTED_OP_ERROR);
    }

    @Override
    public void doDelete(RequestContext context) throws IOException, SecurityException {

        throw new IOException(UNSUPPORTED_OP_ERROR);
    }

    /**
     * Parses the request, called if security and permissions allow
     *
     * @param context The request context
     * @throws IOException If there is an issue reading the request or writing a response
     */
    private List<String> extractRuleIds(RequestContext context) throws IOException {

        String optionsString = context.getParameter("supporting");

        List<String> ruleIds;

        if (optionsString == null) {

            throw new IOException(INVALID_RESOURCE_ERROR + ": " +
                    MISSING_QUERY_PARAMETERS_ERROR + " -> " + QUERY_PARAMETER);

        } else {

            ruleIds = List.of(optionsString.split(Constants.CSV_RULE_DELIM));

            logger.debug("conditionsList {}", ruleIds);
        }

        return ruleIds;
    }

    /**
     * Executes the requested discovery query given the ruleIds to apply and sends a corresponding response
     *
     * @param ruleIds The ids of the rules to apply
     * @param context The request context
     * @throws IOException If there is an issue writing a response
     */
    private void executeDiscovery(RequestContext context, List<String> ruleIds) throws IOException {

        RulesEngine.getInstance().setRuleIds(ruleIds);

//        RulesEngine.getInstance().fire();
        DiscoveryServlet discoveryServlet = (DiscoveryServlet)context.getServlet();
        RulesEngine.getInstance().getFilteredResults((SensorHub) discoveryServlet.parentService.getParentHub());

        context.setResponseContentType(APPLICATION_JSON_MIME_TYPE);

        // Writing the message on the web page
        PrintWriter out = context.getWriter();

        String result = RulesEngine.getInstance().getResultSet().toJsonString();

        logger.debug("{}", result);

        out.println(result);
    }
}
