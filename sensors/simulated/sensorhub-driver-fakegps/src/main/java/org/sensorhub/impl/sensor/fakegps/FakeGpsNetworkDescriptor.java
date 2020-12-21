/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.fakegps;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.module.JarModuleProvider;


public class FakeGpsNetworkDescriptor extends JarModuleProvider implements IModuleProvider
{
    static final String DRIVER_NAME = "Simulated GPS Vehicle Network";
    static final String DRIVER_DESC = "Simulated vehicle network reporting GPS measurements along random itineraries obtained using Google Direction API";
    
    
    @Override
    public String getModuleName()
    {
        return DRIVER_NAME;
    }
    
    
    @Override
    public String getModuleDescription()
    {
        return DRIVER_DESC;
    }
    
    
    @Override
    public Class<? extends IModule<?>> getModuleClass()
    {
        return FakeGpsNetwork.class;
    }


    @Override
    public Class<? extends ModuleConfig> getModuleConfigClass()
    {
        return FakeGpsNetworkConfig.class;
    }

}
