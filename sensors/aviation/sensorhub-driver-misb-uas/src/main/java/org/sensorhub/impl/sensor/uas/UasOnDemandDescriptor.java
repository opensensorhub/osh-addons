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

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.module.JarModuleProvider;
import org.sensorhub.impl.sensor.uas.config.UasOnDemandConfig;

/**
 * Class named in META-INF/services/org.sensorhub.api.module.IModuleProvider that informs OpenSensorHub of the existence
 * of a sensor driver and its configuration class. In this case, it tells OSH about the {@link UasOnDemandSensor}.
 *
 * @author Nick Garay
 * @author Chris Dillard
 * @since March 19, 2022
 */
public class UasOnDemandDescriptor extends JarModuleProvider implements IModuleProvider {
    public Class<? extends IModule<?>> getModuleClass() {
        return UasOnDemandSensor.class;
    }

    public Class<? extends ModuleConfig> getModuleConfigClass() {
        return UasOnDemandConfig.class;
    }

    @Override
	public String getModuleName() {
		return super.getModuleName() + " (on demand)";
	}

	@Override
	public String getModuleDescription() {
		return super.getModuleDescription() + ". This version does not connect to the video source until OSH clients request its data.";
	}
}
