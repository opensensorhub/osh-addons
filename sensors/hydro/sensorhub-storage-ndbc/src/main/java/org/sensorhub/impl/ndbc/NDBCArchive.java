/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2020 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

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
	static final String IOOS_UID_PREFIX = "urn:ioos:";
	static final String FOI_UID_PREFIX = NDBCArchive.IOOS_UID_PREFIX + "station:wmo:";

	Map<String, BuoyRecordStore> dataStores = new LinkedHashMap<>();
	Map<String, AbstractFeature> fois = new LinkedHashMap<>();
    Map<String, ObsPeriod> foiTimeRanges = new ConcurrentHashMap<>();
	Bbox foiExtent = new Bbox();
	PhysicalSystem systemDesc;
	
	Timer capsTimer;
	CapsReaderTask capsTask;  // = new CapsTask("https://sdf.ndbc.noaa.gov/sos/server.php", TimeUnit.MINUTES.toMillis(60L));
	
	@Override
	public void start() throws SensorHubException
	{
		logger = getLogger();
		initRecordStores();
		initSensorNetworkDescription();

		//  Kick off thread for loading FOIS and time ranges
		capsTimer = new Timer();
		capsTask = new CapsReaderTask(config.ndbcUrl, TimeUnit.MINUTES.toMillis(config.foiUpdatePeriodMinutes), updateMetadata);
		capsTask.setBbox(config.siteBbox.getMinY(), config.siteBbox.getMinX(), config.siteBbox.getMaxY(), config.siteBbox.getMaxX());
		capsTimer.scheduleAtFixedRate(capsTask, 0, TimeUnit.MINUTES.toMillis(config.foiUpdatePeriodMinutes));
	}

	@Override
	public void stop() throws SensorHubException {
		if(capsTimer != null)
			capsTimer.cancel();
	}

	//	Callback for CapsReader to update fois and TimeRanges
	//  Check concurrency concerns
	Consumer<List<BuoyMetadata>>  updateMetadata = (mdList)-> {
        for(BuoyMetadata md: mdList) {
        	if(!fois.containsKey(md.name)) {
        		fois.put(md.name, md.foi);
        		logger.info("Adding Buoy FOI: {}" , md.uid);
        	}
      		foiTimeRanges.put(md.uid, new ObsPeriod(md.uid, md.startTime, md.stopTime));
        }
        logger.info("Update Metadata...  {} foiTimeRange entries", foiTimeRanges.size());
    };
	
	protected void initRecordStores() throws SensorHubException
	{
		//  TOODO instantiate All params stores
//		Iterator<BuoyParam> it = config.parameters.iterator();
		BuoyParam [] params = BuoyParam.values();
		BuoyRecordStore store = null;
		for(BuoyParam param: params) {
//			BuoyParam param = it.next();
			switch(param) {
			case SEA_WATER_TEMPERATURE:
				store = new WaterTemperatureStore();
				break;
			case AIR_TEMPERATURE:
				store = new AirTemperatureStore();
				break;
			case SEA_WATER_ELECTRICAL_CONDUCTIVITY:
				store = new ConductivityStore();
				break;
			case AIR_PRESSURE_AT_SEA_LEVEL:
				store = new AirPressureStore();
				break;
			case WINDS:
				store = new WindsStore();
				break;
			case SEA_WATER_SALINITY:  // TODO, add depth support
//				store = new SalinityStore();
//				break;
			case CURRENTS:
			case SEA_FLOOR_DEPTH_BELOW_SEA_SURFACE:
			case WAVES:
			case GPS:
				store = new GpsRecordStore();
				break;
			default:
				logger.error("BuoyParam not recognized: " + param.name());
				continue;
			}
			dataStores.put(store.getName(), store);
		}
	}

	protected void initSensorNetworkDescription() throws SensorHubException
	{
		SMLHelper helper = new SMLHelper();
		systemDesc = helper.newPhysicalSystem();
		systemDesc.setUniqueIdentifier(IOOS_UID_PREFIX + "network:ndbc:buoy"); // + config.parameters.iterator().next().toString().toLowerCase());
		systemDesc.setName("NDBC Buoy Data Network");
		systemDesc.setDescription("NDBC automated sensor network for realtime and archive buoy data"); 

		// add outputs
		for (BuoyRecordStore rs: dataStores.values())
			systemDesc.addOutput(rs.getName(), rs.getRecordDescription());
	}

	@Override
	public Iterator<? extends IDataRecord> getRecordIterator(IDataFilter filter) {
		String recType = filter.getRecordType();
		BuoyParam param = BuoyParam.valueOf(recType);
		BuoyRecordStore store = (BuoyRecordStore)dataStores.get(recType); 

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
				try {
					List<BuoyRecord> recs = bloader.getRecords(config, param);
					while ( bloader.hasNext()) {
						DataBlock block = bloader.next();
						// null check?
						DataKey key = new DataKey(recType, block.getDoubleValue(0));
						buoyDataRecords.add(new BuoyDataRecord(key, block));
					}
				} catch (IOException e) {
					logger.error(e.getMessage());
				}
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
			reqConfig.stationIds.addAll(config.stationIds);

		// use config params
//		reqConfig.parameters.addAll(config.parameters);
		reqConfig.siteBbox.setMinX(config.siteBbox.getMinX());
		reqConfig.siteBbox.setMinY(config.siteBbox.getMinY());
		reqConfig.siteBbox.setMaxX(config.siteBbox.getMaxX());
		reqConfig.siteBbox.setMaxY(config.siteBbox.getMaxY());

		// init time filter
		// First, check for "now" (both time ranges will be infinite)
		if( Double.isInfinite(filter.getTimeStampRange()[0]) && Double.isInfinite(filter.getTimeStampRange()[1]) )  {
			// If now, set to a window and filter the response in BuoyParser
			reqConfig.setStartTime(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(2));
			reqConfig.setStopTime(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1));
			reqConfig.setLatest(true);
			return reqConfig;
		}
		//  TODO: StreamStoragePanel initializes two requests for full time range. 
		//  Limiting max request days to avoid huge requests to NDBC server, but need a better fix
		long reqStartTime = (long)(filter.getTimeStampRange()[0] * 1000);
		long reqStopTime = (long)(filter.getTimeStampRange()[1] * 1000);
		if( (reqStopTime - reqStartTime) > TimeUnit.DAYS.toMillis(config.maxRequestTimeRangeDays) ) {
			reqStartTime = reqStopTime - TimeUnit.DAYS.toMillis(config.maxRequestTimeRangeDays);
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
		// Bypassing check.  Need to limit possible response size differently
		return 1;
		
		// compute rough estimate here
//		NDBCConfig config = getNdbcConfig(filter);
//		long dt = System.currentTimeMillis() - config.getStartTime();
//		long samplingPeriod = TimeUnit.MINUTES.toMillis(15); // shortest sampling period seems to be 15min
//		int numSites = config.stationIds.isEmpty() ? fois.size() : config.stationIds.size();
//		return (int)(numSites * dt / samplingPeriod);
	}


	//  NOTE:  Don't think this is useful since we aren't actually storing any records
	@Override
	public int getNumRecords(String recordType)
	{
		long dt = System.currentTimeMillis() - config.getStartTime();
		long samplingPeriod = TimeUnit.MINUTES.toMillis(15); // shortest sampling period seems to be 15min
		int numSites = fois.size();
		return (int)(numSites * dt / samplingPeriod);
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
    public void updateRecordStore(String name, DataComponent recordStructure)
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
