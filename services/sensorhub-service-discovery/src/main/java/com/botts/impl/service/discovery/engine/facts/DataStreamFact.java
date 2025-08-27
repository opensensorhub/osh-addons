/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2022 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.service.discovery.engine.facts;

import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.data.IDataStreamInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * DataStreamFacts wrap instances of {@link IDataStreamInfo} records allowing for the rules
 * based engine to perform rules based queries on data streams.
 *
 * The simplest case of a rules based query stipulates a rule containing logical AND & OR
 * statements comprised of URI ontology definition strings.  This class thus provides
 * a quick lookup of data components within a data streams record data structure by
 * URI definition.
 *
 * @author Nicolas Garay
 * @since 24 Jan 2022
 */
public class DataStreamFact {

    protected final Logger logger = LoggerFactory.getLogger(DataStreamFact.class);

    /**
     * The data stream info object this fact wraps and operates on
     * based on ontological definitions.
     */
    private final IDataStreamInfo dataStreamInfo;

    /**
     * Map of Ontological definition to the corresponding {@link DataComponent} of
     * the {@link IDataStreamInfo} provided.  The data stream info is data record
     * of data records each containing an ontological definition.  They are mapped
     * here in order to perform quick look-ups of metadata within the data record
     * associated with a particular definition.
     */
    private final Map<String, DataComponent> dataComponentMap = new HashMap<>();

    /**
     * The associated system id
     */
    private final String systemId;

    /**
     * The associated data stream id
     */
    private final String dataStreamId;

    /**
     * Constructor
     *
     * @param systemId       The id of the system to which the given data stream belongs or rather
     *                       samples are published for
     * @param dataStreamId   The id of the data stream
     * @param dataStreamInfo The record containing metadata about the data stream
     */
    public DataStreamFact(String systemId, String dataStreamId, IDataStreamInfo dataStreamInfo) {

        logger.debug("SystemId: {} | DataStreamId: {}", systemId, dataStreamId);

        this.systemId = systemId;
        this.dataStreamId = dataStreamId;

        this.dataStreamInfo = dataStreamInfo;

        this.mapDataComponents(dataStreamInfo.getRecordStructure());

        logger.debug("Map Fact Definitions: {}", dataComponentMap.keySet());
    }

    /**
     * Retrieves the associated system id.
     *
     * @return the system id
     */
    public String getSystemId() {

        return systemId;
    }

    /**
     * Retrieves the associated data stream id
     *
     * @return the data stream id
     */
    public String getDataStreamId() {

        return dataStreamId;
    }

    /**
     * Provides access to the wrapped data stream info
     *
     * @return The {@link IDataStreamInfo} instance wrapped by this fact
     */
    public IDataStreamInfo getDataStreamInfo() {

        return dataStreamInfo;
    }

    /**
     * Determines if the fact satisfies the named condition
     *
     * @param condition The condition to be tested
     * @return true if the condition is satisfied, false otherwise
     */
    public boolean satisfies(String condition) {

        boolean satisfies = false;

        for (String definition : dataComponentMap.keySet()) {

            if (definition.equalsIgnoreCase(condition)) {

                satisfies = true;
                break;
            }
        }

        return satisfies;
    }

    /**
     * Get a component, if available, that satisfies the condition, the
     * condition in this case is an ontological definition URI
     *
     * @param condition the condition that is to be satisfied
     * @return null if no component contains the target definition otherwise
     * returns the {@link DataComponent} containing the definition
     */
    public DataComponent getDataComponentSatisfyingCondition(String condition) {

        return dataComponentMap.get(condition);
    }

    /**
     * Creates a mapping of ontological definitions to data components that contain them
     *
     * @param recordStructure The data component to recurse over mining for definitions
     *                        and mapping said definitions to the data component containing
     *                        them
     */
    private void mapDataComponents(DataComponent recordStructure) {

        String definition = recordStructure.getDefinition();

        if (definition != null) {

            dataComponentMap.put(definition, recordStructure);
        }

        for (int idx = 0; idx < recordStructure.getComponentCount(); ++idx) {

            mapDataComponents(recordStructure.getComponent(idx));
        }
    }
    
    /**
     * Returns the Fact's map of DataComponents
     * @return dataComponentMap The Fact's DataComponentMap
     */
    public Map<String, DataComponent> getDataComponentMap() {
        return dataComponentMap;
    }
}
