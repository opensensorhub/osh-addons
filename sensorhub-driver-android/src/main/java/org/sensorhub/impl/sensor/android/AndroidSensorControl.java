/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Sensia Software LLC. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.android;

import java.util.List;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.common.CommandStatus;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import android.hardware.Sensor;
import android.hardware.SensorManager;


/**
 * <p>
 * Implementation of control interface for Android sensors
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Sep 6, 2013
 */
public class AndroidSensorControl extends AbstractSensorControl<AndroidSensorsDriver>
{
    SensorManager aSensorManager;
    Sensor androidSensor;
    
    
    protected AndroidSensorControl(AndroidSensorsDriver parentModule, SensorManager aSensorManager, Sensor androidSensor)
    {
        super(parentModule);
        this.aSensorManager = aSensorManager;
        this.androidSensor = androidSensor;
    }


    @Override
    public DataComponent getCommandDescription()
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public CommandStatus execCommand(DataBlock command) throws SensorException
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public CommandStatus execCommandGroup(List<DataBlock> commands) throws SensorException
    {
        // TODO Auto-generated method stub
        return null;
    }

}
