/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2022 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.georobotix.ai.impl.rulesengine.results;

import com.georobotix.ai.impl.rulesengine.facts.DataStreamFact;
import com.georobotix.ai.impl.rulesengine.rules.DataStreamRule;
import com.georobotix.ai.impl.rulesengine.rules.Rules;
import com.google.gson.stream.JsonWriter;
import net.opengis.swe.v20.DataComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Nicolas Garay
 * @since 24 Jan 2022
 */
public class DataStreamResults {

    private final Logger logger = LoggerFactory.getLogger(DataStreamResults.class);

    Map<String, List<String>> systemToRulesMap = new HashMap<>();
    
    Map<String, List<DataStreamFact>> rulesToDataStreamsMap = new HashMap<>();

    /**
     * Constructor
     */
    public DataStreamResults() {
    }

    /**
     * Adds results from a query to the set of results
     *
     * @param ruleId     The id of the rule fow which the results are being added
     * @param resultList The list of {@link DataStreamFact}s satisfying the rule
     */
    public void addResults(String ruleId, List<DataStreamFact> resultList) {

        // Find the mapping from system id to rule id
        for (DataStreamFact dataStreamFact : resultList) {

            // If the map does not contain the correlation
            if (!systemToRulesMap.containsKey(dataStreamFact.getSystemId())) {

                // Add a new entry for the correlation
                systemToRulesMap.put(dataStreamFact.getSystemId(), new ArrayList<>());
            }

            boolean mapHasRule = systemToRulesMap.get(dataStreamFact.getSystemId())
                        .stream().anyMatch(currentRule -> currentRule.equalsIgnoreCase(ruleId));

            if (!mapHasRule) {

                // Add the rule id mapping to the system id
                systemToRulesMap.get(dataStreamFact.getSystemId()).add(ruleId);
            }
        }

        // If the map does not contain the correlation of rule id to data stream info
        if (!rulesToDataStreamsMap.containsKey(ruleId)) {

            // Add a new entry for the correlation
            rulesToDataStreamsMap.put(ruleId, new ArrayList<>());
        }

        // Add the data stream info mapping the rule id
        rulesToDataStreamsMap.get(ruleId).addAll(resultList);
    }

    /**
     * Clears internal maps and cache
     */
    public void clear() {

        systemToRulesMap.clear();
        rulesToDataStreamsMap.clear();
    }

    /**
     * Generates a JSON string containing the information necessary for clients to
     * operate on the data stream when it is requested.  Currently, provides a mapping
     * of SystemIds to rules satisfied, including how they are satisfied and the pertinent
     * field information from the {@link org.sensorhub.api.data.IDataStreamInfo}.
     * <p>
     * Example:
     * <p>
     * {
     *  "resultSet": [
     *      {
     *          "systemId": "41s2wfnr",
     *          "location": [
     *              {
     *                  "dataStreamId": "8wl0jfwp",
     *                  "paths": [
     *                      {
     *                          "name": "location",
     *                          "definition": "http://www.opengis.net/def/property/OGC/0/SensorLocation",
     *                          "path": "location"
     *                      },
     *                      {
     *                          "name": "lat",
     *                          "definition": "http://sensorml.com/ont/swe/property/GeodeticLatitude",
     *                          "path": "location.lat"
     *                      },
     *                      {
     *                          "name": "lon",
     *                          "definition": "http://sensorml.com/ont/swe/property/Longitude",
     *                          "path": "location.lon"
     *                      }
     *                  ]
     *              }
     *          ],
     *          "orientation": [
     *              {
     *                  "dataStreamId": "7tpnywjt",
     *                  "paths": [
     *                      {
     *                          "name": "attitude",
     *                          "definition": "http://www.opengis.net/def/property/OGC/0/PlatformOrientation",
     *                          "path": "attitude"
     *                      }
     *                  ]
     *              }
     *          ]
     *      }
     *  ]
     * }
     *
     * @return A JSON string representing the result set per system per rule
     * @throws IOException if there is a failure in creating the JSON string
     */
    public String toJsonString(Rules rules) throws IOException {

        StringWriter stringWriter = new StringWriter();

        JsonWriter writer = new JsonWriter(stringWriter);

        writer.beginObject();

        writer.name("resultSet");

        writer.beginArray();

        for (Map.Entry<String, List<String>> entry : systemToRulesMap.entrySet()) {

            writer.beginObject();

            writer.name("systemId").value(entry.getKey());

            for (String ruleId : entry.getValue()) {

                writer.name(ruleId);

                writer.beginArray();

                for (DataStreamFact fact : rulesToDataStreamsMap.get(ruleId)) {

                    if (fact.getSystemId().equalsIgnoreCase(entry.getKey())) {

                        writer.beginObject();

                        writer.name("dataStreamId").value(fact.getDataStreamId());

                        DataStreamRule rule = rules.getRule(ruleId);

                        List<String> targets = rule.getTargets();

                        writer.name("paths");

                        writer.beginArray();

                        for (String targetCondition : targets) {

                            DataComponent component = fact.getDataComponentSatisfyingCondition(targetCondition);

                            if (component != null) {

                                writer.beginObject();

                                writer.name("name").value(component.getName());

                                writer.name("definition").value(component.getDefinition());

                                StringBuilder path = new StringBuilder(component.getName());

                                component = component.getParent();

                                while (component != null) {

                                    path.insert(0, component.getName() + ".");

                                    component = component.getParent();
                                }

                                writer.name("path").value(path.toString());

                                writer.endObject();
                            }
                        }

                        writer.endArray();

                        writer.endObject();
                    }
                }

                writer.endArray();
            }
            writer.endObject();
        }

        writer.endArray();

        writer.endObject();

        return stringWriter.toString();
    }
    
    public Map<String, List<DataStreamFact>> getRulesToDataStreamsMap() {
        return rulesToDataStreamsMap;
    }
}
