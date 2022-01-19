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

import org.sensorhub.impl.sensor.simuav.PositionOutput;
import java.util.concurrent.TimeUnit;
import org.sensorhub.api.data.DataEvent;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.vast.swe.SWEConstants;
import org.vast.swe.helper.GeoPosHelper;


public class PositionOutput extends UavOutput<SimUavDriver>
{
    DataComponent dataStruct;
    DataEncoding dataEncoding;

    
    public PositionOutput(SimUavDriver parentSensor)
    {
        super("platform_pos", parentSensor);
        
        // create output data structure
        GeoPosHelper fac = new GeoPosHelper();
        dataStruct = fac.createRecord()
            .name(getName())
            .label("Platform Position")
            .addField("time", fac.createTime()
                .asSamplingTimeIsoUTC())
            .addField("pos", fac.createLocationVectorLLA()
                .definition(SWEConstants.DEF_PLATFORM_LOC))
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
            dataBlock.setDoubleValue(1, currentState.lat);
            dataBlock.setDoubleValue(2, currentState.lon);
            dataBlock.setDoubleValue(3, currentState.alt);
            
            // update latest record and send event
            latestRecord = dataBlock;
            latestRecordTime = now;
            eventHandler.publish(new DataEvent(latestRecordTime, PositionOutput.this, dataBlock));
            
        }, 0, 1000, TimeUnit.MILLISECONDS);
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
        return dataEncoding;
    }
}
