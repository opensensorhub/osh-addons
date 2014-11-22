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
import net.opengis.sensorml.v20.AbstractProcess;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.persistence.StorageException;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.common.BasicEventHandler;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;


public class AndroidSensorsDriver extends AbstractSensorModule<AndroidSensorsConfig>
{
    SensorManager sensorManager;
    
    
    public AndroidSensorsDriver(Context androidContext)
    {
        this.eventHandler = new BasicEventHandler();
        this.sensorManager = (SensorManager)androidContext.getSystemService(Context.SENSOR_SERVICE);
        
        // create one data interface per available sensor
        List<Sensor> deviceSensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor sensor: deviceSensors)
        {
            obsOutputs.put(sensor.getName(), new AndroidSensorOutput(this, sensorManager, sensor));
            controlInputs.put(sensor.getName(), new AndroidSensorControl(this, sensorManager, sensor));
        }
    }
    
    
    @Override
    public void start() throws StorageException
    {
        // TODO Auto-generated method stub
    }
    
    
    @Override
    public void stop() throws StorageException
    {
        // TODO Auto-generated method stub
    }


    @Override
    public AbstractProcess getCurrentSensorDescription() throws SensorException
    {
        return super.getCurrentSensorDescription();
    }


    @Override
    public boolean isConnected()
    {
        // TODO Check if Android sensors are active
        return false;
    }
    
    
    @Override
    public void cleanup() throws SensorHubException
    {
        // TODO deactivate sensors        
    }
}
