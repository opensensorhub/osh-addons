/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2022 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.service.discovery;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.module.JarModuleProvider;

/**
 * Descriptor classes provide access to informative data on the OpenSensorHub driver
 *
 * @author Nick Garay
 * @since Jan. 7, 2022
 */
public class DiscoveryServiceDescriptor extends JarModuleProvider implements IModuleProvider {

    @Override
    public Class<? extends IModule<?>> getModuleClass() {

        return DiscoveryService.class;
    }

    @Override
    public Class<? extends ModuleConfig> getModuleConfigClass() {

        return DiscoveryServiceConfig.class;
    }
}
