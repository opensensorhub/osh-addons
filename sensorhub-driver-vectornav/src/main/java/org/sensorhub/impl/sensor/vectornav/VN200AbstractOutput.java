/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.vectornav;

import java.io.IOException;
import java.nio.ByteBuffer;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.impl.sensor.AbstractSensorOutput;


public abstract class VN200AbstractOutput extends AbstractSensorOutput<VN200Sensor>
{
    DataComponent dataStruct;
    DataEncoding dataEncoding;
    double samplingPeriod;


    public VN200AbstractOutput(String name, VN200Sensor parentSensor, double samplingPeriod)
    {
        super(name, parentSensor);
        this.samplingPeriod = samplingPeriod;
    }


    protected abstract void decodeAndSendMeasurement(long timeStamp, ByteBuffer payload) throws IOException;
    
    
    @Override
    public double getAverageSamplingPeriod()
    {
        return samplingPeriod;
    }


    @Override
    public DataComponent getRecordDescription()
    {
        return dataStruct;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return dataEncoding;
    }

}