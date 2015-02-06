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
 * Implementation of data interface for Android magnetometers
 * </p>
 *
 * <p>Copyright (c) 2015</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Jan 18, 2015
 */
public class AndroidMagnetoOutput extends AndroidSensorOutput implements SensorEventListener
{
    private static final String MAG_FIELD_DEF = "http://sensorml.com/ont/swe/property/MagneticField";
    private static final String MAG_FIELD_CRS = "#" + AndroidSensorsDriver.LOCAL_REF_FRAME;
    private static final String MAG_FIELD_UOM = "uT";
    
    
    protected AndroidMagnetoOutput(AndroidSensorsDriver parentModule, SensorManager aSensorManager, Sensor aSensor)
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
        vec.setDefinition(MAG_FIELD_DEF);
        ((Vector)vec).setReferenceFrame(MAG_FIELD_CRS);
        dataStruct.addComponent("mag", vec);
        
        Quantity c;
        c = fac.newQuantity(DataType.FLOAT);
        c.getUom().setCode(MAG_FIELD_UOM);
        c.setDefinition(MAG_FIELD_DEF);
        c.setAxisID("x");
        vec.addComponent("mx",c);

        c = fac.newQuantity(DataType.FLOAT);
        c.getUom().setCode(MAG_FIELD_UOM);
        c.setDefinition(MAG_FIELD_DEF);
        c.setAxisID("y");
        vec.addComponent("my", c);

        c = fac.newQuantity(DataType.FLOAT);
        c.getUom().setCode(MAG_FIELD_UOM);
        c.setDefinition(MAG_FIELD_DEF);
        c.setAxisID("z");
        vec.addComponent("mz", c);        
        
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
        eventHandler.publishEvent(new SensorDataEvent(sampleTime, this, dataBlock)); 
    }    
}
