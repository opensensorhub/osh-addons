/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.android;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.Time;
import net.opengis.swe.v20.Vector;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.vast.data.SWEFactory;
import org.vast.swe.SWEConstants;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;


/**
 * <p>
 * Implementation of data interface for Android accelerometers
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Jan 18, 2015
 */
public class AndroidAcceleroOutput extends AndroidSensorOutput implements SensorEventListener
{
    private static final String ACCEL_DEF = "http://sensorml.com/ont/swe/property/Acceleration";
    private static final String ACCEL_CRS = "#" + AndroidSensorsDriver.LOCAL_REF_FRAME;
    private static final String ACCEL_UOM = "m/s2";
    
    
    protected AndroidAcceleroOutput(AndroidSensorsDriver parentModule, SensorManager aSensorManager, Sensor aSensor)
    {
        super(parentModule, aSensorManager, aSensor);
    }


    @Override
    public void init()
    {
        // SWE Common data structure
        SWEFactory fac = new SWEFactory();
        dataStruct = fac.newDataRecord(2);
        dataStruct.setName(getName());
        
        Time c1 = fac.newTime();
        c1.getUom().setHref(Time.ISO_TIME_UNIT);
        c1.setDefinition(SWEConstants.DEF_SAMPLING_TIME);
        c1.setReferenceFrame(TIME_REF);
        dataStruct.addComponent("time", c1);

        Vector vec = fac.newVector();        
        vec.setDefinition(ACCEL_DEF);
        ((Vector)vec).setReferenceFrame(ACCEL_CRS);
        dataStruct.addComponent("accel", vec);
        
        Quantity c;
        c = fac.newQuantity(DataType.FLOAT);
        c.getUom().setCode(ACCEL_UOM);
        c.setDefinition(ACCEL_DEF);
        c.setAxisID("x");
        vec.addComponent("ax",c);

        c = fac.newQuantity(DataType.FLOAT);
        c.getUom().setCode(ACCEL_UOM);
        c.setDefinition(ACCEL_DEF);
        c.setAxisID("y");
        vec.addComponent("ay", c);

        c = fac.newQuantity(DataType.FLOAT);
        c.getUom().setCode(ACCEL_UOM);
        c.setDefinition(ACCEL_DEF);
        c.setAxisID("z");
        vec.addComponent("az", c);        
        
        super.init();
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int arg1)
    {     
    }


    @Override
    public void onSensorChanged(SensorEvent e)
    {
        double sampleTime = getJulianTimeStamp(e.timestamp);
        
        // build and populate datablock
        DataBlock dataBlock = dataStruct.createDataBlock();
        dataBlock.setDoubleValue(0, sampleTime);
        dataBlock.setFloatValue(1, e.values[0]);
        dataBlock.setFloatValue(2, e.values[1]);
        dataBlock.setFloatValue(3, e.values[2]);        
                
        // TODO since this sensor is high rate,we could package several records in a single event
        // update latest record and send event
        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, this, dataBlock)); 
    }    
}
