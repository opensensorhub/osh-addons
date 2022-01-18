/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.simuav;

import org.sensorhub.impl.sensor.simuav.StatusOutput;
import java.util.concurrent.TimeUnit;
import org.sensorhub.api.data.DataEvent;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataType;
import org.vast.sensorML.helper.CommonCharacteristics;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;


public class StatusOutput extends UavOutput<SimUavDriver>
{
    DataComponent dataStruct;
    DataEncoding dataEncoding;

    
    public StatusOutput(SimUavDriver parentSensor)
    {
        super("platform_status", parentSensor);
        
        // create output data structure
        GeoPosHelper fac = new GeoPosHelper();
        dataStruct = fac.createRecord()
            .name(getName())
            .label("Platform Status")
            .addField("time", fac.createTime()
                .asSamplingTimeIsoUTC())
            .addField("armed_state", fac.createCategory()
                .definition(SWEHelper.getPropertyUri("ArmedState"))
                .label("Armed State")
                .addAllowedValues("ARMED", "DISARMED", "NA"))
            .addField("battery_level", fac.createQuantity()
                .definition(CommonCharacteristics.BATT_CAPACITY_DEF)
                .label("Remaining Battery Capacity")
                .dataType(DataType.FLOAT)
                .uom("%"))
            .addField("system_temp", fac.createQuantity()
                .definition(SWEHelper.getQudtUri("Temperature"))
                .label("System Temperature")
                .description("Temperature of motherboard")
                .dataType(DataType.FLOAT)
                .uom("Cel"))
            .build();
     
        // also generate encoding definition
        dataEncoding = fac.newTextEncoding(",", "\n");
    }

    
    public void start()
    {
        stop(); // always stop previous runnable
        
        future = parentSensor.scheduler.scheduleAtFixedRate(() -> {
            
            var now = System.currentTimeMillis();
            var currentState = parentSensor.currentState;
            
            // build and publish datablock
            DataBlock dataBlock = dataStruct.createDataBlock();
            dataBlock.setDoubleValue(0, now/1000.);
            dataBlock.setStringValue(1, currentState.armed ? "ARMED" : "DISARMED");
            dataBlock.setDoubleValue(2, currentState.batt);
            dataBlock.setDoubleValue(3, currentState.temp);
            
            // update latest record and send event
            latestRecord = dataBlock;
            latestRecordTime = now;
            eventHandler.publish(new DataEvent(latestRecordTime, StatusOutput.this, dataBlock));
            
        }, 0, 10000, TimeUnit.MILLISECONDS);
    }


    @Override
    public double getAverageSamplingPeriod()
    {
        return 0.1;
    }


    @Override
    public DataComponent getRecordDescription()
    {
        return dataStruct;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return dataEncoding;
    }
}
