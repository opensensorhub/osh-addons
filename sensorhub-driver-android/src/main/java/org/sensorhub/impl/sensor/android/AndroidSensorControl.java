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
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.sensor.ISensorControlInterface;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.api.sensor.SensorException;
import org.vast.util.DateTime;
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
public class AndroidSensorControl implements ISensorControlInterface
{
    SensorManager aSensorManager;
    Sensor androidSensor;
    
    
    protected AndroidSensorControl(SensorManager aSensorManager, Sensor androidSensor)
    {
        this.aSensorManager = aSensorManager;
        this.androidSensor = androidSensor;
    }
    
    
    protected void init()
    {
        
    }
    
    
    @Override
    public void unregisterListener(IEventListener listener)
    {
        // TODO Auto-generated method stub

    }


    @Override
    public boolean isAsyncExecSupported()
    {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean isSchedulingSupported()
    {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean isStatusHistorySupported()
    {
        // TODO Auto-generated method stub
        return false;
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


    @Override
    public CommandStatus sendCommand(DataBlock command) throws SensorException
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public CommandStatus sendCommandGroup(List<DataBlock> commands) throws SensorException
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public CommandStatus scheduleCommand(DataBlock command, DateTime execTime) throws SensorException
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public CommandStatus scheduleCommandGroup(List<DataBlock> commands, DateTime execTime) throws SensorException
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public CommandStatus cancelCommand(String commandID) throws SensorException
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public CommandStatus getCommandStatus(String commandID) throws SensorException
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public List<CommandStatus> getCommandStatusHistory(String commandID) throws SensorException
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public void registerListener(IEventListener listener)
    {
        // TODO Auto-generated method stub

    }


    @Override
    public ISensorModule<?> getParentSensor()
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public boolean isEnabled()
    {
        // TODO Auto-generated method stub
        return false;
    }

}
