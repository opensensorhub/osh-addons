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

import org.sensorhub.impl.sensor.AbstractSensorOutput;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;


public abstract class ISAOutput extends AbstractSensorOutput<ISASensor>
{
    DataComponent dataStruct;
    DataEncoding dataEnc;
    
    
    public ISAOutput(String name, ISASensor parentSensor)
    {
        super(name, parentSensor);
    }
    
    
    protected abstract void sendRandomMeasurement();


    @Override
    public double getAverageSamplingPeriod()
    {
        // default = sample every 1 second
        return 1.0;
    }


    @Override
    public DataComponent getRecordDescription()
    {
        return dataStruct;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return dataEnc;
    }

}
