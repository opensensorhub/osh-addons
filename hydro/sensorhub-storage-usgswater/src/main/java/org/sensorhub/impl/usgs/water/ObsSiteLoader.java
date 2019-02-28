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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map;
import org.sensorhub.impl.module.AbstractModule;
import org.sensorhub.impl.usgs.water.CodeEnums.SiteType;
import org.sensorhub.impl.usgs.water.CodeEnums.StateCode;
import org.vast.ogc.om.SamplingPoint;
import org.vast.swe.SWEHelper;
import org.vast.util.Bbox;
import net.opengis.gml.v32.AbstractFeature;
import net.opengis.gml.v32.Point;
import net.opengis.gml.v32.impl.GMLFactory;


/**
 * <p>
 * Loader for USGS tabular data of observation sites<br/>
 * See <a href="https://waterservices.usgs.gov/rest/Site-Service.html">
 * web service documentation</a>
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Mar 13, 2017
 */
public class ObsSiteLoader
{
    static final String BASE_URL = USGSWaterDataArchive.BASE_USGS_URL + "site?";
    static final String FOI_UID_PREFIX = USGSWaterDataArchive.UID_PREFIX + "site:";
    static final String AREA_UID_PREFIX = USGSWaterDataArchive.UID_PREFIX + "region:";
    
    AbstractModule<?> module;
    
    
    public ObsSiteLoader(AbstractModule<?> module)
    {
        this.module = module;
    }
    
    
    protected String buildSiteInfoRequest(DataFilter filter)
    {
        StringBuilder buf = new StringBuilder(BASE_URL);
        
        // site ids
        if (!filter.siteIds.isEmpty())
        {
            buf.append("sites=");
            for (String id: filter.siteIds)
                buf.append(id).append(',');
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
        
        // county codes
        else if (!filter.countyCodes.isEmpty())
        {
            buf.append("countyCd=");
            for (String countyCd: filter.countyCodes)
            	buf.append(countyCd).append(',');
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
        
        // site types
        if (!filter.siteTypes.isEmpty())
        {
            buf.append("siteType=");
            for (SiteType type: filter.siteTypes)
                buf.append(type.name()).append(',');
            buf.setCharAt(buf.length()-1, '&');
        }
        
        // constant options
        buf.append("dataType=iv&"); // site with IV data available
        buf.append("siteOutput=expanded&"); // get all info
        buf.append("format=rdb"); // output format
        
        return buf.toString();
    }
    
    
    public void preloadSites(DataFilter filter, Map<String, AbstractFeature> fois) throws IOException
    {
        String requestUrl = buildSiteInfoRequest(filter);
                
        module.getLogger().debug("Requesting site info from: {}", requestUrl);        
        URL url = new URL(requestUrl);
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));)
        {
            GMLFactory gmlFac = new GMLFactory(true);
            String line;
            
            // skip header comments
            while ((line = reader.readLine()) != null)
            {
                line = line.trim();
                if (!line.startsWith("#"))
                    break;
            }
            
            // skip field names and sizes
            reader.readLine();
            
            // start parsing actual data records        
            while ((line = reader.readLine()) != null)
            {                
                line = line.trim();
                if (line.startsWith("#")) // skip comments
                    continue;
                
                String[] fields = line.split("\t");
                if (fields.length < 9) // skip if missing fields
                    continue;
                
                String id = fields[1];
                String desc = fields[2];
                
                try
                {
                    double lat = Double.parseDouble(fields[6]);
                    double lon = Double.parseDouble(fields[7]);
                    int stateCd = Integer.parseInt(fields[13]);
                    //int countyCd = Integer.parseInt(fields[14]);
                    double alt = fields[19].isEmpty() ? Double.NaN : Double.parseDouble(fields[19]);
                                        
                    SamplingPoint site = new SamplingPoint();
                    site.setId(id);
                    site.setUniqueIdentifier(FOI_UID_PREFIX+id);
                    site.setName("USGS Water Site #" + id);
                    site.setDescription(desc);
                                        
                    // location
                    Point siteLoc = gmlFac.newPoint();
                    if (Double.isNaN(alt))
                    {
                        siteLoc.setSrsDimension(2);
                        siteLoc.setSrsName(SWEHelper.getEpsgUri(4269)); // NAD83
                        siteLoc.setPos(new double[] {lat, lon});
                    }
                    else
                    {
                        siteLoc.setSrsDimension(3);
                        siteLoc.setSrsName(SWEHelper.getEpsgUri(5498)); // NAD83 + NGVD29/NAVD88 height
                        siteLoc.setPos(new double[] {lat, lon, alt});
                    }
                    site.setShape(siteLoc);
                    
                    // sampled features (state and county)
                    String stateStr = stateCd<10 ? "0" + stateCd : stateCd + "";
                    
                    site.setSampledFeatureUID(AREA_UID_PREFIX + StateCode.get(stateStr));
                    
                    //site.setSampledFeatureUID(AREA_UID_PREFIX + countyCd);
                    module.getLogger().debug("Sample FOI ID: " + site.getUniqueIdentifier());
                    fois.put(id, site);                
                }
                catch (Exception e)
                {
                    module.getLogger().debug("Unknown location for site " + id + " (" + desc + ")");
                }
            }
        }
    }
}
