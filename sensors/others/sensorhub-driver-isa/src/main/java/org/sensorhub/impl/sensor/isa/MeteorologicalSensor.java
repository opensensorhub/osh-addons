/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.isa;


public class MeteorologicalSensor extends ISASensor
{

    public MeteorologicalSensor(ISADriver parent, String id)
    {
        super(parent, id, "Weather Station " + id);
        
        addOutput(new AtmosTempOutput(this), false);
        addOutput(new AtmosPressureOutput(this), false);
        addOutput(new AtmosHumidityOutput(this), false);
        addOutput(new AtmosPrecipOutput(this), false);
        addOutput(new AtmosWindOutput(this), false);
    }
    
    
    @Override
    protected String getSensorType()
    {
        return "Weather Station";
    }
}
