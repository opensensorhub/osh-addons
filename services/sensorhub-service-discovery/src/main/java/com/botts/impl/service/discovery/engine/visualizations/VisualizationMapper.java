/***************************** BEGIN LICENSE BLOCK ***************************
 
 Copyright (C) 2022 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/

package com.botts.impl.service.discovery.engine.visualizations;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.data.IDataStreamInfo;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.DataStreamKey;
import org.sensorhub.api.datastore.obs.IDataStreamStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.TimeImpl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Handles parsing the Visualization Rules and Mapping those relationships to Rule Results.
 *
 * @author Ian Patterson
 * @since July 28, 2022
 */
public class VisualizationMapper {
    private String visRelationFilePath;
    private static VisualizationMapper instance = null;
    private List<VisualizationRuleRelation> visualizationRules = null;
    private IDataStreamStore federatedDB = null;
    ISensorHub parentHub = null;
    private static final Logger logger = LoggerFactory.getLogger(VisualizationMapper.class);
    
    private VisualizationMapper() {
    
    }
    
    public static VisualizationMapper getInstance() {
        if (instance == null) {
            instance = new VisualizationMapper();
        }
        
        return instance;
    }
    
    public void setFile(String filePath) {
        visRelationFilePath = filePath;
    }
    
    public void populateVisRelations() {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        
        try(BufferedReader bufferedReader = new BufferedReader(new FileReader(visRelationFilePath))) {
            Type visList = new TypeToken<List<VisualizationRuleRelation>>() {
            }.getType();
            visualizationRules = gson.fromJson(bufferedReader, visList);
            visualizationRules.forEach(VisualizationRuleRelation::setDataComponentClassListFromTypes);
            visualizationRules.forEach(visualizationRuleRelation -> {
                visualizationRuleRelation.setDataComponentClassListFromTypes();
                visualizationRuleRelation.setDataComponentExclusionClassListFromTypes();
            });
        } catch (IOException e) {
            logger.error("Cannot find Configuration File for Visualization Mapper:", e);
        }
        
        
    }
    
    public List<String> getVisualizationNames() {
        if (visualizationRules == null) {
            populateVisRelations();
        }
        
        List<String> names = new ArrayList<>();
        for (VisualizationRuleRelation rel : visualizationRules) {
            names.add(rel.name);
        }
        
        return names;
    }
    
    public void setFederatedDB(IDataStreamStore federatedDB) {
        this.federatedDB = federatedDB;
    }
    
    public void setParentHub(ISensorHub parenthub) {this.parentHub = parenthub;}
    
    public VisualizationRuleRelation findRuleById(String id) {
        return visualizationRules.stream()
                .filter(rule -> id.equalsIgnoreCase(rule.name))
                .findAny()
                .orElse(null);
    }
    
    /**
     * Generate the result map of sensors supported by the visualization relation to the gathered ruleset of Facts
     *
     * @param relation Contains the relationship details needed to map a Visualization to a Datastream
     * @return Sensor Datastream Map with details to be ingested by the client
     */
    public Map<String, Map<String, Map<String, VisualizationRuleResult>>> generateMappedResults(VisualizationRuleRelation relation) {
        // HashSet prevents duplicates but behaves as a regular List when accessed or cast
        var visResults = searchDBForProcedure(relation);
        
        return groupResultsByDatasource(visResults);
    }
    
    /**
     * Generate path from bottom up
     *
     * @param dcEntry
     * @return path needed to connect the datastream to a client  (OSH Client)
     */
    String buildPathStringFromLeaf(DataComponent dcEntry) {
        if (dcEntry.getParent().getParent() == null) {
            return dcEntry.getName();
        } else {
            String parentName = buildPathStringFromLeaf(dcEntry.getParent());
            return parentName + "." + dcEntry.getName();
        }
    }
    
    String determineFieldName(DataComponent dcEntry, VisualizationRuleRelation relation) {
        for (VisualizationRuleRelation.OutputPath path : relation.outputPaths) {
            if (path.acceptedPaths.contains(dcEntry.getName()) || path.acceptedPaths.contains("*")) {
                return path.fieldName;
            }
        }
        return null;
    }
    
    /**
     * Group all results under common Datastream IDs
     * Map is keyed as DataSource->fieldName->pathRoot this way we can better group the fields that are siblings
     * It does cause some friction when searching for more complex compound visualizations
     *
     * @param resultList
     * @return map of Datastream Ids to Visualization Rule Results in a form ingestible by an OSH Client
     */
    Map<String, Map<String, Map<String, VisualizationRuleResult>>> groupResultsByDatasource(List<VisualizationRuleResult> resultList) {
        Map<String, Map<String, Map<String, VisualizationRuleResult>>> resultsGroupedByDSID = new HashMap<>();
        for (VisualizationRuleResult result : resultList) {
    
            Map.Entry<String, VisualizationRuleResult> leafEntry = new AbstractMap.SimpleEntry<>(result.pathRoot, result);
            if (resultsGroupedByDSID.containsKey(result.dataSourceID)) {
                if(resultsGroupedByDSID.get(result.dataSourceID).containsKey(result.fieldName)){
                    // get map at pathroot
                    var tempMap = resultsGroupedByDSID.get(result.dataSourceID).get(result.fieldName);
                    // add  leaf entry to pathRoot Map
                    tempMap.put(leafEntry.getKey(), leafEntry.getValue());
                }else{
                    // create pathRoot map
                    Map<String, VisualizationRuleResult> middleMap = new HashMap<>();
                    // add leaf to PRMap
                    middleMap.put(leafEntry.getKey(), leafEntry.getValue());
                    // add PRMap to top level
                    resultsGroupedByDSID.get(result.dataSourceID).put(result.fieldName, middleMap);
                }

            } else {
                // create inner (fieldName, result)
                Map<String, VisualizationRuleResult> newInnermostMap = new HashMap<>();
                Map<String,Map<String, VisualizationRuleResult>> newMiddleMap = new HashMap<>();
                
                newInnermostMap.put(leafEntry.getKey(), leafEntry.getValue());
                Map.Entry<String,Map<String, VisualizationRuleResult>> newMiddleEntry = new AbstractMap.SimpleEntry<>(result.fieldName, newInnermostMap);
                newMiddleMap.put(newMiddleEntry.getKey(), newMiddleEntry.getValue());
                
                resultsGroupedByDSID.put(result.dataSourceID, newMiddleMap);
            }
        }
        
        return resultsGroupedByDSID;
    }
    
    /**
     * Group all results under common System IDs
     *
     * @param resultList
     * @return map of Datastream Ids to Visualization Rule Results in a form ingestible by an OSH Client
     */
    Map<String, Map<String, VisualizationRuleResult>> groupResultsBySystem(List<VisualizationRuleResult> resultList) {
        Map<String, Map<String, VisualizationRuleResult>> resultsGroupedByDSID = new HashMap<>();
        for (VisualizationRuleResult result : resultList) {
            if (resultsGroupedByDSID.containsKey(result.systemID)) {
                resultsGroupedByDSID.get(result.systemID).put(result.fieldName, result);
            } else {
                resultsGroupedByDSID.put(result.systemID, new HashMap<>());
                resultsGroupedByDSID.get(result.systemID).put(result.fieldName, result);
            }
        }
        
        return resultsGroupedByDSID;
    }
    
    
    public List<VisualizationRuleResult> searchDBForProcedure(VisualizationRuleRelation relation) {
        DataStreamFilter dbFilter = null;
        if (!relation.procedures.isEmpty()) {
            dbFilter = federatedDB.filterBuilder().withObservedProperties(relation.procedures).build();
        } else {
            dbFilter = federatedDB.filterBuilder().build();
        }
        
        Stream<Map.Entry<DataStreamKey, IDataStreamInfo>> streamPipeline = federatedDB.selectEntries(dbFilter);
        
        var dataStreamInfoMap = streamPipeline.collect(Collectors.toMap(e -> (e.getKey().getInternalID()), Map.Entry::getValue));
        
        List<VisualizationRuleResult> visualizationRuleResults = new ArrayList<>();
        
        for (Map.Entry<BigId, IDataStreamInfo> entry : dataStreamInfoMap.entrySet()) {
            
            List<DataComponent> dataComponents = flattenDSOutputs(entry.getValue().getRecordStructure(), relation, 0);
            
            var newResultList = buildResults(entry.getKey(), entry.getValue(), dataComponents, relation, entry.getValue());
            visualizationRuleResults = Stream.concat(visualizationRuleResults.stream(), newResultList.stream()).collect(Collectors.toList());
        }
        
        return visualizationRuleResults;
    }
    
    private List<VisualizationRuleResult> buildResults(BigId dataStreamID, IDataStreamInfo dataStreamInfo, List<DataComponent> dataComponents, VisualizationRuleRelation relation, IDataStreamInfo info) {
        List<VisualizationRuleResult> resultList = new ArrayList<>();
        dataComponents.forEach(dataComponent -> {
            var fieldName = determineFieldName(dataComponent, relation);
            
            if(fieldName != null) {
                var systemID = parentHub.getIdEncoders().getSystemIdEncoder().encodeID(dataStreamInfo.getSystemID().getInternalID());
                var dataStreamId = parentHub.getIdEncoders().getDataStreamIdEncoder().encodeID(dataStreamID);
                
                VisualizationRuleResult newRuleResult = new VisualizationRuleResult(dataStreamId, systemID, dataComponent, dataStreamInfo.getPhenomenonTimeRange(), fieldName, info);
                resultList.add(newRuleResult);
            }
        });
        
        return resultList;
    }
    
    /**
     * Returns a list of all DataComponents, filtered according to the accepted paths, required types and excluded types
     *
     * @param dataComponent the top level DataComponent to be analyzed
     * @param relation      the rule relationship that determines our filtering
     * @param depth         the depth into the datacomponent tree
     * @return
     */
    private List<DataComponent> flattenDSOutputs(DataComponent dataComponent, VisualizationRuleRelation relation, int depth) {
        List<DataComponent> dcList = new ArrayList<>();
        List<String> acceptedPaths = relation.getAcceptedPaths();
        boolean shouldCheckSubs = false;
        
        
        if (!acceptedPaths.isEmpty()) {
            if (relation.classIsAllowed(dataComponent.getClass()) && relation.hasPathMatch(dataComponent.getName())) {
                dcList.add(dataComponent);
            } shouldCheckSubs = true;
        } else {
            if (relation.classIsAllowed(dataComponent.getClass())) {
                dcList.add(dataComponent);
                shouldCheckSubs = true;
            } else shouldCheckSubs = !relation.classIsExplicitlyDisallowed(dataComponent.getClass());
        }
        
        if (shouldCheckSubs) {
            depth += 1;
            for (int i = 0; i < dataComponent.getComponentCount(); i++) {
                var subComponent = dataComponent.getComponent(i);
                if (!(subComponent instanceof TimeImpl)) {
                    dcList = Stream.concat(dcList.stream(), flattenDSOutputs(subComponent, relation, depth).stream()).collect(Collectors.toList());
                }
            }
        }
        return dcList;
    }
}
