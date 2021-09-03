/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sta;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.module.JarModuleProvider;


/**
 * <p>
 * Descriptor of SensorThings service module, needed for automatic discovery by
 * the ModuleRegistry.
 * </p>
 *
 * @author Alex Robin
 * @since Sep 6, 2019
 */
public class STAServiceDescriptor extends JarModuleProvider implements IModuleProvider
{

    @Override
    public String getModuleName()
    {
        return "SensorThings API Service";
    }


    @Override
    public String getModuleDescription()
    {
        return "Data access service compliant with the OGC SensorThings API 1.0 standard";
    }


    @Override
    public Class<? extends IModule<?>> getModuleClass()
    {
        return STAService.class;
    }


    @Override
    public Class<? extends ModuleConfig> getModuleConfigClass()
    {
        return STAServiceConfig.class;
    }

}
