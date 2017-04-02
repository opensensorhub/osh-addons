/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2017 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.usgs.water;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.impl.usgs.water.CodeEnums.ObsParam;
import org.sensorhub.impl.usgs.water.CodeEnums.SiteType;
import org.sensorhub.impl.usgs.water.CodeEnums.StateCode;
import org.vast.util.Bbox;


public class DataFilter
{
    @DisplayInfo(desc="List of site identifiers")
    public Set<String> siteIds = new LinkedHashSet<>();
    
    @DisplayInfo(desc="List of US states")
    public Set<StateCode> stateCodes = new LinkedHashSet<>();
    
    @DisplayInfo(desc="List of US counties")
    public Set<String> countyCodes = new LinkedHashSet<>();
    
    @DisplayInfo(desc="Geographic region (BBOX)")
    public Bbox siteBbox = null;
    
    @DisplayInfo(desc="List of site types")
    public Set<SiteType> siteTypes = new LinkedHashSet<>();
    
    @DisplayInfo(desc="Observed parameters")
    public Set<ObsParam> parameters = new LinkedHashSet<>();
    
    @DisplayInfo(desc="Minimum time stamp of requested objects")
    public Date startTime = null;
    
    @DisplayInfo(desc="Maximum time stamp of requested objects")
    public Date endTime = null;    
}
