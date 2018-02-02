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
import java.nio.file.Paths;
import java.util.ArrayList;
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
import org.sensorhub.impl.sensor.navDb.NavDbEntry.Type;
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
 * @since Nov, 2017
 * 
 * TODO: clean up and remove redundant code
 * 
 */
public class NavDriver extends AbstractSensorModule<NavConfig>  implements IMultiSourceDataProducer  
{
	NavOutput navOutput;
	WaypointOutput wyptOutput;
	NavaidOutput navaidOutput;
	Set<String> foiIDs;
	Map<String, AbstractFeature> navEntryFois;
	//	AbstractFeature airportFoi;
	static final String SENSOR_UID_PREFIX = "urn:osh:sensor:aviation:";
	static final String AIRPORTS_UID_PREFIX = SENSOR_UID_PREFIX + "airports:";
	static final String WAYPOINTS_UID_PREFIX = SENSOR_UID_PREFIX + "waypoints:";
	static final String NAVAID_UID_PREFIX = SENSOR_UID_PREFIX + "navaids:";
	static final Logger log = LoggerFactory.getLogger(NavDriver.class);

	public NavDriver() {
		this.foiIDs = new LinkedHashSet<String>();
		this.navEntryFois = new LinkedHashMap<String, AbstractFeature>();
	}

	@Override
	protected void updateSensorDescription()
	{
		synchronized (sensorDescLock)
		{
			super.updateSensorDescription();
			sensorDescription.setDescription("NavDB offerings");
		}
	}

	boolean airports = true;
	boolean waypoints = true;
	boolean navaids = true;

	@Override
	public void init() throws SensorHubException
	{

		super.init();

		System.err.println("Nav DB Path: " + config.navDbPath);
		// IDs
		this.uniqueID = "urn:osh:sensor:earthcast:navDb";
		this.xmlID = "NavDb";

		// Initialize Outputs
		try {
			if(waypoints) {
				this.wyptOutput = new WaypointOutput(this);
				addOutput(wyptOutput, false);
				wyptOutput.init();
			}

			if(airports) {
				this.navOutput = new NavOutput(this);
				addOutput(navOutput, false);
				navOutput.init();
			}

			if(navaids) {
				this.navaidOutput = new NavaidOutput(this);
				addOutput(navaidOutput, false);
				navaidOutput.init();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
			throw new SensorHubException("Cannot instantiate NavOutput", e);
		}        


	}

	@Override
	public void start() throws SensorHubException
	{
		if(navaids)
			startNavaid();
		if(waypoints)
			startWaypt();
		if(airports)
			startAirports();

	}

	public void startAirports() throws SensorHubException {
		try {
			List<NavDbEntry> airports = LufthansaParser.getDeltaAirports(config.navDbPath, config.deltaAirportsPath);
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

				// no need to send event since we do all this on startup
//				long now = System.currentTimeMillis();
//				eventHandler.publishEvent(new FoiEvent(now, uid, this, airportFoi, now/1000L));
//				log.debug(uid);
			}

			//  Send to output
			navOutput.sendEntries(airports);
		} catch (Exception e) {
			e.printStackTrace();
			throw new SensorHubException("Error parsing NavDB File.  Cannot load.", e);
		} 		
	}


	public void startNavaid() throws SensorHubException
	{
		try {
			List<NavDbEntry> es = LufthansaParser.getNavDbEntries(Paths.get(config.navDbPath));
			List<String> regs = new ArrayList<String>();
			regs.add("USA");
			regs.add("CAN");
			List<NavDbEntry> fregs = LufthansaParser.filterEntries(es, regs);
			List<NavDbEntry> ftype = LufthansaParser.filterEntries(fregs, Type.NAVAID);
			//  add FOIS, one per airport
			SMLHelper smlFac = new SMLHelper();
			GMLFactory gmlFac = new GMLFactory(true);
			for(NavDbEntry navaid: ftype) {
				AbstractFeature navaidFoi = smlFac.newPhysicalSystem();
				navaidFoi.setId(navaid.id);
				String uid = NAVAID_UID_PREFIX + navaid.id;
				navaidFoi.setUniqueIdentifier(uid);
				navaidFoi.setName(navaid.name);
				navaidFoi.setDescription("Navaid Foi for " + navaid.name);
				Point location = gmlFac.newPoint();
				location.setPos(new double [] {navaid.lat, navaid.lon});
				navaidFoi.setLocation(location);

				foiIDs.add(uid);
				navEntryFois.put(uid, navaidFoi);

				// no need to send event since we do all this on startup
//				long now = System.currentTimeMillis();
//				eventHandler.publishEvent(new FoiEvent(now, uid, this, navaidFoi, now/1000L));
//				log.debug(uid);
			}

			//  Send to output
			navaidOutput.sendEntries(ftype);
		} catch (Exception e) {
			throw new SensorHubException("Error parsing NavDB File.  Cannot load.");
		} 
	}

	public void startWaypt() throws SensorHubException
	{
		try {
			List<NavDbEntry> es = LufthansaParser.getNavDbEntries(Paths.get(config.navDbPath));
			List<String> regs = new ArrayList<String>();
			regs.add("USA");
			regs.add("CAN");
			List<NavDbEntry> freg = LufthansaParser.filterEntries(es, regs);
			List<NavDbEntry> ftype = LufthansaParser.filterEntries(freg, Type.WAYPOINT);
			//  add FOIS, one per airport
			SMLHelper smlFac = new SMLHelper();
			GMLFactory gmlFac = new GMLFactory(true);
			for(NavDbEntry wypt: ftype) {

				AbstractFeature wyptFoi = smlFac.newPhysicalSystem();
				wyptFoi.setId(wypt.id);
				String uid = WAYPOINTS_UID_PREFIX + wypt.id;
				wyptFoi.setUniqueIdentifier(uid);
				wyptFoi.setName(wypt.name);
				wyptFoi.setDescription("Navaid Foi for " + wypt.name);
				Point location = gmlFac.newPoint();
				location.setPos(new double [] {wypt.lat, wypt.lon});
				wyptFoi.setLocation(location);

				foiIDs.add(uid);
				navEntryFois.put(uid, wyptFoi);

				// no need to send event since we do all this on startup
//				long now = System.currentTimeMillis();
//				eventHandler.publishEvent(new FoiEvent(now, uid, this, wyptFoi, now/1000L));
//				log.debug(uid);
			}

			//  Send to output
			wyptOutput.sendEntries(ftype);
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
