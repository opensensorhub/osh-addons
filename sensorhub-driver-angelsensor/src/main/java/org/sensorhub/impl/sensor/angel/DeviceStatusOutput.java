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
import org.vast.swe.SWEHelper;


public class DeviceStatusOutput extends AbstractSensorOutput<AngelSensor>
{
    DataComponent dataStruct;
    DataEncoding dataEnc;
    
    float lastBatLevel;
    float lastTxLevel;
    
    
    public DeviceStatusOutput(AngelSensor parentSensor)
    {
        super(parentSensor);
    }


    @Override
    public String getName()
    {
        return "deviceStatus";
    }


    protected void init()
    {
        SWEHelper fac = new SWEHelper();
        
        // build SWE Common record structure
        dataStruct = fac.newDataRecord();
        dataStruct.setName(getName());
        
        // time stamp
        dataStruct.addComponent("time", fac.newTimeStampIsoUTC());
        
        // status info
        dataStruct.addComponent("batLevel", fac.newQuantity("http://sensorml.com/ont/swe/property/BatteryLevel", "Battery Level", null, "%"));
        dataStruct.addComponent("txLevel", fac.newQuantity("http://sensorml.com/ont/swe/property/TxLevel", "Transmit Power Level", null, "dBm"));
     
        // also generate encoding definition as text block
        dataEnc = fac.newTextEncoding(",", "\n");        
    }


    protected void newBatLevel(float val)
    {
        this.lastBatLevel = val;
        sendData();
    }
    
    
    protected void newTxLevel(float val)
    {
        this.lastTxLevel = val;
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
        data.setFloatValue(1, lastBatLevel);
        data.setFloatValue(2, lastTxLevel);
        
        // update latest record and send event
        latestRecord = data;
        latestRecordTime = timeStamp;
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, this, data));
    }
    
    
    @Override
    public double getAverageSamplingPeriod()
    {
    	return 5.0;
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
