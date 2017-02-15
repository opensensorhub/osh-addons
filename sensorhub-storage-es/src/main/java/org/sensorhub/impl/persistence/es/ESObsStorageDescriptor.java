/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence.es;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;

/**
 * <p>
 * Descriptor of ES observation storage module.
 * This is needed for automatic discovery by the ModuleRegistry.
 * </p>
 *
 * @author Mathieu Dhainaut <mathieu.dhainaut@gmail.com>
 * @since 2017
 */
public class ESObsStorageDescriptor implements IModuleProvider{

	@Override
	public String getModuleName() {
		 return "ElasticSearch Observation Storage";
	}

	@Override
	public String getModuleDescription() {
		return "Generic implementation of observation storage using ElasticSearch Database";
	}

	@Override
	public String getModuleVersion() {
		return "0.5";
	}

	@Override
	public String getProviderName() {
		return "Sensia Software LLC";
	}

	@Override
	public Class<? extends IModule<?>> getModuleClass() {
		return ESObsStorageImpl.class;
	}

	@Override
	public Class<? extends ModuleConfig> getModuleConfigClass() {
		return ESStorageConfig.class;
	}

}
