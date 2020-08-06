/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.ndbc;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.persistence.ObsStorageConfig;


public class NDBCConfig extends ObsStorageConfig
{
    @DisplayInfo(desc="Only data matching this filter will be accessible through this storage instance")
    public DataFilter exposeFilter = new DataFilter();
    
    public NDBCConfig()
    {
//        exposeFilter.stationIds.add("0Y2W3");
        exposeFilter.stationIds.add("ljpc1");
        exposeFilter.setStopTime(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));
        exposeFilter.setStartTime(exposeFilter.getStopTime() - TimeUnit.DAYS.toMillis(7));
        System.err.println("Start in cons: " + Instant.ofEpochMilli(exposeFilter.getStartTime()));
        System.err.println("End in cons: " + Instant.ofEpochMilli(exposeFilter.getStopTime()));
//        exposeFilter.startTime = new Date(exposeFilter.endTime.getTime()-3600*24*30*1000);
//        ObsParam [] props = ObsParam.values();
//        for(ObsParam prop: props)
//        	exposeFilter.parameters.add(prop);
        exposeFilter.parameters.add(BuoyParam.WAVES);
//        exposeFilter.parameters.add(ObsParam.AIR_TEMPERATURE);
        
        // NOTE: NDBC bbox requests are apparently not returning all available buoys
//        exposeFilter.siteBbox = new Bbox(-118.0,32.0, -117.0, 33.0);
    }

	@Override
	public void setStorageIdentifier(String name) {
		// TODO Auto-generated method stub
	}
}
