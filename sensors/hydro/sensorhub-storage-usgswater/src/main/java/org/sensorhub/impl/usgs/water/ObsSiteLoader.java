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
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.sensorhub.impl.usgs.water.CodeEnums.StateCode;
import org.slf4j.Logger;
import org.vast.ogc.gml.IFeature;
import org.vast.ogc.om.SamplingPoint;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.util.Asserts;
import net.opengis.gml.v32.Point;
import net.opengis.gml.v32.impl.GMLFactory;


/**
 * <p>
 * Loader for USGS tabular data of observation sites<br/>
 * See <a href="https://waterservices.usgs.gov/rest/Site-Service.html">
 * web service documentation</a>
 * </p>
 *
 * @author Alex Robin
 * @since Mar 13, 2017
 */
public class ObsSiteLoader
{
    static final String BASE_URL = USGSWaterDataArchive.BASE_USGS_URL + "site?";
    static final String FOI_UID_PREFIX = USGSWaterDataArchive.UID_PREFIX + "site:";
    static final String AREA_UID_PREFIX = USGSWaterDataArchive.UID_PREFIX + "region:";
    
    final int idScope;
    final IParamDatabase paramDb;
    final Logger logger;
    
    public ObsSiteLoader(int idScope, IParamDatabase paramDb, Logger logger)
    {
        this.idScope = idScope;
        this.paramDb = Asserts.checkNotNull(paramDb, IParamDatabase.class);
        this.logger = Asserts.checkNotNull(logger, Logger.class);
    }
    
    
    public Stream<IFeature> getSites(USGSDataFilter filter)
    {
        try
        {
            String requestUrl = buildSiteInfoRequest(filter);
            
            logger.debug("Requesting site info: {}", requestUrl);
            URL url = new URL(requestUrl);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        
            // skip header comments
            String line = null;
            while ((line = reader.readLine()) != null)
            {
                line = line.trim();
                if (!line.startsWith("#"))
                    break;
            }
            
            // skip field names and sizes
            reader.readLine();
            
            var it = new Iterator<IFeature>() {
                GMLFactory gmlFac = new GMLFactory(true);
                IFeature next;
                
                @Override
                public boolean hasNext()
                {
                    return next != null;
                }
    
                @Override
                public IFeature next()
                {
                    if (!hasNext())
                        throw new NoSuchElementException();
                    var next = this.next;
                    preloadNext();
                    return next;
                }
                
                public void preloadNext()
                {
                    String line = null;
                    this.next = null;
                    
                    try
                    {
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
                            double lat = Double.parseDouble(fields[6]);
                            double lon = Double.parseDouble(fields[7]);
                            int stateCd = Integer.parseInt(fields[13]);
                            int countyCd = Integer.parseInt(fields[14]);
                            double alt = fields[19].isEmpty() ? Double.NaN : Double.parseDouble(fields[19]);
                            /*int natAqfrCd = Integer.parseInt(fields[35]);
                            int aqfrCd = Integer.parseInt(fields[36]);
                            int wellDepth = Integer.parseInt(fields[38]);*/
                            
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
                                siteLoc.setSrsName(SWEConstants.REF_FRAME_4326);//SWEHelper.getEpsgUri(4269)); // NAD83
                                siteLoc.setPos(new double[] {lat, lon});
                            }
                            else
                            {
                                siteLoc.setSrsDimension(3);
                                siteLoc.setSrsName(SWEConstants.REF_FRAME_4979);//SWEHelper.getEpsgUri(5498)); // NAD83 + NGVD29/NAVD88 height
                                siteLoc.setPos(new double[] {lat, lon, alt});
                            }
                            site.setShape(siteLoc);
                            
                            // sampled features (state and county)
                            String stateStr = stateCd<10 ? "0" + stateCd : stateCd + "";
                            site.setSampledFeatureUID(AREA_UID_PREFIX + StateCode.get(stateStr));
                            
                            this.next = site;
                            break;
                        }
                    }
                    catch (Exception e)
                    {
                        if (logger.isDebugEnabled())
                            logger.warn("Cannot read site: " + line);
                    }
                }
            };
            
            it.preloadNext();
            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(it, Spliterator.DISTINCT), false);
        }
        catch (IOException e)
        {
            logger.error("Could not fetch site data", e);
            return Stream.empty();
        }
    }
    
    
    protected String buildSiteInfoRequest(USGSDataFilter filter)
    {
        return UsgsUtils.buildRequestUrl(BASE_URL, filter)
            .append("&hasDataTypeCd=iv") // get only site with IV data available
            .append("&siteOutput=expanded")
            .toString();
    }
}
