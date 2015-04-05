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
import org.vast.math.Quat4d;
import org.vast.swe.SWEConstants;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;


/**
 * <p>
 * Implementation of data interface for Android rotation vector sensors
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Jan 18, 2015
 */
public class AndroidOrientationQuatOutput extends AndroidSensorOutput implements SensorEventListener
{
    // keep logger name short because in LogCat it's max 23 chars
    //private static final Logger log = LoggerFactory.getLogger(AndroidOrientationQuatOutput.class.getSimpleName());
    
    private static final String ORIENT_DEF = "http://sensorml.com/ont/swe/property/OrientationQuaternion";
    private static final String QUAT_DEF = "http://sensorml.com/ont/swe/property/QuaternionComponent";
    private static final String ORIENT_CRS = "http://www.opengis.net/def/crs/OGC/0/ENU";
    private static final String ORIENT_UOM = "1";
    
    Quat4d q = new Quat4d();
    
    
    protected AndroidOrientationQuatOutput(AndroidSensorsDriver parentModule, SensorManager aSensorManager, Sensor aSensor)
    {
        super(parentModule, aSensorManager, aSensor);
        this.name = "quat_orientation_data";
    }


    @Override
    public void init()
    {
        SWEFactory fac = new SWEFactory();
        
        // SWE Common data structure
        dataStruct = fac.newDataRecord(2);
        dataStruct.setName(getName());
        
        Time c1 = fac.newTime();
        c1.getUom().setHref(Time.ISO_TIME_UNIT);
        c1.setDefinition(SWEConstants.DEF_SAMPLING_TIME);
        c1.setReferenceFrame(TIME_REF);
        dataStruct.addComponent("time", c1);

        Vector vec = fac.newVector();        
        vec.setDefinition(ORIENT_DEF);
        ((Vector)vec).setReferenceFrame(ORIENT_CRS);
        ((Vector)vec).setLocalFrame("#" + AndroidSensorsDriver.LOCAL_REF_FRAME);
        dataStruct.addComponent("orient", vec);
        
        Quantity c;
        c = fac.newQuantity(DataType.FLOAT);
        c.getUom().setCode(ORIENT_UOM);
        c.setDefinition(QUAT_DEF);
        c.setAxisID("x");
        vec.addComponent("qx",c);

        c = fac.newQuantity(DataType.FLOAT);
        c.getUom().setCode(ORIENT_UOM);
        c.setDefinition(QUAT_DEF);
        c.setAxisID("y");
        vec.addComponent("qy", c);

        c = fac.newQuantity(DataType.FLOAT);
        c.getUom().setCode(ORIENT_UOM);
        c.setDefinition(QUAT_DEF);
        c.setAxisID("z");
        vec.addComponent("qz", c);
        
        c = fac.newQuantity(DataType.FLOAT);
        c.getUom().setCode(ORIENT_UOM);
        c.setDefinition(QUAT_DEF);
        vec.addComponent("q0", c); 
        
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
        
        // normalize quaternion
        q.x = e.values[0];
        q.y = e.values[1];
        q.z = e.values[2];
        q.w =  e.values[3];
        q.normalize();
        
        // build and populate datablock
        DataBlock dataBlock = dataStruct.createDataBlock();
        dataBlock.setDoubleValue(0, sampleTime);
        dataBlock.setFloatValue(1, (float)q.x);
        dataBlock.setFloatValue(2, (float)q.y);
        dataBlock.setFloatValue(3, (float)q.z);
        dataBlock.setFloatValue(4, (float)q.w); 
        
        // TODO since this sensor is high rate,we could package several records in a single event
        // update latest record and send event
        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, this, dataBlock)); 
    }    
}
