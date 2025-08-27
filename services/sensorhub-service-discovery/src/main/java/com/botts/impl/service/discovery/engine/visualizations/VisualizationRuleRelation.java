/***************************** BEGIN LICENSE BLOCK ***************************
 
 Copyright (C) 2022 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/

package com.botts.impl.service.discovery.engine.visualizations;

import com.google.gson.annotations.Expose;
import org.vast.data.*;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Creates a relationship from the visualization rules JSON file to interpreted in the Visualization Mapper
 * @author Ian Patterson
 * @since August 1, 2022
 */
public class VisualizationRuleRelation {
    String name;
    List<String> includedRules;
    List<String> excludedRules;
    List<String> tags;
    List<OutputPath> outputPaths;
    List<String> procedures;
    List<String> types;
    List<String> excludedTypes;
    List<Class<? extends AbstractDataComponentImpl>> dataComponentClassList = new ArrayList<>();
    List<Class<? extends AbstractDataComponentImpl>> dataComponentExclusionClassList = new ArrayList<>();
    
    @Expose(serialize = false, deserialize = false)
    private static final Map<String, Class<? extends AbstractDataComponentImpl>> classMap = Map.ofEntries(
            new AbstractMap.SimpleEntry<String, Class<? extends AbstractDataComponentImpl>>("time", TimeImpl.class),
            new AbstractMap.SimpleEntry<String, Class<? extends AbstractDataComponentImpl>>("boolean", BooleanImpl.class),
            new AbstractMap.SimpleEntry<String, Class<? extends AbstractDataComponentImpl>>("count", CountImpl.class),
            new AbstractMap.SimpleEntry<String, Class<? extends AbstractDataComponentImpl>>("quantity", QuantityImpl.class),
            new AbstractMap.SimpleEntry<String, Class<? extends AbstractDataComponentImpl>>("text", TextImpl.class),
            new AbstractMap.SimpleEntry<String, Class<? extends AbstractDataComponentImpl>>("vector", VectorImpl.class),
            new AbstractMap.SimpleEntry<String, Class<? extends AbstractDataComponentImpl>>("data_array", DataArrayImpl.class),
            new AbstractMap.SimpleEntry<String, Class<? extends AbstractDataComponentImpl>>("data_record", DataRecordImpl.class)
    );
    
    class OutputPath {
        String fieldName;
        List<String> acceptedPaths;
    }
    
    public List<String> getIncludedRules() {
        return includedRules;
    }
    
    public List<String> getExcludedRules() {
        return excludedRules;
    }
    
    public void getRules(){
        // Should return a list of included rules, less the excluded ones
        // except in cases where there are no included rules, then return ALL rules except excludes
    }
    
    public void setDataComponentClassListFromTypes(){
        if(types != null) {
            this.types.forEach(type -> {
                if (classMap.containsKey(type.toLowerCase())) {
                    this.dataComponentClassList.add(classMap.get(type.toLowerCase()));
                }
            });
        }
    }
    
    public List<Class<? extends AbstractDataComponentImpl>> getDataComponentClassList() {
        if(dataComponentClassList.isEmpty()) setDataComponentClassListFromTypes();
        return dataComponentClassList;
    }
    
    public void setDataComponentExclusionClassListFromTypes(){
        if(excludedTypes != null) {
            this.excludedTypes.forEach(type -> {
                if (classMap.containsKey(type.toLowerCase())) {
                    this.dataComponentExclusionClassList.add(classMap.get(type.toLowerCase()));
                }
            });
        }
    }
    
    public List<Class<? extends AbstractDataComponentImpl>> getDataComponentExclusionClassList() {
        if (dataComponentExclusionClassList.isEmpty()) setDataComponentExclusionClassListFromTypes();
        return dataComponentExclusionClassList;
    }
    
    public String isAcceptedPath(String pathName){
        for(OutputPath path:outputPaths){
            for(String acceptedPathName:path.acceptedPaths){
                if(pathName.equals(acceptedPathName)){
                    return path.fieldName;
                }
            }
        }
        return null;
    }
    
    public List<String> getAcceptedPaths(){
        List<String> pathList = new ArrayList<>();
       for(OutputPath outputPath: outputPaths) {
            pathList = Stream.concat(pathList.stream(), outputPath.acceptedPaths.stream()).collect(Collectors.toList());
        }
        return pathList;
    }
    
    public boolean classIsAllowed(Class<?> className){
        return dataComponentClassList.contains(className);
    }
    
    public boolean classIsExplicitlyDisallowed(Class<?> className){
        return dataComponentExclusionClassList.contains(className);
    }
    
    public boolean hasPathMatch(String path){
        return getAcceptedPaths().contains("*") || getAcceptedPaths().contains(path);
    }
}
