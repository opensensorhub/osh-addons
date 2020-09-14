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
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.persistence.DataKey;
import org.sensorhub.api.persistence.FoiFilter;
import org.sensorhub.api.persistence.IDataFilter;
import org.sensorhub.api.persistence.IDataRecord;
import org.sensorhub.api.persistence.IFoiFilter;
import org.sensorhub.api.persistence.IMultiSourceStorage;
import org.sensorhub.api.persistence.IObsFilter;
import org.sensorhub.api.persistence.IObsStorage;
import org.sensorhub.api.persistence.IObsStorageModule;
import org.sensorhub.api.persistence.IRecordStoreInfo;
import org.sensorhub.api.persistence.IStorageModule;
import org.sensorhub.api.persistence.ObsPeriod;
import org.sensorhub.api.persistence.StorageException;
import org.sensorhub.impl.module.AbstractModule;
import org.sensorhub.impl.persistence.FilteredIterator;
import org.sensorhub.impl.persistence.IteratorWrapper;
import org.sensorhub.impl.persistence.StorageUtils;
import org.vast.sensorML.SMLHelper;
import org.vast.util.Bbox;

import com.vividsolutions.jts.geom.Polygon;

import net.opengis.gml.v32.AbstractFeature;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.sensorml.v20.PhysicalSystem;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;

public class NDBCArchive extends AbstractModule<NDBCConfig> implements IObsStorageModule<NDBCConfig>, IMultiSourceStorage<IObsStorage>
{
//	static final String BASE_NDBC_URL;
	static final String IOOS_UID_PREFIX = "urn:ioos:";
	static final String FOI_UID_PREFIX = NDBCArchive.IOOS_UID_PREFIX + "station:wmo:";

	//	Map<String, RecordStore> dataStores = new LinkedHashMap<>();
	Map<String, BuoyRecordStore> dataStores = new LinkedHashMap<>();
	Map<String, AbstractFeature> fois = new LinkedHashMap<>();
	Bbox foiExtent = new Bbox();
	PhysicalSystem systemDesc;
	Map<String, String[]> sensorOfferings = new LinkedHashMap<>();
	
	Timer capsTimer;
	CapsTask capsTask;  // = new CapsTask("https://sdf.ndbc.noaa.gov/sos/server.php", TimeUnit.MINUTES.toMillis(60L));
    Map<String, ObsPeriod> foiTimeRanges;
	
	@Override
	public void start() throws SensorHubException
	{
		loadFois();
		initRecordStores();
		initSensorNetworkDescription();
		
		//  Kick off thread for loading FOI time ranges
		capsTimer = new Timer();
		capsTask = new CapsTask(config.ndbcUrl, TimeUnit.MINUTES.toMillis(config.foiUpdatePeriodMinutes));
		capsTask.setBbox(config.siteBbox.getMinY(), config.siteBbox.getMinX(), config.siteBbox.getMaxY(), config.siteBbox.getMaxX());
//		capsTask.setBbox(31.0, -120.0, 35.0, -115.0);
		capsTimer.scheduleAtFixedRate(capsTask, 0, config.foiUpdatePeriodMinutes);
	}

	@Override
	public void stop() throws SensorHubException {
		if(capsTimer != null)
			capsTimer.cancel();
	}

	//  TODO - modify to get these from caps instead of requesting observations
	protected void loadFois()  throws SensorHubException {
		try
		{
			ObsStationLoader parser = new ObsStationLoader(config.ndbcUrl, logger);
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
		//  TOODO instantiate All params stores
		Iterator<BuoyParam> it = config.parameters.iterator();
		BuoyRecordStore store = null;
		while(it.hasNext()) {
			BuoyParam param = it.next();
			switch(param) {
			case SEA_WATER_TEMPERATURE:
				store = new WaterTemperatureStore();
				break;
			case AIR_TEMPERATURE:
				store = new AirTemperatureStore();
				break;
			case AIR_PRESSURE_AT_SEA_LEVEL:
			case SEA_WATER_ELECTRICAL_CONDUCTIVITY:
				store = new ConductivityStore();
				break;
			case SEA_WATER_SALINITY:
			case CURRENTS:
			case ENVIRONMENTAL:
			case SEA_FLOOR_DEPTH_BELOW_SEA_SURFACE:
			case WAVES:
			case WINDS:
				logger.info("BuoyParam not yet supported: " + param.name());
				continue;
			default:
				logger.error("BuoyParam not recognized: " + param.name());
				continue;
			}
			dataStores.put(store.getName(), store);
			
		}
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
		for (BuoyRecordStore rs: dataStores.values())
			systemDesc.addOutput(rs.getName(), rs.getRecordDescription());
	}

	@Override
	public Iterator<? extends IDataRecord> getRecordIterator(IDataFilter filter) {
		String recType = filter.getRecordType();
		//		RecordStore rs = dataStores.get(recType); 
		BuoyRecordStore store = (BuoyRecordStore)dataStores.get(recType); 

		// prepare loader to fetch data from NDBC web service
		//        final ObsRecordLoader loader = new ObsRecordLoader(rs.getRecordDescription(), logger);
		BuoyRecordLoader bloader = new BuoyRecordLoader(store.getRecordDescription());
		NDBCConfig config = getNdbcConfig(filter);

		class RecordIterator implements Iterator<IDataRecord>
		{                
			ArrayList<BuoyDataRecord> buoyDataRecords = new ArrayList<>();
			Iterator<BuoyDataRecord>  iterator;

			RecordIterator()
			{
				loadRecords();
				iterator = buoyDataRecords.iterator();

			}

			protected void loadRecords()
			{
				//                	Iterator<DataBlock> recIterator;
//				config.setStartTime(config.getStartTime());
//				config.setStopTime(config.getStopTime());
				try {
					List<BuoyRecord> recs = bloader.getRecords(config);
					while ( bloader.hasNext()) {
						DataBlock block = bloader.next();
						// null check?
						DataKey key = new DataKey(recType, block.getDoubleValue(0));
						buoyDataRecords.add(new BuoyDataRecord(key, block));
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				Collections.sort(buoyDataRecords);
			}

			@Override
			public boolean hasNext()
			{
				return iterator.hasNext();
			}


			@Override
			public IDataRecord next()
			{
				if (!hasNext())
					throw new NoSuchElementException();
				return iterator.next();
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
			return reqConfig;
		}
		long reqStartTime = (long)(filter.getTimeStampRange()[0] * 1000);
		long reqStopTime = (long)(filter.getTimeStampRange()[1] * 1000);
		if( (reqStopTime - reqStartTime) > TimeUnit.DAYS.toMillis(config.maxRequestTimeRange) ) {
			reqStartTime = reqStopTime - TimeUnit.DAYS.toMillis(config.maxRequestTimeRange);
		}
		reqConfig.setStartTime(reqStartTime);
		reqConfig.setStopTime(reqStopTime);
		logger.info("Request Range: {} {}" , reqConfig.startTimeIso, reqConfig.stopTimeIso);

		return reqConfig;
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

	//  NOTE: In theory this should correspond to full records in storage
	//    but we aren't storing records, so just make it enough to pass 
	//    boolean TimeExtent.intersects(TimeExtent) check
	@Override
	public double[] getRecordsTimeRange(String recordType)
	{
		double startTime = (config.getStartTime() - TimeUnit.DAYS.toMillis(1)) / 1000.;
		double stopTime = (System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)) / 1000.;
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
    
    @Override
    public Iterator<ObsPeriod> getFoiTimeRanges(IObsFilter filter)
    {
        FoiFilter foiFilter = new FoiFilter()
        {
            public Set<String> getFeatureIDs() { return filter.getFoiIDs(); }
            public Polygon getRoi() { return filter.getRoi(); }
        };
                
        return new IteratorWrapper<String, ObsPeriod>(getFoiIDs(foiFilter))
        {
            @Override
            protected ObsPeriod process(String foiID)
            {
            	ObsPeriod obsPeriod = foiTimeRanges.get(foiID);
            	if(obsPeriod != null)
            		return obsPeriod;
                return new ObsPeriod(foiID, Double.NaN, Double.NaN);
            }            
        };
    }
}
