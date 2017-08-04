/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.vectornav;

import java.io.IOException;
import java.nio.ByteBuffer;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.Vector;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;


public class VN200QuatOutput extends VN200AbstractOutput
{
    float[] quat = new float[4];
    
    
    public VN200QuatOutput(VN200Sensor parentSensor, double samplingPeriod)
    {
        super("quatData", parentSensor, samplingPeriod);
    }


    protected void init()
    {
        GeoPosHelper fac = new GeoPosHelper();
        
        // build SWE Common record structure
        dataStruct = fac.newDataRecord(2);
        dataStruct.setName(getName());
        
        String localRefFrame = parentSensor.getUniqueIdentifier() + "#" + VN200Sensor.CRS_ID;
                        
        // time stamp
        dataStruct.addComponent("time", fac.newTimeStampIsoUTC());
        
        // fused attiude measurement
        Vector quat = fac.newQuatOrientationNED(SWEHelper.getPropertyUri("Orientation"));
        quat.setLocalFrame(localRefFrame);
        quat.setDataType(DataType.FLOAT);
        dataStruct.addComponent("attitude", quat);
     
        // also generate encoding definition as text block
        dataEncoding = fac.newTextEncoding(",", "\n");        
    }
    
    
    protected void decodeAndSendMeasurement(long timeStamp, ByteBuffer payload) throws IOException
    {
        // decode quaternion message
        // scalar value is last on VN200
        quat[1] = payload.getFloat();
        quat[2] = payload.getFloat();
        quat[3] = payload.getFloat();
        quat[0] = payload.getFloat();
        
        // create and populate datablock
        DataBlock dataBlock;
        if (latestRecord == null)
            dataBlock = dataStruct.createDataBlock();
        else
            dataBlock = latestRecord.renew();
        
        int k = 0;
        dataBlock.setDoubleValue(k++, timeStamp / 1000.);
        for (int i=0; i<4; i++, k++)
            dataBlock.setFloatValue(k, quat[i]);
        
        // update latest record and send event
        latestRecord = dataBlock;
        latestRecordTime = timeStamp;
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, this, dataBlock));        
    }
}
