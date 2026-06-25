/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2022 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.service.discovery.engine;

import com.botts.impl.service.discovery.engine.facts.DataStreamFact;
import com.botts.impl.service.discovery.engine.results.DataStreamResults;
import com.botts.impl.service.discovery.engine.rules.Rules;
import org.sensorhub.api.data.IDataStreamInfo;
import org.sensorhub.api.datastore.obs.DataStreamKey;
import org.sensorhub.api.datastore.obs.IDataStreamStore;
import org.sensorhub.impl.SensorHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A simple implementation of a Rule's Based Engine, making use of rule files to define
 * rule sets, employing predicate statements as rule conditions to be satisfied.
 * <p>
 * Rules Based Engines are composed of three elements:
 * <li>Rules: comprised of conditions to be met</li>
 * <li>Facts: Knowledge contained in the system</li>
 * <li>Actions: Business logic to execute</li>
 * <p>
 * In our case the rules for predicates or predicate chains of conditions, the facts
 * are {@link org.sensorhub.api.data.IDataStreamInfo} instances decorated for use by the engine
 * and {@link DataStreamResults} used to collect the data streams satisfying the rules.  The
 * addition of a data stream to the data streams result set is our business logic as we do
 * not want to modify knowledge, simply identify knowledge that satisfies the conditions of
 * our rules.
 *
 * @author Nicolas Garay
 * @since 24 Jan 2022
 */
public class RulesEngine {

    private final Logger logger = LoggerFactory.getLogger(RulesEngine.class);

    /**
     * The current set of active rules
     */
    private Rules rules;

    /**
     * The collection of data streams wrapped and decorated as facts to be acted upon by the
     * rules based engine
     */
    private List<DataStreamFact> facts = new ArrayList<>();

    private final Object factsLock = new Object();

    /**
     * A list of the ruleIds
     */
    private List<String> ruleIds = new ArrayList<>();

    /**
     * A container for the results when the engine is fired up
     */
    private DataStreamResults resultSet = new DataStreamResults();

    /**
     * The one and only instance of the rules engine
     */
    private static RulesEngine instance = null;

    /**
     * Constructor
     */
    private RulesEngine() {

    }

    /**
     * Singleton access to the RulesEngine object
     *
     * @return The one and only instance of the rules engine
     */
    public static RulesEngine getInstance() {

        if (instance == null) {

            instance = new RulesEngine();
        }

        return instance;
    }

    /**
     * Sets the rules to be used by the rules engine
     *
     * @param rules The rules to be used by the rules engine
     */
    public void setRules(Rules rules) {

        this.rules = rules;
    }

    /**
     * Adds a fact to the rules engine.
     *
     * @param fact The fact to be added.
     */
    public void addFact(DataStreamFact fact) {

        boolean isDuplicate = false;

        synchronized (factsLock) {

            isDuplicate = facts.stream().anyMatch(dataStreamFact -> dataStreamFact.getSystemId().equalsIgnoreCase(fact.getSystemId()) &&
                    dataStreamFact.getDataStreamId().equalsIgnoreCase(fact.getDataStreamId()));

            if (!isDuplicate) {

                facts.add(fact);
            }
        }
    }

    /**
     * Remove a fact from the rules engine by system and data stream ids
     *
     * @param systemId     The fact's associated system id to be removed.
     * @param dataStreamId The fact's associated data stream id to be removed.
     */
    public void removeFact(String systemId, String dataStreamId) {

        synchronized (factsLock) {

            facts = facts.stream().filter(dataStreamFact ->
                    !dataStreamFact.getSystemId().equalsIgnoreCase(systemId) &&
                            !dataStreamFact.getDataStreamId().equalsIgnoreCase(dataStreamId)).collect(Collectors.toList());
        }
    }

    /**
     * Sets the list of rule ids to be used by the engine
     *
     * @param ruleIds The list of rule ids to be used by the engine
     */
    public void setRuleIds(List<String> ruleIds) {

        this.ruleIds = ruleIds;
    }

    /**
     * Executes the rule based engine on current knowledge with current rules to
     * generate a result set.
     */
    synchronized public void fire() {

        // Clear the result set to not aggregate results across queries
        resultSet.clear();

        List<String> ruleIds = this.ruleIds;

        if (ruleIds != null) {

            for (String ruleId : ruleIds) {

                Predicate<DataStreamFact> condition = rules.getCondition(ruleId);

                if (condition != null) {

                    synchronized (factsLock) {

                        List<DataStreamFact> collect = facts.stream().filter(condition).collect(Collectors.toList());

                        resultSet.addResults(ruleId, collect);
                    }
                }
            }
        }
    }

    /**
     * Returns the current result set
     *
     * @return The current result set
     */
    public DataStreamResults getResultSet() {

        return resultSet;
    }

    /**
     * Resets the rule engine's internal cache
     */
    public void reset() {

        rules = null;

        ruleIds = null;

        facts.clear();

        resultSet.clear();
    }

    /**
     * Gets the current set of rules applied to the engine
     *
     * @return The current set of rules applied to the engine
     */
    public Rules getRules() {

        return rules;
    }
    
    /**
     * Generates a list of facts from supplied hub's Federated DB
     *
     * @return The full list of facts from the hub's Federated DB
     */
    public ArrayList<DataStreamFact> generateCurrentFacts(SensorHub hub){
        ArrayList<DataStreamFact> facts = new ArrayList<>();
        
        IDataStreamStore dsStore = hub.getDatabaseRegistry().getFederatedDatabase().getDataStreamStore();
        for(Map.Entry<DataStreamKey, IDataStreamInfo> dataStreamEntry : dsStore.entrySet()){
            IDataStreamInfo dataStreamInfo = dataStreamEntry.getValue();
            
            String systemId = hub.getIdEncoders().getSystemIdEncoder().encodeID(dataStreamInfo.getSystemID().getInternalID());
            String dataStreamId = hub.getIdEncoders().getDataStreamIdEncoder().encodeID(dataStreamEntry.getKey().getInternalID());
            
            facts.add(new DataStreamFact(systemId, dataStreamId, dataStreamInfo));
        }
        return facts;
    }
    
    public void getFilteredResults(SensorHub hub)  {

        var facts = generateCurrentFacts(hub);
    
        resultSet.clear();
    
        List<String> ruleIds = this.ruleIds;
    
        if (ruleIds != null) {
        
            for (String ruleId : ruleIds) {
                Predicate<DataStreamFact> condition = rules.getCondition(ruleId);
            
                if (condition != null) {
                    List<DataStreamFact> collect = facts.stream().filter(condition).collect(Collectors.toList());
                    resultSet.addResults(ruleId, collect);
                }
            }
        }
    }
}
