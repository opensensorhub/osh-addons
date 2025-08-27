/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2022 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.service.discovery.engine.rules;

import com.botts.impl.service.discovery.engine.facts.DataStreamFact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * The collection of rules to be used by the {@link com.botts.impl.service.discovery.engine.RulesEngine}
 *
 * @author Nicolas Garay
 * @since 24 Jan 2022
 */
public class Rules {

    /**
     * Logger
     */
    private final Logger logger = LoggerFactory.getLogger(Rules.class);

    /**
     * Mapping of rule id to {@link DataStreamRule}
     */
    private final Map<String, DataStreamRule> rulesMap = new HashMap<>();

    /**
     * Constructor
     */
    public Rules() {

    }

    /**
     * Returns a rule for the give id if available.
     *
     * @param ruleId The id of the rule to lookup
     * @return A rule for the give id if available, null otherwise.
     */
    public DataStreamRule getRule(String ruleId) {

        return rulesMap.get(ruleId);
    }

    /**
     * Adds a rule to the set of rules
     *
     * @param ruleId The id of the rule to lookup
     * @param rule   The rule to be added
     */
    void addRule(String ruleId, DataStreamRule rule) {

        rulesMap.put(ruleId, rule);
    }

    /**
     * Given a rule id, looks up the rule and returns the predicate, or predicate
     * chain, as the condition of the rule
     *
     * @param ruleId The id of the rule to lookup
     * @return The predicate or predicate chain of the rule given by the id
     */
    public Predicate<DataStreamFact> getCondition(String ruleId) {

        Predicate<DataStreamFact> condition = null;

        if (rulesMap.containsKey(ruleId)) {

            condition = rulesMap.get(ruleId).getPredicateTest();
        }

        return condition;
    }
}