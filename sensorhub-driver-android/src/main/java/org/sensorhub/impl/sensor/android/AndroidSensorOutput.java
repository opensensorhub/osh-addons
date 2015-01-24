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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.TextEncodingImpl;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;


/**
 * <p>
 * Abstract base for data interfaces connecting to Android sensor API
 * </p>
 *
 * <p>Copyright (c) 2015</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Jan 18, 2015
 */
public abstract class AndroidSensorOutput extends AbstractSensorOutput<AndroidSensorsDriver> implements IAndroidOutput, SensorEventListener
{
    // keep logger name short because in LogCat it's max 23 chars
    protected static final Logger log = LoggerFactory.getLogger(AndroidSensorOutput.class.getSimpleName());
        
    SensorManager sensorManager;
    Sensor sensor;
    String name;
    boolean enabled;
    DataBlock latestRecord;
    DataComponent dataStruct;
    double samplingPeriod;
    long systemTimeOffset = -1L;
    
    
    protected AndroidSensorOutput(AndroidSensorsDriver parentModule, SensorManager aSensorManager, Sensor aSensor)
    {
        super(parentModule);
        this.sensorManager = aSensorManager;
        this.sensor = aSensor;
        this.name = sensor.getName().replaceAll(" ", "_") + "_data";
    }
    
    
    @Override
    public String getName()
    {
        return name;
    }
    
    
    @Override
    public void init()
    {
        // max 10Hz events
        int rateUs = Math.max(sensor.getMinDelay(), 100000);
        samplingPeriod = rateUs / 1e6;
        sensorManager.registerListener(this, sensor, rateUs);
    }
    
    
    @Override
    public void stop()
    {
        sensorManager.unregisterListener(this);
    }


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
        return new TextEncodingImpl(",", "\n");
    }

    
    @Override
    public DataBlock getLatestRecord() throws SensorException
    {
        return latestRecord;
    }
    
    
    @Override
    public double getLatestRecordTime()
    {
        if (latestRecord != null)
            return latestRecord.getDoubleValue(0);
        
        return Double.NaN;
    }
    
    
    protected final double getJulianTimeStamp(long sensorTimeStampNanos)
    {
        long sensorTimeMillis = sensorTimeStampNanos / 1000000;
        
        if (systemTimeOffset < 0)
            systemTimeOffset = System.currentTimeMillis() - sensorTimeMillis;
            
        return (systemTimeOffset + sensorTimeMillis) / 1000.;
    }
}
