/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2022 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.simuav.feasibility;

import org.sensorhub.api.command.ICommandReceiver;
import org.sensorhub.api.command.IStreamingControlInterfaceWithResult;
import org.sensorhub.api.event.IEventHandler;
import org.sensorhub.impl.sensor.AbstractSensorControl;


public abstract class UavFeasibility<T extends ICommandReceiver> extends AbstractSensorControl<T> implements IStreamingControlInterfaceWithResult
{
    
    protected UavFeasibility(String name, T parentSensor)
    {
        super(name, parentSensor);
    }
    
    
    public IEventHandler getEventHandler()
    {
        return this.eventHandler;
    }
}
