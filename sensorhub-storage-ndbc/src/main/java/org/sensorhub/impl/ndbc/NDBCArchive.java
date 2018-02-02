package org.sensorhub.impl.ndbc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.persistence.DataKey;
import org.sensorhub.api.persistence.IDataFilter;
import org.sensorhub.api.persistence.IDataRecord;
import org.sensorhub.api.persistence.IFoiFilter;
import org.sensorhub.api.persistence.IMultiSourceStorage;
import org.sensorhub.api.persistence.IObsStorage;
import org.sensorhub.api.persistence.IObsStorageModule;
import org.sensorhub.api.persistence.IRecordStoreInfo;
import org.sensorhub.api.persistence.IStorageModule;
import org.sensorhub.api.persistence.StorageException;
import org.sensorhub.impl.module.AbstractModule;
import org.vast.sensorML.SMLHelper;
import org.vast.util.Bbox;

import net.opengis.gml.v32.AbstractFeature;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.sensorml.v20.PhysicalSystem;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;

public class NDBCArchive extends AbstractModule<NDBCConfig> implements IObsStorageModule<NDBCConfig>, IMultiSourceStorage<IObsStorage>
{
	static final String BASE_USGS_URL = "http://sdf.ndbc.noaa.gov";
	static final String UID_PREFIX = "urn:ioos:";
	
	Map<String, RecordStore> dataStores = new LinkedHashMap<>();
    Map<String, AbstractFeature> fois = new LinkedHashMap<>();
    Bbox foiExtent = new Bbox();
    PhysicalSystem systemDesc;
	
	@Override
	public void start() throws SensorHubException
    {
		loadFois();
		for (Entry<String, AbstractFeature> entry : fois.entrySet()) {
    		System.out.println(entry.getKey() + " | " + entry.getValue().getUniqueIdentifier() + " | " + entry.getValue().getName() + " | " + entry.getValue().getLocation());
    	}
		
		initRecordStores();
		for (Entry<String, RecordStore> entry : dataStores.entrySet()) {
    		System.out.println(entry.getKey());
    		System.out.println(entry.getValue().getRecordDescription());
    	}
		
		initSensorNetworkDescription();
		
		RecordStore rs = dataStores.get("buoyData");
		final ObsRecordLoader loader = new ObsRecordLoader(this, rs.getRecordDescription());
		loader.postData();
		
//    	Document doc = Jsoup.connect("http://sdf.ndbc.noaa.gov/stations.shtml").get();
//    	Elements stn = doc.getElementsByClass("stndata"); // Grab the <tr> elements of class "stndata"
//    	List<BuoyStation> stations = new ArrayList<BuoyStation>();
//    	
//    	for (int i = 0; i < stn.size(); i++) {
//        	BuoyStation bs = new BuoyStation();
//    		bs.setId(stn.get(i).select("td").get(0).text());
//    		bs.setName(stn.get(i).select("td").get(1).text());
//    		bs.setOwner(stn.get(i).select("td").get(2).text());
//    		
//        	LocationLatLon bsLoc = new LocationLatLon();
//    		bsLoc.setLat(Double.parseDouble(stn.get(i).select("td").get(3).text()));
//    		bsLoc.setLon(Double.parseDouble(stn.get(i).select("td").get(4).text()));
//    		
//    		bs.setLoc(bsLoc);
//        	String[] sensorStr = new String[stn.get(i).select("td").get(5).text().split(" ").length];
//        	for (int k = 0; k < stn.get(i).select("td").get(5).text().split(" ").length; k++)
//        		sensorStr[k] = stn.get(i).select("td").get(5).text().split(" ")[k].trim();
//        	bs.setSensor(sensorStr);
//        	stations.add(bs); // Got the stations that are currently reporting
//    	}
//    	for (BuoyStation buoy : stations) {
//    		System.out.println(buoy.getId());
//    	}
    }


    protected void loadFois()  throws SensorHubException {
    	// request and parse station info
        try
        {
            ObsStationLoader parser = new ObsStationLoader(this);
            parser.loadStations(fois);
        }
        catch (Exception e)
        {
            throw new SensorHubException("Error loading station information", e);
        }
	}
    
    protected void initRecordStores() throws SensorHubException
    {
        RecordStore rs = new RecordStore("buoyData", config.exposeFilter.parameters);
        dataStores.put(rs.getName(), rs);
    }
    
    protected void initSensorNetworkDescription() throws SensorHubException
    {
        SMLHelper helper = new SMLHelper();
        systemDesc = helper.newPhysicalSystem();
        systemDesc.setUniqueIdentifier(UID_PREFIX + "network");
        systemDesc.setName("USGS Water Data Network");
        systemDesc.setDescription("NDBC automated sensor network collecting buoy data at " + getNumFois(null) + " stations across the US");
        
        // add outputs
        for (RecordStore rs: dataStores.values())
            systemDesc.addOutput(rs.getName(), rs.getRecordDescription());
    }


	public void stop() throws SensorHubException
    {        
    }


	@Override
	public void backup(OutputStream os) throws IOException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void restore(InputStream is) throws IOException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void commit() {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void rollback() {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void sync(IStorageModule<?> storage) throws StorageException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public AbstractProcess getLatestDataSourceDescription() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public List<AbstractProcess> getDataSourceDescriptionHistory(double startTime, double endTime) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public AbstractProcess getDataSourceDescriptionAtTime(double time) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void storeDataSourceDescription(AbstractProcess process) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void updateDataSourceDescription(AbstractProcess process) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void removeDataSourceDescription(double time) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void removeDataSourceDescriptionHistory(double startTime, double endTime) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public Map<String, ? extends IRecordStoreInfo> getRecordStores() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void addRecordStore(String name, DataComponent recordStructure, DataEncoding recommendedEncoding) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public int getNumRecords(String recordType) {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public double[] getRecordsTimeRange(String recordType) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Iterator<double[]> getRecordsTimeClusters(String recordType) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public DataBlock getDataBlock(DataKey key) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Iterator<DataBlock> getDataBlockIterator(IDataFilter filter) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Iterator<? extends IDataRecord> getRecordIterator(IDataFilter filter) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public int getNumMatchingRecords(IDataFilter filter, long maxCount) {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public void storeRecord(DataKey key, DataBlock data) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void updateRecord(DataKey key, DataBlock data) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void removeRecord(DataKey key) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public int removeRecords(IDataFilter filter) {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public boolean isReadSupported() {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public boolean isWriteSupported() {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public int getNumFois(IFoiFilter filter) {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public Bbox getFoisSpatialExtent() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Iterator<String> getFoiIDs(IFoiFilter filter) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Iterator<AbstractFeature> getFois(IFoiFilter filter) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void storeFoi(String producerID, AbstractFeature foi) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public Collection<String> getProducerIDs() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public IObsStorage getDataStore(String producerID) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public IObsStorage addDataStore(String producerID) {
		// TODO Auto-generated method stub
		return null;
	}
}
