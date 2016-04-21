/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.v4l;

import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorOutput;


public abstract class V4LCameraOutput extends AbstractSensorOutput<V4LCameraDriver>
{
    long systemTimeOffset = -1L;
    
    
    public V4LCameraOutput(V4LCameraDriver parentSensor)
    {
        super(parentSensor);
    }


    public V4LCameraOutput(String name, V4LCameraDriver parentSensor)
    {
        super(name, parentSensor);
    }


    protected abstract void init() throws SensorException;


    protected abstract void stop();
    
    
    protected final double getJulianTimeStamp(long v4lTimeStampMicros)
    {
        long sensorTimeMillis = v4lTimeStampMicros / 1000;
        
        if (systemTimeOffset < 0)
            systemTimeOffset = System.currentTimeMillis() - sensorTimeMillis;
            
        return (systemTimeOffset + sensorTimeMillis) / 1000.;
    }

}