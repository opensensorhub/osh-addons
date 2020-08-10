package org.sensorhub.impl.ndbc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.persistence.DataKey;
import org.sensorhub.api.persistence.IDataFilter;
import org.sensorhub.api.persistence.IDataRecord;
import org.sensorhub.api.persistence.IFoiFilter;
import org.sensorhub.api.persistence.IMultiSourceStorage;
import org.sensorhub.api.persistence.IObsFilter;
import org.sensorhub.api.persistence.IObsStorage;
import org.sensorhub.api.persistence.IObsStorageModule;
import org.sensorhub.api.persistence.IRecordStoreInfo;
import org.sensorhub.api.persistence.IStorageModule;
import org.sensorhub.api.persistence.StorageException;
import org.sensorhub.impl.module.AbstractModule;
import org.sensorhub.impl.persistence.FilteredIterator;
import org.sensorhub.impl.persistence.StorageUtils;
import org.vast.sensorML.SMLHelper;
import org.vast.util.Bbox;
import org.vast.util.DateTimeFormat;

import net.opengis.gml.v32.AbstractFeature;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.sensorml.v20.PhysicalSystem;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;

public class NDBCArchive extends AbstractModule<NDBCConfig> implements IObsStorageModule<NDBCConfig>, IMultiSourceStorage<IObsStorage>
{
	static final String BASE_NDBC_URL = "https://sdf.ndbc.noaa.gov";
	static final String IOOS_UID_PREFIX = "urn:ioos:";
	static final String FOI_UID_PREFIX = NDBCArchive.IOOS_UID_PREFIX + "station:wmo:";
	
	Map<String, RecordStore> dataStores = new LinkedHashMap<>();
//	Map<String, BuoyRecordStore> dataStores = new LinkedHashMap<>();
    Map<String, AbstractFeature> fois = new LinkedHashMap<>();
    Bbox foiExtent = new Bbox();
    PhysicalSystem systemDesc;
    Map<String, String[]> sensorOfferings = new LinkedHashMap<>();

	@Override
	public void start() throws SensorHubException
    {
		loadFois();
		initRecordStores();
		initSensorNetworkDescription();
    }
	
	@Override
	public void stop() throws SensorHubException {
		// TODO Auto-generated method stub
		
	}
	
    protected void loadFois()  throws SensorHubException {
    	// request and parse station info
        try
        {
            ObsStationLoader parser = new ObsStationLoader(this);
            parser.loadStations(fois, config);
        }
        catch (Exception e)
        {
            throw new SensorHubException("Error loading station information", e);
        }
	}
    
//    protected void initRecordStores() throws SensorHubException
//    {
//    	for(BuoyParam param: config.parameters) {
//    		switch(param) {
//    		case SEA_WATER_TEMPERATURE:
//        		BuoyRecordStore rs = new WaterTemperatureStore();
//                dataStores.put(rs.getName(), rs);
//        		break;
//        	default:
//        		logger.error("Param unrecognized or unsupported: {}" + param);
//    		} 
//    	}
//    }
    
    protected void initRecordStores() throws SensorHubException
    {
        RecordStore rs = new RecordStore("buoyData", config.parameters);
        dataStores.put(rs.getName(), rs);
    }
//    
    
    protected void initSensorNetworkDescription() throws SensorHubException
    {
        SMLHelper helper = new SMLHelper();
        systemDesc = helper.newPhysicalSystem();
        systemDesc.setUniqueIdentifier(IOOS_UID_PREFIX + "network:ndbc:buoy"); // + config.parameters.iterator().next().toString().toLowerCase());
        systemDesc.setName("NDBC Buoy Data Network");
        systemDesc.setDescription("NDBC automated sensor network for realtime and archive buoy data"); // + getNumFois(null) + " stations across the US");
        
        // add outputs
//        for (BuoyRecordStore rs: dataStores.values())
//            systemDesc.addOutput(rs.getName(), rs.getRecordDescription());
        for (RecordStore rs: dataStores.values())
            systemDesc.addOutput(rs.getName(), rs.getRecordDescription());
    }
    
	@Override
	public Iterator<? extends IDataRecord> getRecordIterator(IDataFilter filter) {
		String recType = filter.getRecordType();
		RecordStore rs = dataStores.get(recType); 
//		BuoyRecordStore rs = dataStores.get(recType); 
		
		// prepare loader to fetch data from NDBC web service
        final ObsRecordLoader loader = new ObsRecordLoader(rs.getRecordDescription(), logger);
        BuoyRecordLoader bloader = new BuoyRecordLoader();
        NDBCConfig config = getNdbcConfig(filter);
        
        // request observations by batch and iterate through them sequentially
//        final long batchLength = TimeUnit.DAYS.toMillis(31); // 31 days
//        final long stopTime = ((long)Math.ceil(ndbcFilter.stopTime.getTime()/1000.))*1000; // round to next second
//        final long stopTime =  config.getStopTime(); // round to next second
        
        class RecordIterator implements Iterator<IDataRecord>
        {                
            Iterator<BuoyDataRecord> batchIt;
            IDataRecord next;
            //long nextBatchStartTime = ndbcFilter.startTime.getTime()/1000*1000; // round to previous second
            long nextBatchStartTime = config.getStartTime(); 
            
            RecordIterator()
            {
                preloadNext();
            }
            
            protected IDataRecord preloadNext()
            {
                IDataRecord current = next;
                next = null;
                
                // retrieve next batch if needed
//                if ((batchIt == null || !batchIt.hasNext()) && nextBatchStartTime <= stopTime)
//                {
                	//  Bypass batching mechanism til I figure it out
//                	ndbcFilter.setStartTime(nextBatchStartTime);
                	config.setStartTime(config.getStartTime());
                    
                    // adjust batch length to avoid a very small batch at the end
	//                    long timeGap = 1000; // gap to avoid duplicated obs
	//                    long adjBatchLength = batchLength;
	//                    long timeLeft = stopTime - nextBatchStartTime;
	//                    if (((double)timeLeft) / batchLength < 1.5)
	//                        adjBatchLength = timeLeft+timeGap;
//                    ndbcFilter.setStopTime(Math.min(nextBatchStartTime + adjBatchLength - timeGap, stopTime));
                    config.setStopTime(config.getStopTime());
                    batchIt = nextBatch(loader, config, recType).iterator();
//                    nextBatchStartTime += adjBatchLength;
//                }
                                
//                if (batchIt != null && batchIt.hasNext())
//                    next = batchIt.next();
                
                return current;
            }
            
            @Override
            public boolean hasNext()
            {
                return (next != null);
            }
            

            @Override
            public IDataRecord next()
            {
                if (!hasNext())
                    throw new NoSuchElementException();
                return preloadNext();
            }
        };
        
        return new RecordIterator();
	}
	
    protected NDBCConfig getNdbcConfig(IDataFilter filter)
    {
        NDBCConfig reqConfig = new NDBCConfig();  
        
        // keep only site IDs that are in both request and config
        if (filter.getProducerIDs() != null)
        	//config.stationIds.addAll(filter.getProducerIDs());
        	config.stationIds.retainAll(filter.getProducerIDs());
        
        if (filter instanceof IObsFilter)
        {
            Collection<String> fois = ((IObsFilter) filter).getFoiIDs();
            if (fois != null)
            {
                for (String foiID: fois)
                	reqConfig.stationIds.add(foiID.substring(FOI_UID_PREFIX.length()));
            }
        }
        if (!config.stationIds.isEmpty())
        	//ndbcFilter.stationIds.retainAll(config.stationIds);
        	reqConfig.stationIds.addAll(config.stationIds);
                    
        // use config params
        reqConfig.parameters.addAll(config.parameters);
        reqConfig.siteBbox.setMinX(config.siteBbox.getMinX());
        reqConfig.siteBbox.setMinY(config.siteBbox.getMinY());
        reqConfig.siteBbox.setMaxX(config.siteBbox.getMaxX());
        reqConfig.siteBbox.setMaxY(config.siteBbox.getMaxY());
        
        // init time filter
        //  NDBC limits requests to 31 days
        // First, check for latest (both time ranges will be infinite)
        if( Double.isInfinite(filter.getTimeStampRange()[0]) && Double.isInfinite(filter.getTimeStampRange()[1]) )  {
        	reqConfig.setStartTime(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1));
        	reqConfig.setStopTime(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1));
        	return config;
        }
        long reqStartTime = (long)(filter.getTimeStampRange()[0] * 1000);
        long reqStopTime = (long)(filter.getTimeStampRange()[1] * 1000);
        System.err.println(reqStopTime - reqStartTime);
        System.err.println(TimeUnit.DAYS.toMillis(config.maxRequestTimeRange));
        if( (reqStopTime - reqStartTime) > TimeUnit.DAYS.toMillis(config.maxRequestTimeRange) ) {
        	reqStartTime = reqStopTime - TimeUnit.DAYS.toMillis(config.maxRequestTimeRange);
        }
        reqConfig.setStartTime(reqStartTime);
        reqConfig.setStopTime(reqStopTime);
        System.err.println("Request Range: "  + reqConfig.startTimeIso + "," + reqConfig.stopTimeIso);
        
//        long configStartTime = config.getStartTime();
//        long configstopTime = config.getStopTime();
//        long filterStartTime = Long.MIN_VALUE;
//        long filterstopTime = Long.MAX_VALUE;
//        if (filter.getTimeStampRange() != null)
//        {
//            filterStartTime = (long)(filter.getTimeStampRange()[0] * 1000);
//            filterstopTime = (long)(filter.getTimeStampRange()[1] * 1000);
//        }
//        
//        config.setStartTime(Math.min(configstopTime, Math.max(configStartTime, filterStartTime)));
//        config.setStopTime(Math.min(configstopTime, Math.max(configStartTime, filterstopTime)));
        
        return reqConfig;
    }
    
    protected Collection<BuoyDataRecord> nextBatch(ObsRecordLoader loader, NDBCConfig config, String recType)
    {
        try
        {
            ArrayList<BuoyDataRecord> records = new ArrayList<>();
            
            // log batch time range
            if (getLogger().isDebugEnabled())
            {
                getLogger().info("Next batch is {} - {}", config.startTimeIso, config.stopTimeIso);
            }
            
            // request and parse next batch
            loader.sendRequest(config);
            while (loader.hasNext())
            {
                DataBlock data = loader.next();
                if (data == null)
                    break;
                DataKey key = new DataKey(recType, data.getDoubleValue(0));
                records.add(new BuoyDataRecord(key, data));
            }
            
            // sort by timestamps
            Collections.sort(records);
            return records;
        }
        catch (IOException e)
        {
        	e.printStackTrace(System.err);
            throw new RuntimeException("Error while sending request for instantaneous values");
        }
    }

    
	@Override
	public AbstractProcess getLatestDataSourceDescription()
	{
		return systemDesc;
	}
	
    @Override
    public List<AbstractProcess> getDataSourceDescriptionHistory(double startTime, double stopTime)
    {
        return Arrays.<AbstractProcess>asList(systemDesc);
    }
    
	@Override
	public AbstractProcess getDataSourceDescriptionAtTime(double time)
	{
		return systemDesc;
	}

    @Override
    public Map<String, ? extends IRecordStoreInfo> getRecordStores()
    {
        return Collections.unmodifiableMap(dataStores);
    }

    //  NOTE:  Don't think this is useful since we aren't actually storing any records
    @Override
    public int getNumMatchingRecords(IDataFilter filter, long maxCount)
    {
        // compute rough estimate here
        NDBCConfig config = getNdbcConfig(filter);
//        long dt = ndbcFilter.stopTime.getTime() - ndbcFilter.startTime.getTime();
        long dt = System.currentTimeMillis() - config.getStartTime();
        long samplingPeriod = TimeUnit.MINUTES.toMillis(15); // shortest sampling period seems to be 15min
        int numSites = config.stationIds.isEmpty() ? fois.size() : config.stationIds.size();
        return (int)(numSites * dt / samplingPeriod);
//    	return 1;
    }


    //  NOTE:  Don't think this is useful since we aren't actually storing any records
    @Override
    public int getNumRecords(String recordType)
    {
        long dt = System.currentTimeMillis() - config.getStartTime();
        long samplingPeriod = TimeUnit.MINUTES.toMillis(15); // shortest sampling period seems to be 15min
        int numSites = fois.size();
        return (int)(numSites * dt / samplingPeriod);
//        return 1;
    }

    //  NOTE: This should be the time range of available data from NDBC 
    @Override
    public double[] getRecordsTimeRange(String recordType)
    {
        double startTime = config.getStartTime() / 1000.;
        double stopTime = System.currentTimeMillis() / 1000.;
        return new double[] {startTime, stopTime};
    }
    
    
    @Override
    public int[] getEstimatedRecordCounts(String recordType, double[] timeStamps)
    {
        return StorageUtils.computeDefaultRecordCounts(this, recordType, timeStamps);
    }


    @Override
    public DataBlock getDataBlock(DataKey key)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public Iterator<DataBlock> getDataBlockIterator(IDataFilter filter)
    {
        final Iterator<? extends IDataRecord> it = getRecordIterator(filter);
        
        return new Iterator<DataBlock>()
        {
            @Override
            public boolean hasNext()
            {
                return it.hasNext();
//                return false;
            }

            @Override
            public DataBlock next()
            {
                return it.next().getData();
            }
        };
    }
    

    @Override
    public int getNumFois(IFoiFilter filter)
    {
        if (filter == null)
            return fois.size();
        
        Iterator<AbstractFeature> it = getFois(filter);
        
        int count = 0;
        while (it.hasNext())
        {
            it.next();
            count++;
        }
        
        return count;
    }


    @Override
    public Bbox getFoisSpatialExtent()
    {
        return foiExtent.copy();
    }


    @Override
    public Iterator<String> getFoiIDs(IFoiFilter filter)
    {
        final Iterator<AbstractFeature> it = getFois(filter);
        
        return new Iterator<String>()
        {
            @Override
            public boolean hasNext()
            {
                return it.hasNext();
            }

            @Override
            public String next()
            {
                return it.next().getUniqueIdentifier();
            }
        };
    }


    @Override
    public Iterator<AbstractFeature> getFois(final IFoiFilter filter)
    {
        Iterator<AbstractFeature> it = fois.values().iterator();
        
        return new FilteredIterator<AbstractFeature>(it)
        {
            @Override
            protected boolean accept(AbstractFeature f)
            {
                return StorageUtils.isFeatureSelected(filter, f);
            }
        };
    }


    @Override
    public Collection<String> getProducerIDs()
    {
        return Collections.unmodifiableSet(fois.keySet());
    }


    @Override
    public IObsStorage getDataStore(String producerID)
    {
        return this;
    }
    
    
    @Override
    public boolean isReadSupported()
    {
        return true;
    }
    
    
    @Override
    public boolean isWriteSupported()
    {
        return false;
    }


    @Override
    public IObsStorage addDataStore(String producerID)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public void storeDataSourceDescription(AbstractProcess process)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public void updateDataSourceDescription(AbstractProcess process)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public void removeDataSourceDescription(double time)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public void removeDataSourceDescriptionHistory(double startTime, double stopTime)
    {
        throw new UnsupportedOperationException();
    }
    

    @Override
    public void addRecordStore(String name, DataComponent recordStructure, DataEncoding recommendedEncoding)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public void storeRecord(DataKey key, DataBlock data)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public void updateRecord(DataKey key, DataBlock data)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public void removeRecord(DataKey key)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public int removeRecords(IDataFilter filter)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public void storeFoi(String producerID, AbstractFeature foi)
    {
        throw new UnsupportedOperationException();
    }
    
    
    @Override
    public void backup(OutputStream os) throws IOException
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public void restore(InputStream is) throws IOException
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public void commit()
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public void rollback()
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public void sync(IStorageModule<?> storage) throws StorageException
    {
        throw new UnsupportedOperationException();
    }
}
