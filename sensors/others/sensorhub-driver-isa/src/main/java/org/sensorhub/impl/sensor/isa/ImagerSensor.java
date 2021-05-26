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


public class ImagerSensor extends ISASensor
{

    public ImagerSensor(ISADriver parent, String id, String codec)
    {
        super(parent, id, "Imager Sensor " + id);
        
        addOutput(new ImageOutput(this, codec), false);
    }
    
    
    @Override
    protected String getSensorType()
    {
        return "Imager";
    }
}
