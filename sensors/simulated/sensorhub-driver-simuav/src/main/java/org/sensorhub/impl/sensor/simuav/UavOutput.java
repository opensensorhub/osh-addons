/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2022 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.simuav;

import java.util.concurrent.ScheduledFuture;
import org.sensorhub.api.data.IDataProducer;
import org.sensorhub.impl.sensor.AbstractSensorOutput;


public abstract class UavOutput<T extends IDataProducer> extends AbstractSensorOutput<T>
{
    protected ScheduledFuture<?> future;
    
    
    public UavOutput(String name, T parentSensor)
    {
        super(name, parentSensor);
    }
    
    
    public abstract void start();
    
    
    public void stop()
    {
        if (future != null)
        {
            future.cancel(true);
            future = null;
        }
    }
}
