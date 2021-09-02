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

import net.opengis.sensorml.v20.PhysicalSystem;

public class BiologicalSensor extends ISASensor
{

    public BiologicalSensor(ISADriver parent, String id)
    {
        super(parent, id, "Biological Sensor " + id);
        
        sml.edit((PhysicalSystem)smlDescription)
            .addCharacteristicList("operating", sml.characteristics.operatingCharacteristics()
                .add("voltage", sml.characteristics.operatingVoltage(12, "V"))
                .add("current", sml.characteristics.operatingCurrent(1.5, "A"))
                .add("batt_capacity", sml.characteristics.batteryCapacity(200, "W.h")))
            .addCapabilityList("measurement", sml.capabilities.systemCapabilities()
                .add("resolution", sml.capabilities.resolution(1, "[ppm]"))
                .add("accuracy", sml.capabilities.absoluteAccuracy(10, "[ppm]"))
                .add("integ_time", sml.capabilities.integrationTime(14, "s"))
                .add("sampling_freq", sml.capabilities.samplingFrequency(1/60)));
        
        addOutput(new BioReadingOutput(this), false);
    } 
}
