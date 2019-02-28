/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.mavlink;

import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.impl.sensor.AbstractSensorControl;


public abstract class MavlinkControlInput extends AbstractSensorControl<MavlinkDriver>
{
    DataChoice commandData;


    public MavlinkControlInput(MavlinkDriver parentSensor)
    {
        super(parentSensor);
    }
    
    
    @Override
    public DataComponent getCommandDescription()
    {
        return commandData;
    }


    public void stop()
    {

    }

}