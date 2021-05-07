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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.AbstractMap.SimpleEntry;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.Map.Entry;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.sensorhub.api.datastore.obs.DataStreamKey;
import org.sensorhub.api.obs.IDataStreamInfo;
import org.sensorhub.api.procedure.ProcedureId;
import org.slf4j.Logger;
import org.vast.data.TextEncodingImpl;
import org.vast.swe.SWEBuilders.DataComponentBuilder;
import org.vast.swe.SWEHelper;


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
public class ObsSeriesLoader extends ObsSiteLoader
{
    static final String BASE_URL = USGSWaterDataArchive.BASE_USGS_URL + "site?";
    static final String FOI_UID_PREFIX = USGSWaterDataArchive.UID_PREFIX + "site:";
    static final String AREA_UID_PREFIX = USGSWaterDataArchive.UID_PREFIX + "region:";
    
    
    public ObsSeriesLoader(IParamDatabase paramDb, Logger logger)
    {
        super(paramDb, logger);
    }
    
    
    public Stream<Entry<DataStreamKey, IDataStreamInfo>> getSeries(USGSDataFilter filter)
    {
        try
        {
            String requestUrl = buildSiteInfoRequest(filter);
            
            logger.debug("Requesting time series info: {}", requestUrl);
            URL url = new URL(requestUrl);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            var swe = new SWEHelper();
            
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
            
            var it = new Iterator<Entry<DataStreamKey, IDataStreamInfo>>() {
                Entry<DataStreamKey, IDataStreamInfo> next;
                
                @Override
                public boolean hasNext()
                {
                    return next != null;
                }
    
                @Override
                public Entry<DataStreamKey, IDataStreamInfo> next()
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
                            
                            String siteNum = fields[1];
                            String siteDesc = fields[2];
                            String dataTypeCd = fields[12];
                            String paramCd = fields[13];
                            String seriesIdStr = fields[15];
                            String paramDesc = fields[16];
                            String beginDateStr = fields[21];
                            String endDateStr = fields[22];
                            
                            // skip entries with unsupported values
                            if (seriesIdStr.isBlank() || seriesIdStr.equals("0") || paramCd.isBlank())
                                continue;
                            
                            // parse row values
                            Instant beginDate = LocalDate.parse(beginDateStr).atStartOfDay().toInstant(ZoneOffset.UTC);
                            Instant endDate = LocalDate.parse(endDateStr).atStartOfDay().toInstant(ZoneOffset.UTC);
                            
                            // lookup param name, description and unit
                            var param = paramDb.getParamById(paramCd);
                            
                            // create datacomponent of proper type
                            DataComponentBuilder<?,?> paramComp;
                            if (param.isCategory)
                                paramComp = swe.createCategory();
                            else if (param.isCount)
                                paramComp = swe.createCategory();
                            else
                                paramComp = swe.createQuantity().uom(param.unit);
                            
                            // set common properties
                            var endLabelIdx = param.name.indexOf(',');
                            paramComp.definition(param.uri)
                                     .label(param.name.substring(0, endLabelIdx > 0 ? endLabelIdx : param.name.length()))
                                     .description(param.name + (paramDesc.isBlank() ? "" : ", " + paramDesc));
                            
                            // build complete record
                            var recordStruct = swe.createRecord()
                                .name("param_" + paramCd)
                                .addSamplingTimeIsoUTC("time")
                                .addField("site", swe.createText()
                                    .label("Site Number")
                                    .description("USGS site number composed of 8 to 15 numerical characters"))
                                .addField("value", paramComp)
                                .build();
                            
                            // generate key
                            long dsID = FilterUtils.toDataStreamId(seriesIdStr, paramCd);
                            var dsKey = new DataStreamKey(dsID);
                            
                            var dsInfo = new USGSTimeSeriesInfo.Builder()
                                .withName("USGS Water Time Series #" + seriesIdStr)
                                .withDescription("Time series from site " + siteDesc + " (" + siteNum + ")"
                                                 + (paramDesc.isBlank() ? "" : ", " + paramDesc))
                                .withProcedure(new ProcedureId(1, USGSWaterDataArchive.UID_PREFIX + "network"))
                                .withTimeRange(beginDate, endDate)
                                .withSiteNum(siteNum)
                                .withParamCode(paramCd)
                                .withRecordDescription(recordStruct)
                                .withRecordEncoding(new TextEncodingImpl())
                                .build();
                            
                            this.next = new SimpleEntry<>(dsKey, dsInfo);
                            break;                    
                        }
                    }
                    catch (Exception e)
                    {
                        if (logger.isDebugEnabled())
                            logger.warn("Cannot read site: " + line, e);
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
        return FilterUtils.buildRequestUrl(BASE_URL, filter)
            .append("&outputDataTypeCd=iv") // get only site with IV data available
            .append("&seriesCatalogOutput=true")        
            .toString();
    }
}
