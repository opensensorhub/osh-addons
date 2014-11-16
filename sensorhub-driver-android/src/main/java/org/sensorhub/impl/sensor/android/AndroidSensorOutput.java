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
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.api.sensor.SensorException;
import org.vast.data.TextEncodingImpl;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;


/**
 * <p>
 * Implementation of data interface for Android sensors
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Sep 6, 2013
 */
public class AndroidSensorOutput implements ISensorDataInterface, SensorEventListener
{
    SensorManager aSensorManager;
    Sensor aSensor;
    
    
    protected AndroidSensorOutput(SensorManager aSensorManager, Sensor aSensor)
    {
        this.aSensorManager = aSensorManager;
        this.aSensor = aSensor;
    }
    
    
    protected void init()
    {
        aSensorManager.registerListener(this, aSensor, 10);
    }
    
    
    @Override
    public boolean isEnabled()
    {
        // TODO Auto-generated method stub
        return true;
    }
    
    
    @Override
    public boolean isStorageSupported()
    {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean isPushSupported()
    {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public double getAverageSamplingPeriod()
    {
        // TODO Auto-generated method stub
        return 0;
    }


    @Override
    public DataComponent getRecordDescription() throws SensorException
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DataEncoding getRecommendedEncoding() throws SensorException
    {
        return new TextEncodingImpl(",", "\n");
    }

    
    @Override
    public DataBlock getLatestRecord() throws SensorException
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public int getStorageCapacity() throws SensorException
    {
        // TODO Auto-generated method stub
        return 0;
    }


    @Override
    public int getNumberOfAvailableRecords() throws SensorException
    {
        // TODO Auto-generated method stub
        return 0;
    }


    @Override
    public List<DataBlock> getLatestRecords(int maxRecords, boolean clear) throws SensorException
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public List<DataBlock> getAllRecords(boolean clear) throws SensorException
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public int clearAllRecords() throws SensorException
    {
        // TODO Auto-generated method stub
        return 0;
    }


    @Override
    public void registerListener(IEventListener listener)
    {
        // TODO Auto-generated method stub
    }
    
    
    @Override
    public void unregisterListener(IEventListener listener)
    {
        // TODO Auto-generated method stub
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int arg1)
    {
        // TODO Auto-generated method stub        
    }


    @Override
    public void onSensorChanged(SensorEvent e)
    {
        // TODO Auto-generated method stub        
    }


    @Override
    public ISensorModule<?> getSensorInterface()
    {
        // TODO Auto-generated method stub
        return null;
    }
}
