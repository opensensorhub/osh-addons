/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2020-2024 Nicolas Garay. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.pibot.buzzer;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.module.JarModuleProvider;

/**
 * Descriptor classes provide access to informative data on the OpenSensorHub driver
 *
 * @author Nick Garay
 * @since Jan. 24, 2021
 */
public class Descriptor extends JarModuleProvider implements IModuleProvider {

    /**
     * Retrieves the class implementing the OpenSensorHub interface necessary to
     * perform SOS/SPS/SOS-T operations.
     *
     * @return The class used to interact with the sensor/sensor platform.
     */
    public Class<? extends IModule<?>> getModuleClass() {

        return Sensor.class;
    }

    /**
     * Identifies the class used to configure this driver
     * @return The java class used to exposing configuration settings for the driver.
     */
    public Class<? extends ModuleConfig> getModuleConfigClass() {

        return Config.class;
    }
}
