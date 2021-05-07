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
import java.util.TreeSet;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.impl.usgs.water.CodeEnums.ObsParam;
import org.sensorhub.impl.usgs.water.CodeEnums.SiteType;
import org.sensorhub.impl.usgs.water.CodeEnums.StateCode;
import org.vast.util.Bbox;


public class USGSDataFilter
{
    
    @DisplayInfo(desc="List of site identifiers")
    public Set<String> siteIds = new LinkedHashSet<>();
    
    @DisplayInfo(desc="List of US states")
    public Set<StateCode> stateCodes = new LinkedHashSet<>();
    
    @DisplayInfo(desc="List of US counties")
    public Set<String> countyCodes = new LinkedHashSet<>();
    
    @DisplayInfo(desc="Geographic region (BBOX)")
    public Bbox siteBbox = null;
    
    @DisplayInfo(desc="Search string to select sites by name")
    public String siteNameSearch;
    
    @DisplayInfo(desc="List of site types")
    public Set<SiteType> siteTypes = new LinkedHashSet<>();
    
    @DisplayInfo(label="Common parameters", desc="Common observed parameter codes provided as enum")
    public Set<ObsParam> paramCodes = new LinkedHashSet<>();
    
    @DisplayInfo(label="Other parameters", desc="Other observed parameter codes (any 5-digits numerical code)")
    public Set<String> otherParamCodes = new LinkedHashSet<>();
    
    @DisplayInfo(desc="Minimum time stamp of requested objects")
    public Date startTime = null;
    
    @DisplayInfo(desc="Maximum time stamp of requested objects")
    public Date endTime = null;
    
    @DisplayInfo(desc="Get sites operational between for a period in the past, encoded in ISO-8601 (e.g. P10D for the last 10 days)")
    public String isoPeriod = null;
    
    
    public Set<String> getAllParamCodes()
    {
        // merge common and custom parameter sets
        var mergedParams = new TreeSet<>(otherParamCodes);
        for (ObsParam param: paramCodes)
            mergedParams.add(param.getCode());
        
        return mergedParams;
    }
    
}
