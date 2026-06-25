/***************************** BEGIN LICENSE BLOCK ***************************
 
 Copyright (C) 2022 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/

package com.botts.impl.service.discovery.servlet.handler;

import com.botts.impl.service.discovery.DiscoveryServlet;
import com.botts.impl.service.discovery.ResourcePermissions;
import com.botts.impl.service.discovery.engine.RulesEngine;
import com.botts.impl.service.discovery.engine.visualizations.VisualizationMapper;
import com.botts.impl.service.discovery.engine.visualizations.VisualizationRuleRelation;
import com.botts.impl.service.discovery.engine.visualizations.VisualizationRuleResult;
import com.botts.impl.service.discovery.servlet.context.RequestContext;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.sensorhub.impl.SensorHub;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/**
 * Handles the requests starting at "visualization" for the Visualization Discovery Service. And defines the dndpoints
 * available to that service.
 *
 * @author Ian Patterson
 * @since July 28, 2022
 */
public class VisualizationHandler extends BaseHandler {
    /**
     * The valid endpoints for the resource
     */
    protected static final String[] names = {"visualizations"};
    /**
     * The set of permissions
     */
    private final ResourcePermissions permission;
    
    public VisualizationHandler(ResourcePermissions permissions) {
        this.permission = permissions;
    }
    
    @Override
    public String[] getNames() {
        return names;
    }
    
    @Override
    public void doGet(RequestContext ctx) throws IOException, SecurityException {
        ctx.checkPermission(permission.read);
        if (ctx.isEndOfPath()) {
            // match against visualizations in JSON file
            showVisualizationList(ctx);
        } else {
            String visualizationID = ctx.popNextPathElt();
            if (ctx.isEndOfPath()) {
                findVisualizationMatch(ctx, visualizationID);
            } else {
                String param = ctx.popNextPathElt();
                if (param.equals("list")) {
                    listSensorsByVizMatch(visualizationID, ctx);
                }
            }
        }
    }
    
    @Override
    public void doPost(RequestContext ctx) throws IOException, SecurityException {
    
    }
    
    @Override
    public void doPut(RequestContext ctx) throws IOException, SecurityException {
    
    }
    
    @Override
    public void doDelete(RequestContext ctx) throws IOException, SecurityException {
    
    }
    
    /**
     * Shows the details of the specifically requested visualization type
     * @param context
     * @param visId
     * @throws IOException
     */
    private void findVisualizationMatch(RequestContext context, String visId) throws IOException {
        VisualizationMapper mapper = VisualizationMapper.getInstance();
        context.setResponseContentType(APPLICATION_JSON_MIME_TYPE);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        VisualizationRuleRelation rule = mapper.findRuleById(visId);
        PrintWriter out = context.getWriter();
        out.println(gson.toJson(rule));
    }
    
    /**
     *  Shows the complete list of supported visualization types
     * @param context
     * @throws IOException
     */
    private void showVisualizationList(RequestContext context) throws IOException {
        VisualizationMapper mapper = VisualizationMapper.getInstance();
        context.setResponseContentType(APPLICATION_JSON_MIME_TYPE);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String result = gson.toJson(mapper.getVisualizationNames());
        PrintWriter out = context.getWriter();
        out.println(result);
    }
    
    /**
     * Generates a response that contains all datastreams that satisfy the conditions necessary to support the
     * requested visualization type
     *
     * @param visualizationId The type of Visualizations (ex. MapMarker, Video, Image, etc.)
     * @param context RequestContext passed through from RootHandler
     * @throws IOException
     */
    private void listSensorsByVizMatch(String visualizationId, RequestContext context) throws IOException {
        VisualizationMapper mapper = VisualizationMapper.getInstance();
        VisualizationRuleRelation rule = mapper.findRuleById(visualizationId);
        List<String> ruleIds = rule.getIncludedRules();
        
        // Rules Engine Interface
        RulesEngine.getInstance().setRuleIds(ruleIds);
        DiscoveryServlet discoveryServlet = (DiscoveryServlet) context.getServlet();
        RulesEngine.getInstance().getFilteredResults((SensorHub) discoveryServlet.parentService.getParentHub());
        
        // Map of Visualization Results (Datastreams that satisfy the conditions)
        Map<String, Map<String, Map<String, VisualizationRuleResult>>> results = mapper.generateMappedResults(rule);
        
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonResult = gson.toJson(results);
        
        PrintWriter out = context.getWriter();
        context.setResponseContentType(APPLICATION_JSON_MIME_TYPE);
        out.println(jsonResult);
    }
}
