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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.TimeUnit;
import java.util.Map.Entry;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.data.ObsData;
import org.slf4j.Logger;
import org.vast.data.DataBlockDouble;
import org.vast.data.DataBlockFloat;
import org.vast.data.DataBlockMixed;
import org.vast.data.DataBlockString;
import org.vast.util.Asserts;
import net.opengis.swe.v20.DataBlock;


/**
 * <p>
 * Streaming parser for USGS tabular data of observations.<br/>
 * See <a href="https://waterservices.usgs.gov/rest/IV-Service.html"> web
 * service documentation</a>
 * </p>
 *
 * @author Alex Robin
 * @since Mar 15, 2017
 */
public class ObsRecordLoader implements Iterator<Entry<BigId, IObsData>>
{
    static final String BASE_URL = USGSWaterDataArchive.BASE_USGS_URL + "iv?";

    final int idScope;
    final IParamDatabase paramDb;
    final Logger logger;
    USGSDataFilter usgsFilter;
    long originalEndTime = 0;
    long limit;
    long batchDuration;
    long nextBatchStartTime;
    boolean lastBatch;
    BufferedReader reader;
    ArrayDeque<Entry<BigId, IObsData>> nextRecords = new ArrayDeque<>();
    ParamValueParser[] paramReaders;
    DataBlockMixed dataBlkTemplate;
    
    
    public ObsRecordLoader(int idScope, IParamDatabase paramDb, Logger logger)
    {
        this.idScope = idScope;
        this.paramDb = Asserts.checkNotNull(paramDb, IParamDatabase.class);
        this.logger = Asserts.checkNotNull(logger, Logger.class);
    }
    
    
    @Override
    public boolean hasNext()
    {
        return !nextRecords.isEmpty();
    }
    

    @Override
    public Entry<BigId, IObsData> next()
    {
        if (!hasNext())
            throw new NoSuchElementException();
        var next = nextRecords.poll();
        if (nextRecords.isEmpty())
            preloadNext();
        return next;
    }
    
    
    protected void preloadNext()
    {
        String line = null;
        
        try
        {
            do
            {
                while ((line = reader.readLine()) != null)
                {                            
                    // parse site header
                    if (line.startsWith("#"))
                    {
                        // reset param value parsers
                        paramReaders = parseHeader(reader, usgsFilter);
                        
                        // create initial datablock
                        dataBlkTemplate = new DataBlockMixed(3);
                        dataBlkTemplate.setBlock(0, new DataBlockDouble(1)); // time
                        dataBlkTemplate.setBlock(1, new DataBlockString(1)); // site
                        dataBlkTemplate.setBlock(2, new DataBlockFloat(1)); // value
                        continue;
                    }
                    
                    // split line into field values
                    line = line.trim();
                    String[] fields = line.split("\t");
                    if (fields.length < 2) // no result
                        continue;
                    
                    // read all requested fields to datablock
                    var dataBlk = dataBlkTemplate.renew();
                    for (ParamValueParser reader: paramReaders)
                    {
                        reader.parse(fields, dataBlk);
                    
                        if (reader instanceof TimeStampParser || reader instanceof SiteNumParser)
                            continue;
                        
                        if (reader.noValue)
                            continue;
                        
                        var ts = Instant.ofEpochMilli((long)(dataBlk.getDoubleValue(0)*1000.));
                        var obs = new ObsData.Builder()
                            .withDataStream(reader.dsId)
                            .withFoi(UsgsUtils.toBigId(idScope, dataBlk.getStringValue(1)))
                            .withPhenomenonTime(ts)
                            .withResult(dataBlk)
                            .build();
    
                        var id = UsgsUtils.toObsId(reader.dsId, ts);
                        nextRecords.add(new SimpleEntry<>(id, obs));
                        dataBlk = dataBlk.clone();
                    }
                    
                    if (!nextRecords.isEmpty())
                        return;
                }
                
                if (line == null && !lastBatch)
                    nextBatch();
            }
            while (!lastBatch);
        }
        catch (IOException e)
        {
            logger.error("Error while reading tabular data", e);
        }
    }
    
    
    protected void nextBatch()
    {
        try
        {
            if (originalEndTime != Long.MIN_VALUE)
            {                
                usgsFilter.startTime = new Date(nextBatchStartTime);
                
                // set start time for next batch
                // adjust batch length to avoid a very small batch at the end
                long timeGap = 1000; // gap to avoid duplicated obs
                long adjBatchLength = batchDuration;
                long timeLeft = originalEndTime - nextBatchStartTime;
                if (((double) timeLeft) / batchDuration < 1.5)
                    adjBatchLength = timeLeft + timeGap;
                
                var endTime = nextBatchStartTime + adjBatchLength - timeGap;
                if (endTime >= originalEndTime)
                {
                    endTime = originalEndTime;
                    lastBatch = true;
                }
                usgsFilter.endTime = new Date(endTime);
                                
                logger.debug("Next batch is {} - {}",
                    Instant.ofEpochMilli(nextBatchStartTime),
                    Instant.ofEpochMilli(endTime));
                
                nextBatchStartTime += adjBatchLength;
            }
            else
                lastBatch = true;
            
            String requestUrl = buildInstantValuesRequest(usgsFilter);
            
            logger.debug("Requesting observations: {}", requestUrl);
            URL url = new URL(requestUrl);
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
        }
        catch (IOException e)
        {
            logger.error("Error fetching observation data", e);
        }
    }
    
    
    public Stream<Entry<BigId, IObsData>> getObservations(USGSDataFilter filter, long limit)
    {
        this.usgsFilter = filter;
        this.originalEndTime = filter.endTime != null ? filter.endTime.getTime() : Long.MIN_VALUE;
        this.nextBatchStartTime = filter.startTime != null ? filter.startTime.getTime() : Long.MIN_VALUE;
        this.reader = null;
        this.limit = limit;
        this.lastBatch = false;
        this.nextRecords.clear();
        
        // compute batch size
        // assuming an average of 4 records per hour
        limit = Math.max(Math.min(limit, 6000), 100);
        int numTimeSeries = filter.siteIds.size() * filter.getAllParamCodes().size();
        batchDuration = TimeUnit.MINUTES.toMillis(15) * Math.min(limit, 5000) / numTimeSeries;
        
        nextBatch();
        preloadNext();
        return StreamSupport
            .stream(Spliterators.spliteratorUnknownSize(this, Spliterator.DISTINCT), false)
            .onClose(this::close);
    }
    
    
    protected void close()
    {
        try
        {
            logger.debug("Closing RDB reader");
            if (reader != null)
                reader.close();
        }
        catch (IOException e)
        {
        }
    }


    protected String buildInstantValuesRequest(USGSDataFilter filter)
    {
        StringBuilder sb = UsgsUtils.buildRequestUrl(BASE_URL, filter);
        return sb.toString();
    }


    protected ParamValueParser[] parseHeader(BufferedReader reader, USGSDataFilter filter) throws IOException
    {
        // skip header comments
        String line;
        while ((line = reader.readLine()) != null)
        {
            line = line.trim();
            if (!line.startsWith("#"))
                break;
        }

        // parse field names and prepare corresponding readers
        String[] fieldNames = line.split("\t");
        
        // skip field sizes
        reader.readLine();
        
        // create param readers
        return initParamReaders(filter, fieldNames);        
    }


    protected ParamValueParser[] initParamReaders(USGSDataFilter filter, String[] fieldNames)
    {
        ArrayList<ParamValueParser> readers = new ArrayList<>();
        int i = 0;

        // always add time stamp and site ID readers
        readers.add(new TimeStampParser(2, i++));
        readers.add(new SiteNumParser(1, i++));

        // create a reader for each selected param
        var selectedParams = filter.getAllParamCodes();
        for (int j = 4; j < fieldNames.length; j++)
        {
            var tokens = fieldNames[j].split("_");
            if (tokens.length != 2)
                continue;
            
            var fieldTs = tokens[0];
            var paramCd = tokens[1];
            if (!UsgsUtils.PARAM_CODE_REGEX.matcher(paramCd).matches())
                continue;
            
            // use field only if param code is selected
            if (selectedParams.isEmpty() || selectedParams.contains(paramCd))
            {
                logger.debug("Creating parser for param {} @ index={}", paramCd, j);
                var paramReader = new FloatValueParser(j, i, logger);
                paramReader.dsId = UsgsUtils.toDataStreamId(idScope, fieldTs, paramCd);
                readers.add(paramReader);
            }
        }

        return readers.toArray(new ParamValueParser[0]);
    }
    

    // The following classes are used to parse individual values from the tabular
    // data
    // A list of parsers is built according to the desired output and parsers are
    // applied
    // in sequence to fill the datablock with the proper values

    /*
     * Base value parser class
     */
    static abstract class ParamValueParser
    {
        int fromIndex;
        int toIndex;
        BigId dsId;
        boolean noValue;

        public ParamValueParser(int fromIndex, int toIndex)
        {
            this.fromIndex = fromIndex;
            this.toIndex = toIndex;
        }


        public abstract void parse(String[] tokens, DataBlock data) throws IOException;
    }

    /*
     * Parser for time stamp field, including time zone
     */
    static class TimeStampParser extends ParamValueParser
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm z");
        StringBuilder buf = new StringBuilder();

        public TimeStampParser(int fromIndex, int toIndex)
        {
            super(fromIndex, toIndex);
        }


        public void parse(String[] tokens, DataBlock data) throws IOException
        {
            buf.setLength(0);
            buf.append(tokens[fromIndex].trim());
            buf.append(' ');
            buf.append(tokens[fromIndex + 1].trim());

            try
            {
                long ts = dateFormat.parse(buf.toString()).getTime();
                data.setDoubleValue(toIndex, ts / 1000.);
            }
            catch (ParseException e)
            {
                throw new IOException("Invalid time stamp " + buf.toString());
            }
        }
    }

    /*
     * Parser for site ID
     */
    static class SiteNumParser extends ParamValueParser
    {
        public SiteNumParser(int fromIndex, int toIndex)
        {
            super(fromIndex, toIndex);
        }


        public void parse(String[] tokens, DataBlock data)
        {
            String val = tokens[fromIndex].trim();
            data.setStringValue(toIndex, val);
        }
    }

    /*
     * Parser for floating point value
     */
    static class FloatValueParser extends ParamValueParser
    {
        Logger logger;

        public FloatValueParser(int fromIndex, int toIndex, Logger logger)
        {
            super(fromIndex, toIndex);
            this.logger = logger;
        }

        public void parse(String[] tokens, DataBlock data) throws IOException
        {
            try
            {
                float f = Float.NaN;
                noValue = false;
                
                if (fromIndex >= 0 && fromIndex < tokens.length)
                {
                    String val = tokens[fromIndex].trim();
                    if (!val.isEmpty() && !val.startsWith("*"))
                    {
                        try
                        {
                            f = Float.parseFloat(val);
                        }
                        catch (NumberFormatException e)
                        {
                            //  If value is non-numeric, leave field as NaN
                            logger.trace("Special value: {}", val);
                        }
                    }
                    else
                        noValue = true;
                }

                data.setFloatValue(toIndex, f);
            }
            catch (NumberFormatException e)
            {
                throw new IOException("Invalid numeric value " + tokens[fromIndex], e);
            }
        }
    }
}
