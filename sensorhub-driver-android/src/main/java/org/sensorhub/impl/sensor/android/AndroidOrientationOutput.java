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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class AndroidOrientationOutput extends AndroidSensorOutput implements SensorEventListener
{
    // keep logger name short because in LogCat it's max 23 chars
    private static final Logger log = LoggerFactory.getLogger(AndroidOrientationOutput.class.getSimpleName());
    
    private static final String ORIENT_DEF = "http://sensorml.com/ont/swe/property/OrientationQuaternion";
    private static final String QUAT_DEF = "http://sensorml.com/ont/swe/property/QuaternionComponent";
    private static final String ORIENT_CRS = "http://www.opengis.net/def/crs/OGC/0/ENU";
    private static final String ORIENT_UOM = "1";
    
    
    protected AndroidOrientationOutput(AndroidSensorsDriver parentModule, SensorManager aSensorManager, Sensor aSensor)
    {
        super(parentModule, aSensorManager, aSensor);
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
        
        // build and populate datablock
        DataBlock dataBlock = dataStruct.createDataBlock();
        dataBlock.setDoubleValue(0, sampleTime);
        dataBlock.setFloatValue(1, e.values[0]);
        dataBlock.setFloatValue(2, e.values[1]);
        dataBlock.setFloatValue(3, e.values[2]);
        dataBlock.setFloatValue(4, e.values[3]); 
        
        // convert to euler angles
        /*Quat4d q = new Quat4d();
        q.x = e.values[0];
        q.y = e.values[1];
        q.z = e.values[2];
        q.w = e.values[3];
        Vector3d euler = q.getEulerAngles();
        euler.scale(180./Math.PI);
        double oldx = euler.x;
        euler.x = euler.y;
        euler.y = oldx;
        euler.z = -euler.z;
        log.debug(euler.toString());*/
        
        // TODO since this sensor is high rate,we could package several records in a single event
        // update latest record and send event
        latestRecord = dataBlock;
        eventHandler.publishEvent(new SensorDataEvent(sampleTime, this, dataBlock)); 
    }    
}
