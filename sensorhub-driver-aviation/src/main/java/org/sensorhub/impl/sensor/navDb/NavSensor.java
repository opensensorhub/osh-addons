/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2012-2017 Sensia Software LLC. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.navDb;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.FoiEvent;
import org.sensorhub.api.data.IMultiSourceDataProducer;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.flightAware.FlightAwareSensor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.sensorML.SMLHelper;

import net.opengis.gml.v32.AbstractFeature;
import net.opengis.gml.v32.Point;
import net.opengis.gml.v32.impl.GMLFactory;
import net.opengis.sensorml.v20.AbstractProcess;

/**
 * 
 * @author tcook
 * @since Sep 13, 2017
 * 
 * TODO:  Need a thread that monitors the watcherThread and restarts it if it dies
 *
 */
public class NavSensor extends AbstractSensorModule<NavConfig>  implements IMultiSourceDataProducer  
{
	NavOutput navOutput;
	Set<String> foiIDs;
	Map<String, AbstractFeature> navEntryFois;
//	AbstractFeature airportFoi;
	static final String SENSOR_UID_PREFIX = "urn:osh:sensor:aviation:";
	static final String AIRPORTS_UID_PREFIX = SENSOR_UID_PREFIX + "airports:";
	static final Logger log = LoggerFactory.getLogger(NavSensor.class);
	
	public NavSensor() {
		this.foiIDs = new LinkedHashSet<String>();
		this.navEntryFois = new LinkedHashMap<String, AbstractFeature>();
	}

	@Override
	public void init() throws SensorHubException
	{
		super.init();

		System.err.println("Nav DB Path: " + config.navDbPath);
		// IDs
		this.uniqueID = "urn:osh:sensor:earthcast:navDb";
		this.xmlID = "NavDb";

		// Initialize interface
		try {
			this.navOutput = new NavOutput(this);
			navOutput.init();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new SensorHubException("Cannot instantiate NavOutput", e);
		}        
		addOutput(navOutput, false);


	}

	@Override
	public void start() throws SensorHubException
	{
		try {
			List<NavDbEntry> airports = LufthansaParserOld.getDeltaAirports(config.navDbPath, config.deltaAirportsPath);
			//  add FOIS, one per airport
			SMLHelper smlFac = new SMLHelper();
			GMLFactory gmlFac = new GMLFactory(true);
			for(NavDbEntry airport: airports) {

				AbstractFeature airportFoi = smlFac.newPhysicalSystem();
				airportFoi.setId(airport.icao);
				String uid = AIRPORTS_UID_PREFIX + airport.icao;
				airportFoi.setUniqueIdentifier(uid);
				airportFoi.setName(airport.name);
				airportFoi.setDescription("Airport Foi for " + airport.name);
				Point location = gmlFac.newPoint();
				location.setPos(new double [] {airport.lat, airport.lon});
				airportFoi.setLocation(location);
				
				foiIDs.add(uid);
				navEntryFois.put(uid, airportFoi);
				
				// send event
				long now = System.currentTimeMillis();
				eventHandler.publishEvent(new FoiEvent(now, uid, this, airportFoi, now));
				log.debug(uid);
			}

			//  Send to output
			navOutput.sendEntries(airports);
		} catch (Exception e) {
			throw new SensorHubException("Error parsing NavDB File.  Cannot load.");
		} 
	}


	@Override
	public void stop() throws SensorHubException
	{
	}


	@Override
	public boolean isConnected() {
		return false;
	}

	@Override
	public AbstractFeature getCurrentFeatureOfInterest()
	{
//		return airportFoi;
		return null;
	}


	@Override
	public Collection<String> getEntityIDs()
	{
		return Collections.unmodifiableCollection(navEntryFois.keySet());
	}


	@Override
	public AbstractProcess getCurrentDescription(String entityID)
	{
		return null;
	}


	@Override
	public double getLastDescriptionUpdate(String entityID)
	{
		return 0;
	}


	@Override
	public AbstractFeature getCurrentFeatureOfInterest(String entityID)
	{
		return navEntryFois.get(entityID);
	}


	@Override
	public Collection<? extends AbstractFeature> getFeaturesOfInterest()
	{
		return Collections.unmodifiableCollection(navEntryFois.values());
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
