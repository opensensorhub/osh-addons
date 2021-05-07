/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.uas;

import org.sensorhub.impl.sensor.uas.config.UasConfig;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.module.JarModuleProvider;

/**
 * Descriptor classes provide access to informative data on the OpenSensorHub driver
 *
 * @author Nick Garay
 * @since Feb. 6, 2020
 */
public class UasDescriptor extends JarModuleProvider implements IModuleProvider {

    private static final String MODULE_VERSION = "1.0.0";
    private static final String MODULE_NAME = "UAS Sensor Driver";
    private static final String MODULE_DESCRIPTION = "UAS OpenSensorHub driver providing data feeds to on" +
            "board sensors";
    private static final String MODULE_PROVIDER = "Botts Innovative Research, Inc.";

    /**
     * @return Name of the module
     */
    @Override
    public String getModuleName() {

        return MODULE_NAME;
    }

    /**
     * @return Description of the module
     */
    public String getModuleDescription() {

        return MODULE_DESCRIPTION;
    }

    /**
     * @return The software version of the module as a tuple of three values
     * <code>major.minor.patch</code>
     */
    public String getModuleVersion() {

        return MODULE_VERSION;
    }

    /**
     * @return The name of the provider
     */
    public String getProviderName() {

        return MODULE_PROVIDER;
    }

    /**
     * Retrieves the class implementing the OpenSensorHub interface necessary to
     * perform SOS/SPS/SOS-T operations.
     *
     * @return The class used to interact with the sensor/sensor platform.
     */
    public Class<? extends IModule<?>> getModuleClass() {

        return UasSensor.class;
    }

    /**
     * Identifies the class used to configure this driver
     * @return The java class used to exposing configuration settings for the driver.
     */
    public Class<? extends ModuleConfig> getModuleConfigClass() {

        return UasConfig.class;
    }
}
