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
import org.vast.math.Vector3d;
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
public class AndroidOrientationEulerOutput extends AndroidSensorOutput implements SensorEventListener
{
    // keep logger name short because in LogCat it's max 23 chars
    //private static final Logger log = LoggerFactory.getLogger(AndroidOrientationEulerOutput.class.getSimpleName());
    
    private static final String ORIENT_VEC_DEF = "http://sensorml.com/ont/swe/property/Orientation";
    private static final String HEADING_DEF = "http://sensorml.com/ont/swe/property/AngleToNorth";
    private static final String PITCH_DEF = "http://sensorml.com/ont/swe/property/Pitch";
    private static final String ROLL_DEF = "http://sensorml.com/ont/swe/property/Roll";
    private static final String ORIENT_CRS = "http://www.opengis.net/def/crs/OGC/0/ENU";
    private static final String ORIENT_UOM = "deg";
    
    // for euler computation
    Quat4d q = new Quat4d();
    Quat4d look = new Quat4d();
    Vector3d euler = new Vector3d();
    
    
    protected AndroidOrientationEulerOutput(AndroidSensorsDriver parentModule, SensorManager aSensorManager, Sensor aSensor)
    {
        super(parentModule, aSensorManager, aSensor);
        this.name = "euler_orientation_data";
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
        vec.setDefinition(ORIENT_VEC_DEF);
        ((Vector)vec).setReferenceFrame(ORIENT_CRS);
        ((Vector)vec).setLocalFrame("#" + AndroidSensorsDriver.LOCAL_REF_FRAME);
        dataStruct.addComponent("orient", vec);
        
        Quantity c;
        c = fac.newQuantity(DataType.FLOAT);
        c.getUom().setCode(ORIENT_UOM);
        c.setDefinition(HEADING_DEF);
        c.setAxisID("z");
        vec.addComponent("heading",c);

        c = fac.newQuantity(DataType.FLOAT);
        c.getUom().setCode(ORIENT_UOM);
        c.setDefinition(PITCH_DEF);
        c.setAxisID("y");
        vec.addComponent("pitch", c);

        c = fac.newQuantity(DataType.FLOAT);
        c.getUom().setCode(ORIENT_UOM);
        c.setDefinition(ROLL_DEF);
        c.setAxisID("x");
        vec.addComponent("roll", c);
        
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
        
        // convert to euler angles         
        q.x = e.values[0];
        q.y = e.values[1];
        q.z = e.values[2];
        q.w =  e.values[3];
        q.normalize();
        
        // Y direction in phone ref frame
        look.x = 0;
        look.y = 1;
        look.z = 0;
        look.w = 1;
        
        // rotate to ENU
        look.mul(q, look);
        look.mulInverse(q);
                
        double heading = 90. - Math.toDegrees(Math.atan2(look.y, look.x));        
        double pitch = 0.0;//Math.toDegrees(Math.atan2(look.y, look.x)) - 90.;
        double roll = 0.0;
        
        /*double sqw = q.w*q.w;
        double sqx = q.x*q.x;
        double sqy = q.y*q.y;
        double sqz = q.z*q.z;
        System.out.println(q0);
        euler.z = Math.atan2(2.0 * (q.x*q.y + q.z*q.W), sqx - sqy - sqz + sqw);     // heading
        euler.y = Math.atan2(2.0 * (q.y*q.z + q.x*q.w), -sqx - sqy + sqz + sqw);    // pitch
        euler.x = Math.asin(-2.0 * (q.x*q.z - q.y*q.w));                            // roll
        euler.scale(180./Math.PI);
        
        double oldx = euler.x; // convert to ENU
        euler.x = euler.y;
        euler.y = oldx;
        euler.z = -euler.z;*/
        
        // build and populate datablock
        DataBlock dataBlock = dataStruct.createDataBlock();
        dataBlock.setDoubleValue(0, sampleTime);
        dataBlock.setFloatValue(1, (float)heading);
        dataBlock.setFloatValue(2, (float)pitch);
        dataBlock.setFloatValue(3, (float)roll);
        
        // TODO since this sensor is high rate, we could package several records in a single event
        // update latest record and send event
        latestRecord = dataBlock;
        eventHandler.publishEvent(new SensorDataEvent(sampleTime, this, dataBlock)); 
    }    
}
