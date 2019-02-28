/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.angel;

import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataType;
import org.vast.swe.SWEHelper;


public class HealthMetricsOutput extends AbstractSensorOutput<AngelSensor>
{
    DataComponent dataStruct;
    DataEncoding dataEnc;
    
    float lastBodyTemp;
    float lastHeartRate;
        
    
    public HealthMetricsOutput(AngelSensor parentSensor)
    {
        super(parentSensor);
    }


    @Override
    public String getName()
    {
        return "healthData";
    }


    protected void init()
    {
        SWEHelper fac = new SWEHelper();
        
        // build SWE Common record structure
        dataStruct = fac.newDataRecord();
        dataStruct.setName(getName());
        
        // time stamp
        dataStruct.addComponent("time", fac.newTimeStampIsoUTC());
        
        // health measurement
        dataStruct.addComponent("heartRate", fac.newQuantity("http://sensorml.com/ont/swe/property/HeartRate", "Heart Rate", null, "1/min", DataType.FLOAT));        
        dataStruct.addComponent("bodyTemp", fac.newQuantity("http://sensorml.com/ont/swe/property/BodyTemperature", "Body Temperature", null, "Cel", DataType.FLOAT));
        //dataStruct.addComponent("bloodOxy", fac.newQuantity("http://sensorml.com/ont/swe/property/BloodOxygen", "Blood Oxygen", null, "%"));
        
        // also generate encoding definition as text block
        dataEnc = fac.newTextEncoding(",", "\n");        
    }
    
    
    protected void newBodyTemp(float val)
    {
        this.lastBodyTemp = val;
        sendData();
    }
    
    
    protected void newHeartRate(float val)
    {
        this.lastHeartRate = val;
        sendData();
    }
    
    
    protected void sendData()
    {
        DataBlock data;
        if (latestRecord == null)
            data = dataStruct.createDataBlock();
        else
            data = latestRecord.renew();
        
        long timeStamp = System.currentTimeMillis();
        data.setDoubleValue(0, timeStamp / 1000.);
        data.setFloatValue(1, lastHeartRate);
        data.setFloatValue(2, lastBodyTemp);
        
        // update latest record and send event
        latestRecord = data;
        latestRecordTime = timeStamp;
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, this, data));
    }


    @Override
    public double getAverageSamplingPeriod()
    {
    	return 1.0;
    }


    @Override
    public DataComponent getRecordDescription()
    {
        return dataStruct;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return dataEnc;
    }
}
