/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2024 Botts Innovative Research, Inc. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sta.ingest;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.module.JarModuleProvider;


/**
 * <p>
 * Descriptor of STA Ingest module, needed for automatic discovery
 * by the ModuleRegistry.
 * </p>
 *
 * @author Alex Almanza
 * @since Dec 9, 2024
 */
public class STAIngestDescriptor extends JarModuleProvider implements IModuleProvider
{

    @Override
    public String getModuleName()
    {
        return "SensorThings API Ingest Service";
    }


    @Override
    public String getModuleDescription()
    {
        return "Service to ingest SensorThings data as OSH-readable data";
    }


    @Override
    public Class<? extends IModule<?>> getModuleClass()
    {
        return STAIngestService.class;
    }


    @Override
    public Class<? extends ModuleConfig> getModuleConfigClass()
    {
        return STAIngestConfig.class;
    }

}
