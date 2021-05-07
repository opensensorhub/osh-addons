/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.usgs.water;

import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.regex.Pattern;
import org.sensorhub.api.datastore.TemporalFilter;
import org.sensorhub.api.datastore.feature.FeatureFilterBase;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.feature.FeatureId;
import org.sensorhub.impl.usgs.water.CodeEnums.SiteType;
import org.sensorhub.impl.usgs.water.CodeEnums.StateCode;
import org.vast.util.Bbox;
import com.google.common.base.Strings;


public class FilterUtils
{
    static final Pattern STATE_CODE_REGEX = Pattern.compile("[A-Z a-z]{2}");
    static final Pattern COUNTY_CODE_REGEX = Pattern.compile("[0-9]{5}");
    static final Pattern SITE_CODE_REGEX = Pattern.compile("[0-9]{8,15}");
    static final Pattern PARAM_CODE_REGEX = Pattern.compile("[0-9]{5}");
    
    
    /**
     * Creates a new USGS web service query from a FeatureFilter
     * @param filter Feature filter from DataStore API
     * @return USGS query filter
     */
    public static USGSDataFilter from(FeatureFilterBase<?> filter)
    {
        return from(filter, new USGSDataFilter());
    }
    
    
    /**
     * Sets values of the provided USGS web service query using values from a
     * FeatureFilter
     * @param filter Feature filter from DataStore API
     * @param usgsFilter USGS query to modify
     * @return USGS query filter
     */
    public static USGSDataFilter from(FeatureFilterBase<?> filter, USGSDataFilter usgsFilter)
    {
        if (filter.getInternalIDs() != null)
        {
            for (long id: filter.getInternalIDs())
                usgsFilter.siteIds.add(toSiteCode(id));
        }
        
        if (filter.getLocationFilter() != null)
        {
            var env = filter.getLocationFilter().getRoi().getEnvelopeInternal();
            usgsFilter.siteBbox = Bbox.fromJtsEnvelope(env);
        }
        
        if (filter.getValidTime() != null)
        {
            if (filter.getValidTime().isCurrentTime())
            {
                usgsFilter.isoPeriod = "P1D";
            }
            else
            {
                var startTimeMs = filter.getValidTime().getMin().toEpochMilli();
                usgsFilter.startTime = new Date(startTimeMs);
                
                var endTimeMs = filter.getValidTime().getMax().toEpochMilli();
                usgsFilter.endTime = new Date(endTimeMs);
            }
        }
        
        if (filter.getFullTextFilter() != null)
        {
            keywordLoop:
            for (var kw: filter.getFullTextFilter().getKeywords())
            {
                // special case if it's a state code
                if (STATE_CODE_REGEX.matcher(kw).matches())
                {
                    for (var stateCd: StateCode.values())
                    {
                        if (stateCd.name().equals(kw))
                        {
                            usgsFilter.stateCodes.add(stateCd);
                            continue keywordLoop;
                        }
                    }
                }
                
                // special case for county codes
                if (COUNTY_CODE_REGEX.matcher(kw).matches())
                {
                    usgsFilter.countyCodes.add(kw);
                }
                
                // special case for site codes
                else if (SITE_CODE_REGEX.matcher(kw).matches())
                {
                    usgsFilter.siteIds.add(kw);
                }
                
                // search site names
                else
                {
                    usgsFilter.siteNameSearch = kw;
                }
            }
        }
        
        return usgsFilter;
    }
    
    
    public static USGSDataFilter from(DataStreamFilter filter)
    {
        return from(filter, new USGSDataFilter());
    }
    
    
    public static USGSDataFilter from(DataStreamFilter filter, USGSDataFilter usgsFilter)
    {
        /*if (filter.getProcedureFilter() != null)
        {
            var procFilter = from(filter.getProcedureFilter());
            // AND with foi filter options in case it was set
            usgsFilter = and(usgsFilter, procFilter);
        }*/
        
        if (filter.getObservedProperties() != null)
        {
            // convert observed properties to USGS codes
            for (var propUri: filter.getObservedProperties())
            {
                //usgsFilter.parameters.add(ObsParam.)
            }
        }
        
        return usgsFilter;
    }
    
    
    public static USGSDataFilter from(ObsFilter filter)
    {
        var usgsFilter = new USGSDataFilter();
        
        if (filter.getFoiFilter() != null)
            usgsFilter = from(filter.getFoiFilter(), usgsFilter);
        
        if (filter.getDataStreamFilter() != null)
            usgsFilter = from(filter.getDataStreamFilter(), usgsFilter);
        
        if (filter.getInternalIDs() != null)
        {
            // decode ID to site + param + time
        }
        
        // obs time range
        var timeFilter = filter.getPhenomenonTime() != null ? filter.getPhenomenonTime() :
                        filter.getResultTime() != null ? filter.getResultTime() :
                        new TemporalFilter.Builder().withAllTimes().build();        
        var begin = timeFilter.getMin();
        var end = timeFilter.getMax();
                
        // set start/end data only if not requesting current obs
        // since current time is the default on USGS server
        if (!timeFilter.isCurrentTime() && !timeFilter.isLatestTime())
        {
            var startTimeMs = begin != Instant.MIN ? begin.toEpochMilli() : Long.MIN_VALUE;
            usgsFilter.startTime = new Date(startTimeMs);
            
            var endTimeMs = end != Instant.MAX ? end.toEpochMilli() : Instant.now().toEpochMilli();                
            usgsFilter.endTime = new Date(endTimeMs);
        }
        
        return usgsFilter;
    }
    
    
    public static USGSDataFilter and(USGSDataFilter f1, USGSDataFilter f2)
    {
        var resultFilter = new USGSDataFilter();
        
        // AND all sets
        and(f1.siteIds, f2.siteIds, resultFilter.siteIds);
        and(f1.stateCodes, f2.stateCodes, resultFilter.stateCodes);
        and(f1.countyCodes, f2.countyCodes, resultFilter.countyCodes);
        and(f1.siteTypes, f2.siteTypes, resultFilter.siteTypes);
        and(f1.paramCodes, f2.paramCodes, resultFilter.paramCodes);
        and(f1.otherParamCodes, f2.otherParamCodes, resultFilter.otherParamCodes);
        
        // site name search
        if (f1.siteNameSearch == null)
            resultFilter.siteNameSearch = f2.siteNameSearch;

        // compute time range intersection
        resultFilter.startTime = f1.startTime == null ? f2.startTime :
            f2.startTime == null ? f1.startTime :
            new Date(Math.max(f1.startTime.getTime(), f2.startTime.getTime()));
        resultFilter.endTime = f1.endTime == null ? f2.endTime :
            f2.endTime == null ? f1.endTime :
            new Date(Math.max(f1.endTime.getTime(), f2.endTime.getTime()));
        
        // compute intersection with iso period before current time
        if (f1.isoPeriod != null)
        {
            
        }
        
        // compute bbox intersection
        resultFilter.siteBbox = f1.siteBbox == null ? f2.siteBbox :
            f2.siteBbox == null ? f1.siteBbox :
            Bbox.fromJtsEnvelope(f1.siteBbox.toJtsEnvelope().intersection(f2.siteBbox.toJtsEnvelope()));

        return resultFilter;
    }
    
    
    private static <T> void and(Set<T> set1, Set<T> set2, Set<T> result)
    {
        // Note: an empty set means no filter so we cannot just intersect the sets!
        // we have to check for empty sets first
        result.addAll(set1);
        if (result.isEmpty())
            result.addAll(set2);
        else if (!set2.isEmpty())
            result.retainAll(set2);
    }
    
    
    public static StringBuilder buildRequestUrl(String baseUrl, USGSDataFilter filter)
    {
        StringBuilder buf = new StringBuilder(baseUrl);
        
        // site ids
        if (!filter.siteIds.isEmpty())
        {
            buf.append("sites=");
            for (String id: filter.siteIds)
                buf.append(id).append(',');
            buf.setCharAt(buf.length()-1, '&');
        }
        
        // county codes
        else if (!filter.countyCodes.isEmpty())
        {
            buf.append("countyCd=");
            for (String countyCd: filter.countyCodes)
                buf.append(countyCd).append(',');
            buf.setCharAt(buf.length()-1, '&');
        }
        
        // state codes
        else if (!filter.stateCodes.isEmpty())
        {
            buf.append("stateCd=");
            for (StateCode state: filter.stateCodes)
                buf.append(state.name()).append(',');
            buf.setCharAt(buf.length()-1, '&');
        }
        
        // site bbox
        else if (filter.siteBbox != null && !filter.siteBbox.isNull())
        {
            Bbox bbox = filter.siteBbox;
            buf.append("bbox=")
               .append(bbox.getMinX()).append(",")
               .append(bbox.getMaxY()).append(",")
               .append(bbox.getMaxX()).append(",")
               .append(bbox.getMinY()).append("&");
        }
        
        // site name
        if (!Strings.isNullOrEmpty(filter.siteNameSearch))
        {
            buf.append("siteNameMatchOperator=any&siteName=");
            buf.append(filter.siteNameSearch);
            buf.append('&');
        }
        
        // site types
        if (!filter.siteTypes.isEmpty())
        {
            buf.append("siteType=");
            for (SiteType type: filter.siteTypes)
                buf.append(type.name()).append(',');
            buf.setCharAt(buf.length()-1, '&');
        }

        // parameters
        var allParamCodes = filter.getAllParamCodes();
        if (!allParamCodes.isEmpty())
        {
            buf.append("parameterCd=");
            for (var code : allParamCodes)
                buf.append(code).append(',');
            buf.setCharAt(buf.length() - 1, '&');
        }
        
        // absolute date range
        if (filter.startTime != null)
        {
            var startDt = Instant.ofEpochMilli(filter.startTime.getTime());
            buf.append("startDT=")
               .append(startDt.toString())
               .append('&');
        }
         
        if (filter.endTime != null)
        {
            var endDt = Instant.ofEpochMilli(filter.endTime.getTime());
            buf.append("endDT=")
               .append(endDt.toString())
               .append('&');
        }
        
        // past period relative to now
        if (filter.startTime == null && filter.endTime == null && filter.isoPeriod != null)
        {
            buf.append("period=" + filter.isoPeriod + "&");
        }
        // constant options
        buf.append("format=rdb"); // output in RDB format = tab separated 
        
        return buf;
    }
    
    
    public static String toSiteCode(long id)
    {
        // convert to string and pad with zeroes
        if (id < 10000000)
            return String.format("%08d", id);
        else if (id < 1000000000)
            return String.format("%010d", id);
        else if (id < 100000000000L)
            return String.format("%012d", id);
        else
            return Long.toString(id);
    }
    
    
    public static String toParamCode(long id)
    {
        return String.format("%05d", id);
    }
    
    
    public static long toLongId(String id)
    {
        return Long.parseLong(id);
    }
    
    
    public static long toDataStreamId(String tsId, String paramCd)
    {
        long seriesId = Long.parseLong(tsId);
        long paramId = Long.parseLong(paramCd);
        return seriesId << 32 | paramId;
    }
    
    
    public static FeatureId toFoiId(String id)
    {
        return new FeatureId(
            toLongId(id),
            ObsSiteLoader.FOI_UID_PREFIX+id);
    }
}
