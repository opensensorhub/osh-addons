/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2022 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.service.discovery.engine;

import com.georobotix.ai.impl.rulesengine.RulesEngine;

/**
 * Wrapper for the RulesEngine class that provides singleton access to the rule engine.
 */
public class RulesEngineWrapper {

    /**
     * The one and only instance of the rule engine
     */
    private static final RulesEngine instance = new RulesEngine(null);

    /**
     * Singleton access to the RulesEngine object
     *
     * @return The one and only instance of the rule engine
     */
    public static RulesEngine getInstance() {

        return instance;
    }
}
