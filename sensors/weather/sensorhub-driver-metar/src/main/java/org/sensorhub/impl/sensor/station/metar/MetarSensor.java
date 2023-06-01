package org.sensorhub.impl.sensor.station.metar;

import java.io.IOException;
import java.util.Collection;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.station.Station;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.sensorML.SMLHelper;
import net.opengis.gml.v32.Point;
import net.opengis.gml.v32.impl.GMLFactory;
import net.opengis.sensorml.v20.PhysicalSystem;


public class MetarSensor extends AbstractSensorModule<MetarConfig>
{
	static final Logger log = LoggerFactory.getLogger(MetarSensor.class);
	static final String SENSOR_UID_PREFIX = "urn:osh:sensor:metar:";
	static final String STATION_UID_PREFIX = SENSOR_UID_PREFIX + "station:";

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
	}


	@Override
    protected void doInit() throws SensorHubException
	{
		super.doInit();

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
		
		// generate station FOIs, loading info from file
		SMLHelper smlFac = new SMLHelper();
        GMLFactory gmlFac = new GMLFactory(true);

        map = null;
        try {
            map = MetarStationMap.getInstance(config.metarStationMapPath);
        } catch (IOException e) {
            throw new SensorHubException("IO Exception trying to load metarStationMap");
        }
        
        Collection<Station> stations = map.getStations();
        for (Station station: stations)
        {
            String stationId = station.getId();
            //  Only load what we want for SCIRA
            boolean keepStation = false;
            for(String id: config.stationIds) {
                if(id.equalsIgnoreCase(stationId)) {
                    keepStation = true;
                    break;
                }
            }
            if(!keepStation)
                continue;
            String uid = STATION_UID_PREFIX + stationId;
            String description = "METAR weather station: " + stationId;
            log.debug("Adding: " + description);

            // generate small SensorML for FOI (in this case the system is the FOI)
            PhysicalSystem foi = smlFac.newPhysicalSystem();
            foi.setId(station.getId());
            foi.setUniqueIdentifier(uid);
            logger.debug("MetarSensor station/uid: {}/{}" , station, uid);
            foi.setName(station.getName());
            foi.setDescription(description);

            Point stationLoc = gmlFac.newPoint();
            stationLoc.setPos(new double[] {station.getLat(), station.getLon(), station.getElevation()});
            foi.setLocation(stationLoc);
            addFoi(foi);
        }
	}


	@Override
	protected void updateSensorDescription()
	{
		synchronized (sensorDescLock)
		{
			super.updateSensorDescription();
			sensorDescription.setDescription("METAR weather station network");
		}
	}


	@Override
	protected void doStart() throws SensorHubException
	{	
		metarInterface.start();

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
	protected void doStop() throws SensorHubException
	{
		metarInterface.stop();
	}


	@Override
	public boolean isConnected()
	{
		return true;
	}
}
