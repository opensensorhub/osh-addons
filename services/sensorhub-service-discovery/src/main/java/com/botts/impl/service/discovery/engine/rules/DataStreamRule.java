/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2022 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.service.discovery.engine.rules;

import com.botts.impl.service.discovery.engine.Constants;
import com.botts.impl.service.discovery.engine.facts.DataStreamFact;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Class that encapsulates a rule applied to data streams.  In this case the rule is composed of a
 * rule id and a list of target conditions.  When the rule is instantiated the target conditions are
 * "compiled" into a predicate, or predicate chain, to evaluate data streams against the rule.
 *
 * @author Nicolas Garay
 * @since 24 Jan 2022
 */
public class DataStreamRule {

    /**
     * The rule's id
     */
    private final String ruleId;

    /**
     * The list of target conditions
     */
    private final ArrayList<String> targets = new ArrayList<>();

    /**
     * The predicate, or predicate chain, constructed from the target conditions
     */
    private final Predicate<DataStreamFact> predicate;

    /**
     * Constructor
     *
     * @param ruleId     The rule's id
     * @param conditions The list of target conditions
     */
    public DataStreamRule(String ruleId, String[] conditions) {

        this.ruleId = ruleId;

        int tokenIdx = 0;

        Predicate<DataStreamFact> tempPredicate = null;

        do {

            final String token = conditions[tokenIdx++];

            if (tempPredicate != null) {

                if (token.equalsIgnoreCase(Constants.LOGICAL_AND) || token.equalsIgnoreCase(Constants.LOGICAL_AND_SYM)) {

                    final String rightOperand = conditions[tokenIdx++];
                    targets.add(rightOperand);
                    tempPredicate = tempPredicate.and(fact -> fact.satisfies(rightOperand));

                } else if (token.equalsIgnoreCase(Constants.LOGICAL_OR) || token.equalsIgnoreCase(Constants.LOGICAL_OR_SYM)) {

                    final String rightOperand = conditions[tokenIdx++];
                    targets.add(rightOperand);
                    tempPredicate = tempPredicate.or(fact -> fact.satisfies(rightOperand));

                } else if (token.equalsIgnoreCase(Constants.LOGICAL_NOT) || token.equalsIgnoreCase(Constants.LOGICAL_NOT_SYM)) {

                    final String rightOperand = conditions[tokenIdx++];
                    targets.add(rightOperand);
                    tempPredicate = tempPredicate.and(fact -> !fact.satisfies(rightOperand));
                }

            } else if (!token.equalsIgnoreCase(Constants.LOGICAL_OR) &&
                    !token.equalsIgnoreCase(Constants.LOGICAL_OR_SYM) &&
                    !token.equalsIgnoreCase(Constants.LOGICAL_AND) &&
                    !token.equalsIgnoreCase(Constants.LOGICAL_AND_SYM)) {

                targets.add(token);
                tempPredicate = fact -> fact.satisfies(token);

            } else if (token.equalsIgnoreCase(Constants.LOGICAL_NOT) || token.equalsIgnoreCase(Constants.LOGICAL_NOT_SYM)) {

                targets.add(token);
                tempPredicate = fact -> !fact.satisfies(token);
            }

        } while (tokenIdx < conditions.length);

        predicate = tempPredicate;
    }

    /**
     * Returns a copy of the list of target conditions
     *
     * @return A copy of the list of target conditions
     */
    public List<String> getTargets() {

        return List.copyOf(targets);
    }

    /**
     * Returns the constructed predicate, or predicate chain
     *
     * @return The constructed predicate, or predicate chain
     */
    public Predicate<DataStreamFact> getPredicateTest() {

        return predicate;
    }

    /**
     * Returns the rule's id
     *
     * @return The rule's id
     */
    public String getRuleId() {

        return ruleId;
    }
}
