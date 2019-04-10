/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.trupulse;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.config.DisplayInfo.FieldType;
import org.sensorhub.api.config.DisplayInfo.ModuleType;
import org.sensorhub.api.config.DisplayInfo.Required;
import org.sensorhub.api.data.IDataProducerModule;
import org.sensorhub.api.config.DisplayInfo.FieldType.Type;


public class TruPulseWithGeolocConfig extends TruPulseConfig
{    
    
    @DisplayInfo(desc="ID of data source to use as location source")
    @FieldType(Type.MODULE_ID)
    @ModuleType(IDataProducerModule.class)
    @Required
    public String locationSourceID;
        
    
    @DisplayInfo(desc="Name of output streaming location data")
    public String locationOutputName;

    
    public TruPulseWithGeolocConfig()
    {
        this.moduleClass = TruPulseWithGeolocSensor.class.getCanonicalName();
    }
}
