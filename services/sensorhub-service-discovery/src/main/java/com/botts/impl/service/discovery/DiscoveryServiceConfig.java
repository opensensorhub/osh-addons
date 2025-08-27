/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2022 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.service.discovery;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.security.SecurityConfig;
import org.sensorhub.api.service.HttpServiceConfig;

/**
 * Configuration module for the service
 *
 * @author Nick Garay
 * @since Jan. 7, 2022
 */
public class DiscoveryServiceConfig extends HttpServiceConfig {

    @DisplayInfo(desc = "Security related options")
    public SecurityConfig security = new SecurityConfig();

    @DisplayInfo(label = "Rules", desc = "Rules File")
    public String rulesFilePath = "./config/rules/rules.txt";
    
    @DisplayInfo(label = "Visualization Map File", desc = "Visualization Map File")
    public String visRulesFilePath = "./config/rules/visrules-update.json";

    public DiscoveryServiceConfig() {

        this.moduleClass = DiscoveryService.class.getCanonicalName();

        this.endPoint = "/discovery";
    }
}
