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
import org.sensorhub.impl.module.JarModuleProvider;

/**
 * <p>
 * Descriptor of ES multi source storage module.
 * This is needed for automatic discovery by the ModuleRegistry.
 * </p>
 *
 * @author Mathieu Dhainaut <mathieu.dhainaut@gmail.com>
 * @since 2017
 */
public class ESMultiSourceStorageDescriptor extends JarModuleProvider implements IModuleProvider{

	@Override
	public String getModuleName() {
		 return "ElasticSearch Multi-Source Storage";
	}

	@Override
	public String getModuleDescription() {
		return "Generic implementation of multisource storage using ElasticSearch Database";
	}

	@Override
	public Class<? extends IModule<?>> getModuleClass() {
		return ESMultiSourceStorageImpl.class;
	}

	@Override
	public Class<? extends ModuleConfig> getModuleConfigClass() {
		return ESBasicStorageConfig.class;
	}

}
