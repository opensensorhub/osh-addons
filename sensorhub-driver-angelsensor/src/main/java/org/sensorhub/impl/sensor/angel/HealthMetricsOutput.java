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

import org.sensorhub.impl.sensor.AbstractSensorOutput;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.vast.swe.SWEHelper;


public class HealthMetricsOutput extends AbstractSensorOutput<AngelSensor>
{
    DataComponent dataStruct;
    DataEncoding dataEnc;
    
    
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
        dataStruct.addComponent("heartRate", fac.newQuantity("http://sensorml.com/ont/swe/property/HeartRate", "Heart Rate", null, "1/min"));        
        dataStruct.addComponent("bodyTemp", fac.newQuantity("http://sensorml.com/ont/swe/property/BodyTemperature", "Body Temperature", null, "Cel"));
        dataStruct.addComponent("bloodOxy", fac.newQuantity("http://sensorml.com/ont/swe/property/BloodOxygen", "Blood Oxygen", null, "%"));
        
        // also generate encoding definition as text block
        dataEnc = fac.newTextEncoding(",", "\n");        
    }
    
    
    protected void sendData()
    {
        DataBlock data;
        if (latestRecord == null)
            data = dataStruct.createDataBlock();
        else
            data = latestRecord.renew();
        
        
    }
       

    protected void start()
    {
        
    }


    protected void stop()
    {
        
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
