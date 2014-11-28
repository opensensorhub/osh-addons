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

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
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
public class AndroidSensorOutput extends AbstractSensorOutput<AndroidSensorsDriver> implements SensorEventListener
{
    SensorManager aSensorManager;
    Sensor aSensor;
    
    
    protected AndroidSensorOutput(AndroidSensorsDriver parentModule, SensorManager aSensorManager, Sensor aSensor)
    {
        super(parentModule);
        this.aSensorManager = aSensorManager;
        this.aSensor = aSensor;
    }
    
    
    protected void init()
    {
        aSensorManager.registerListener(this, aSensor, 10);
    }


    @Override
    public double getAverageSamplingPeriod()
    {
        // TODO Auto-generated method stub
        return 0;
    }


    @Override
    public DataComponent getRecordDescription()
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
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
    public double getLatestRecordTime()
    {
        // TODO Auto-generated method stub
        return 0;
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
    
}
