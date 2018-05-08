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

import java.util.Date;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.persistence.ObsStorageConfig;
import org.sensorhub.impl.ndbc.BuoyEnums.ObsParam;


public class NDBCConfig extends ObsStorageConfig
{
    @DisplayInfo(desc="Only data matching this filter will be accessible through this storage instance")
    public DataFilter exposeFilter = new DataFilter();
    
    public NDBCConfig()
    {
        exposeFilter.stationIds.add("0Y2W3");
        exposeFilter.parameters.add(ObsParam.AIR_PRESSURE_AT_SEA_LEVEL);
        exposeFilter.endTime = new Date(new Date().getTime()/86400000*86400000);
        exposeFilter.startTime = new Date(exposeFilter.endTime.getTime()-3600*24*30*1000);
    }

	@Override
	public void setStorageIdentifier(String name) {
		// TODO Auto-generated method stub
		
	}
}
