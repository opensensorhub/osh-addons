/*
 *  The contents of this file are subject to the Mozilla Public License, v. 2.0.
 *  If a copy of the MPL was not distributed with this file, You can obtain one
 *  at http://mozilla.org/MPL/2.0/.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 *  for the specific language governing rights and limitations under the License.
 *
 *  Copyright (C) 2025 Botts Innovative Research, Inc. All Rights Reserved.
 */
package org.sensorhub.api.comm.providers;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.module.JarModuleProvider;


public class UDP2CommModuleDescriptor extends JarModuleProvider implements IModuleProvider
{
    @Override
    public Class<? extends IModule<?>> getModuleClass()
    {
        return UDP2CommProvider.class;
    }
    

    @Override
    public Class<? extends ModuleConfig> getModuleConfigClass()
    {
        return UDP2CommProviderConfig.class;
    }
}
