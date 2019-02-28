/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Sensia Software LLC. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.station.metar;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;


public class MetarModuleDescriptor implements IModuleProvider
{

    @Override
    public String getModuleName()
    {
        return "Metar Weather Station Network";
    }


    @Override
    public String getModuleDescription()
    {
        return "Metar weather station network with data provided by AnythingWeather web service";
    }


    @Override
    public String getModuleVersion()
    {
        return "0.1";
    }


    @Override
    public String getProviderName()
    {
        return "Botts Innovative Research Inc";
    }


    @Override
    public Class<? extends IModule<?>> getModuleClass()
    {
        return MetarSensor.class;
    }


    @Override
    public Class<? extends ModuleConfig> getModuleConfigClass()
    {
        return MetarConfig.class;
    }

}
