package org.sensorhub.impl.sensor.station.metar;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.IMultiSourceDataProducer;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.station.Station;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.sensorML.SMLHelper;

import net.opengis.gml.v32.AbstractFeature;
import net.opengis.gml.v32.Point;
import net.opengis.gml.v32.impl.GMLFactory;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.sensorml.v20.PhysicalSystem;


public class MetarSensor extends AbstractSensorModule<MetarConfig> implements IMultiSourceDataProducer 
{
	static final Logger log = LoggerFactory.getLogger(MetarSensor.class);
	static final String SENSOR_UID_PREFIX = "urn:osh:sensor:metar:";
	static final String STATION_UID_PREFIX = SENSOR_UID_PREFIX + "station:";

	Set<String> foiIDs;
	Map<String, PhysicalSystem> stationFois;
	Map<String, PhysicalSystem> stationDesc;
	MetarOutput metarInterface;
	private MetarStationMap map;
	
	//   For Emwin operation- deprecate these
	private DirectoryWatcher watcher;
	private Thread watcherThread;
	
	boolean isRealtime = false;
	
	//  For aviationWeather operation
	
	//  For realtime
	
	public MetarSensor()
	{
		this.foiIDs = new LinkedHashSet<String>();
		this.stationFois = new LinkedHashMap<String, PhysicalSystem>();
		this.stationDesc = new LinkedHashMap<String, PhysicalSystem>();
	}


	@Override
	public void init() throws SensorHubException
	{
		super.init();

		// generate IDs
		this.uniqueID = SENSOR_UID_PREFIX + "network";
		this.xmlID = "METAR_NETWORK";

		this.metarInterface = new MetarOutput(this);        
		addOutput(metarInterface, false);
		metarInterface.init();
		
		if(config.aviationWeatherUrl != null) {
			isRealtime = true;
		} else if(config.archiveServerUrl != null) {
			isRealtime = false;
		} else {
			throw new SensorHubException("MetarSensor.init() failed. Must specify either aviationWeatherUrl or archiveServerUrl");
		}
		
	}


	@Override
	protected void updateSensorDescription()
	{
		synchronized (sensorDescLock)
		{
			super.updateSensorDescription();
			sensorDescription.setDescription("METAR weather station network");

			// append href to all stations composing the network
			//			for (String stationID: config.stationIDs)
			Collection<Station> stations = map.getStations();
			for (Station station: stations)
			{
				String name = "station" + station.getId();
				String href = STATION_UID_PREFIX + station.getId();
				((PhysicalSystem)sensorDescription).getComponentList().add(name, href, null);
			}
		}
	}


	@Override
	public void start() throws SensorHubException
	{
		SMLHelper smlFac = new SMLHelper();
		GMLFactory gmlFac = new GMLFactory(true);

		map = null;
		try {
			map = MetarStationMap.getInstance(config.metarStationMapPath);
		} catch (IOException e) {
			throw new SensorHubException("IO Exception trying to load metarStationMap");
		}
		// generate station FOIs and full descriptions
		Collection<Station> stations = map.getStations();
		for (Station station: stations)
		{
			String stationId = station.getId();
			String uid = STATION_UID_PREFIX + stationId;
			String description = "METAR weather station: " + stationId;

			// generate small SensorML for FOI (in this case the system is the FOI)
			PhysicalSystem foi = smlFac.newPhysicalSystem();
			foi.setId(station.getId());
			foi.setUniqueIdentifier(uid);
//			System.err.println("MetarSensor station/uid: " + station + "/" + uid);
			foi.setName(station.getName());
			foi.setDescription(description);

			Point stationLoc = gmlFac.newPoint();
			stationLoc.setPos(new double[] {station.getLat(), station.getLon(), station.getElevation()});
			foi.setLocation(stationLoc);
			stationFois.put(uid, foi);
			foiIDs.add(uid);

			// TODO generate full SensorML for sensor description
			PhysicalSystem sensorDesc = smlFac.newPhysicalSystem();
			sensorDesc.setId("STATION_" + stationId);
			sensorDesc.setUniqueIdentifier(uid);
			sensorDesc.setName(station.getName());
			sensorDesc.setDescription(description);
			stationDesc.put(uid, sensorDesc);
		}

		// Emwin Mode
//		assert Files.exists(Paths.get(config.emwinRoot));
//		try {
//			watcher = new DirectoryWatcher(Paths.get(config.emwinRoot), StandardWatchEventKinds.ENTRY_CREATE);
//			watcher.addListener(metarInterface);
//			watcherThread = new Thread(watcher);
//			watcherThread.start();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		updateSensorDescription();
	}
	
	public Station getStation(String stationId) {
		return map.getStation(stationId);
	}

	@Override
	public void stop() throws SensorHubException
	{
		metarInterface.stop();
	}


	@Override
	public void cleanup() throws SensorHubException
	{

	}


	@Override
	public boolean isConnected()
	{
		return true;
	}


	@Override
	public Collection<String> getEntityIDs()
	{
		return Collections.unmodifiableCollection(stationFois.keySet());
	}


	@Override
	public AbstractFeature getCurrentFeatureOfInterest()
	{
		return null;
	}


	@Override
	public AbstractProcess getCurrentDescription(String entityID)
	{
		return stationDesc.get(entityID);
	}


	@Override
	public double getLastDescriptionUpdate(String entityID)
	{
		return 0;
	}


	@Override
	public AbstractFeature getCurrentFeatureOfInterest(String entityID)
	{
		return stationFois.get(entityID);
	}


	@Override
	public Collection<? extends AbstractFeature> getFeaturesOfInterest()
	{
		return Collections.unmodifiableCollection(stationFois.values());
	}


	@Override
	public Collection<String> getFeaturesOfInterestIDs()
	{
		return Collections.unmodifiableCollection(foiIDs);
	}

	@Override
	public Collection<String> getEntitiesWithFoi(String foiID)
	{
		return Arrays.asList(foiID);
	}
}
