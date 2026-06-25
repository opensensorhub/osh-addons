/***************************** BEGIN LICENSE BLOCK ***************************
 
 Copyright (C) 2022 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/

package com.botts.impl.service.discovery.engine.visualizations;

import net.opengis.swe.v20.BinaryBlock;
import net.opengis.swe.v20.BinaryEncoding;
import net.opengis.swe.v20.BinaryMember;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.data.IDataStreamInfo;
import org.vast.util.TimeExtent;

import java.util.List;
import java.util.Objects;

/**
 * Defines the structure for results to be returned through the Visualization Discovery API
 *
 * @author Ian Patterson
 * @since August 16, 2022
 */
public class VisualizationRuleResult {
    String dataSourceID;
    String systemID;
    String pathRoot;
    String path;
    String definition;
    String fieldName;
    String phenomenonTimeStart;
    String phenomenonTimeEnd;
    String encoding;
    
    
    VisualizationRuleResult(String dataSourceID, String systemID, DataComponent dataComponent, TimeExtent phenomTimeRange, String fieldName, IDataStreamInfo info) {
        this.dataSourceID = dataSourceID;
        this.systemID = systemID;
        setPhenomenonTimeRange(phenomTimeRange);
        
        this.path = buildPathStringFromLeaf(dataComponent);
        this.pathRoot = findPathRoot();
        this.definition = dataComponent.getDefinition();
        this.fieldName = fieldName;
        if (info.getRecordEncoding() instanceof BinaryEncoding) {
            List<BinaryMember> mbrList = ((BinaryEncoding) info.getRecordEncoding()).getMemberList();
            BinaryBlock binaryBlock = null;
            
            // try to find binary block encoding def in list
            for (BinaryMember spec : mbrList) {
                if (spec instanceof BinaryBlock) {
                    binaryBlock = (BinaryBlock) spec;
                    break;
                }
            }
            
            if (binaryBlock != null) this.encoding = (binaryBlock.getCompression());
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        
        if (!(obj instanceof VisualizationRuleResult)) {
            return false;
        }
        
        VisualizationRuleResult comp = (VisualizationRuleResult) obj;
        
        return Objects.equals(this.dataSourceID, comp.dataSourceID) && Objects.equals(this.path, comp.path) && Objects.equals(this.fieldName, comp.fieldName);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(dataSourceID, path, definition);
    }
    
    private void setPhenomenonTimeRange(TimeExtent timeExtent) {
        if (timeExtent == null) {
            this.phenomenonTimeStart = "0";
            this.phenomenonTimeEnd = "0";
        } else {
            this.phenomenonTimeStart = timeExtent.begin().toString();
            this.phenomenonTimeEnd = timeExtent.end().toString();
        }
    }
    
    private String buildPathStringFromLeaf(DataComponent dcEntry) {
        if (dcEntry.getParent() != null) {
            if (dcEntry.getParent().getParent() == null) {
                return dcEntry.getName();
            } else {
                String parentName = buildPathStringFromLeaf(dcEntry.getParent());
                return parentName + "." + dcEntry.getName();
            }
        } else return dcEntry.getName();
    }
    
    private String findPathRoot() {
        return path.split("\\.")[0];
    }
}
