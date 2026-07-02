/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2022 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.georobotix.ai.impl.rulesengine;

import com.georobotix.ai.impl.rulesengine.facts.DataStreamFact;
import com.georobotix.ai.impl.rulesengine.results.DataStreamResults;
import com.georobotix.ai.impl.rulesengine.rules.Rules;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.data.DataStreamAddedEvent;
import org.sensorhub.api.data.DataStreamEvent;
import org.sensorhub.api.data.DataStreamRemovedEvent;
import org.sensorhub.api.data.IDataStreamInfo;
import org.sensorhub.api.datastore.obs.DataStreamKey;
import org.sensorhub.api.datastore.obs.IDataStreamStore;
import org.sensorhub.api.event.EventUtils;
import org.sensorhub.api.system.SystemEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Flow;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A simple implementation of a Rule's Based Engine, making use of rule files to define
 * rule sets, using predicate statements as rule conditions to be satisfied.
 * <p>
 * Rules Engines are composed of three elements:
 * <li>Rules: comprising conditions to be met</li>
 * <li>Facts: Knowledge contained in the system</li>
 * <li>Actions: Business logic to execute</li>
 * <p>
 * In our case the rules for predicates or predicate chains of conditions, the facts
 * are {@link org.sensorhub.api.data.IDataStreamInfo} instances decorated for use by the engine
 * and {@link DataStreamResults} used to collect the data streams satisfying the rules.  The
 * addition of a data stream to the data streams result set is our business logic as we do
 * not want to modify knowledge, identify knowledge that satisfies the conditions of
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
     * rule engine
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
    private final DataStreamResults resultSet = new DataStreamResults();

    /**
     * Subscription used by the service to be notified in changes to the available data streams
     */
    private Flow.Subscription dataStreamSubscription = null;

    /**
     * Constructs a RulesEngine instance using a specified sensor hub. This constructor
     * initializes the event bus subscription to listen for system events, particularly
     * data stream events, and performs actions based on the event type (e.g., adding
     * or removing facts from the knowledge base).
     *
     * @param sensorHub The sensor hub instance used to initialize the RulesEngine. It
     *                  provides access to the event bus and database registry for handling
     *                  data stream events.
     */
    public RulesEngine(ISensorHub sensorHub) {

        if (sensorHub != null) {

            initializeFacts(sensorHub);

            // Subscribe to system events, and then look for specific data stream events
            // subscription cancellation occurs in doStop
            sensorHub.getEventBus().newSubscription(SystemEvent.class)
                    .withTopicID(EventUtils.getSystemRegistryTopicID())
                    .subscribe(new Flow.Subscriber<>() {
                        @Override
                        public void onSubscribe(Flow.Subscription subscription) {

                            dataStreamSubscription = subscription;
                            dataStreamSubscription.request(Long.MAX_VALUE);
                        }

                        @Override
                        public void onNext(SystemEvent systemEvent) {

                            logger.debug("SystemEvent received");

                            if (systemEvent instanceof DataStreamEvent dataStreamEvent) {

                                logger.debug("SystemEvent is DataStreamEvent");

                                IDataStreamStore dataStreamStore = sensorHub.getDatabaseRegistry().getFederatedDatabase().getDataStreamStore();

                                Map.Entry<DataStreamKey, IDataStreamInfo> dataStreamEntry = dataStreamStore.getLatestVersionEntry(dataStreamEvent.getSystemUID(), dataStreamEvent.getOutputName());

                                IDataStreamInfo dataStreamInfo = dataStreamEntry.getValue();
                                String systemId = sensorHub.getIdEncoders().getSystemIdEncoder().encodeID(dataStreamInfo.getSystemID().getInternalID());
                                String dataStreamId = sensorHub.getIdEncoders().getDataStreamIdEncoder().encodeID(dataStreamEntry.getKey().getInternalID());

                                if (dataStreamEvent instanceof DataStreamAddedEvent) {

                                    logger.debug("Adding new fact to knowledge base");
                                    addFact(new DataStreamFact(systemId, dataStreamId, dataStreamInfo));

                                } else if (dataStreamEvent instanceof DataStreamRemovedEvent) {

                                    logger.debug("Removing fact from knowledge base");
                                    removeFact(systemId, dataStreamId);
                                }
                            }
                        }

                        @Override
                        public void onError(Throwable throwable) {

                            logger.error("Error handling data stream subscriptions: {}", throwable.getMessage());
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        }
    }

    /**
     * Sets the rules to be used by the rule engine
     *
     * @param rules The rules to be used by the rule engine
     */
    public void setRules(Rules rules) {

        this.rules = rules;
    }

    /**
     * Adds a fact to the rule engine.
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
     * Remove a fact from the rule engine by system and data stream ids
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
    public void setTargetRuleIds(List<String> ruleIds) {

        this.ruleIds = ruleIds;
    }

    /**
     * Executes the rule engine on current knowledge with current rules to
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
     * Generates current facts based on the data streams available in the sensor hub's
     * federated database and adds them to the knowledge base. This method processes
     * each data stream in the database registry, encodes their system and data stream IDs,
     * and creates corresponding {@link DataStreamFact} objects.
     *
     * @param hub The sensor hub instance providing access to the database registry
     *            and ID encoders. It is used to iterate over the data streams and
     *            generate the necessary facts.
     */
    private void initializeFacts(ISensorHub hub) {

        IDataStreamStore dsStore = hub.getDatabaseRegistry().getFederatedDatabase().getDataStreamStore();
        for (Map.Entry<DataStreamKey, IDataStreamInfo> dataStreamEntry : dsStore.entrySet()) {
            IDataStreamInfo dataStreamInfo = dataStreamEntry.getValue();

            String systemId = hub.getIdEncoders().getSystemIdEncoder().encodeID(dataStreamInfo.getSystemID().getInternalID());
            String dataStreamId = hub.getIdEncoders().getDataStreamIdEncoder().encodeID(dataStreamEntry.getKey().getInternalID());

            facts.add(new DataStreamFact(systemId, dataStreamId, dataStreamInfo));
        }
    }

    /**
     * Performs cleanup operations for the RulesEngine instance.
     * Specifically, it cancels the active data stream subscription if it exists,
     * to release resources and stop receiving updates.
     */
    public void cleanup() {

        if (dataStreamSubscription != null) {

            dataStreamSubscription.cancel();
        }
    }
}
