/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2012-2017 Sensia Software LLC. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.intellisense;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.IMultiSourceDataProducer;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.sensorML.SMLHelper;

import net.opengis.gml.v32.AbstractFeature;
import net.opengis.gml.v32.Point;
import net.opengis.gml.v32.impl.GMLFactory;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.sensorml.v20.PhysicalSystem;


public class IntellisenseSensor extends AbstractSensorModule<IntellisenseConfig> implements IMultiSourceDataProducer 
{
//	static final Logger log = LoggerFactory.getLogger(IntellisenseSensor.class);
	static final String SENSOR_UID_PREFIX = "urn:osh:sensor:intellisense:";
	static final String DEVICE_UID_PREFIX = SENSOR_UID_PREFIX + "station:";
	
	Set<String> foiIDs;
	Map<String, PhysicalSystem> stationFois;
	Map<String, PhysicalSystem> stationDesc;

	IntellisenseOutput output;
	FloodPoller floodPoller;
	Timer timer;
	long pollingPeriod;
	
	// Test constructor
	public IntellisenseSensor(IntellisenseConfig config) throws SensorHubException {
		this.config = config;
		init();
	}
	
	public IntellisenseSensor() {
	}
	
	@Override
	public void init() throws SensorHubException
	{
		super.init();
		// generate identifiers
		this.uniqueID = SENSOR_UID_PREFIX + "network";
		this.foiIDs = new LinkedHashSet<String>();
		this.stationFois = new LinkedHashMap<String, PhysicalSystem>();
		this.stationDesc = new LinkedHashMap<String, PhysicalSystem>();
		this.output = new IntellisenseOutput(this);
		addOutput(output, false);
		setReportingMode(config.reportingMode);
	}

	@Override
	public void start() throws SensorHubException
	{
		SMLHelper smlFac = new SMLHelper();
		GMLFactory gmlFac = new GMLFactory(true);

		// generate station FOIs and full descriptions
		int cnt = 0;
		for (String deviceId: config.deviceIds)
		{
			String uid = DEVICE_UID_PREFIX + deviceId;
			String description = "Intellisense Flood Sensor.. deviceID, UID: " + deviceId + ", " + uid;
			logger.debug("Adding: " + description);

			// generate small SensorML for FOI (in this case the system is the FOI)
			PhysicalSystem foi = smlFac.newPhysicalSystem();
			foi.setId(deviceId);
			foi.setUniqueIdentifier(uid);
			foi.setName(deviceId); // use name from spreadsheet?
			foi.setDescription(description);

			Point stationLoc = gmlFac.newPoint();
			stationLoc.setPos(new double[] {config.deviceLats[cnt], config.deviceLons[cnt++]});
			foi.setLocation(stationLoc);
			stationFois.put(uid, foi);
			foiIDs.add(uid);

			// TODO generate full SensorML for sensor description
			PhysicalSystem sensorDesc = smlFac.newPhysicalSystem();
			sensorDesc.setId("STATION_" + deviceId);
			sensorDesc.setUniqueIdentifier(uid);
			sensorDesc.setName(deviceId);
			sensorDesc.setDescription(description);
			stationDesc.put(uid, sensorDesc);
		}
	
	}

	@Override
	public void stop() throws SensorHubException
	{
		if (timer != null)
		{
			timer.cancel();
			timer = null;
		}
	}

	public void setReportingMode(int mode) {
		if(floodPoller != null)
			floodPoller.cancel();
		floodPoller = new FloodPoller(config, output, logger);
		switch(mode) {
		case 0: 
		case 1:
			pollingPeriod = (int)TimeUnit.MINUTES.toMillis(10);
			break;
		case 2:
			pollingPeriod = (int)TimeUnit.MINUTES.toMillis(30);
			break;
		case 3:  // for testing
			pollingPeriod = (int)TimeUnit.SECONDS.toMillis(60);
			break;
		default: 
			pollingPeriod = (int)TimeUnit.SECONDS.toMillis(10);
			
		}
		if(timer == null)
			timer = new Timer(true);
	    timer.scheduleAtFixedRate(floodPoller, 0, pollingPeriod);
	}
	
	public static void main(String[] args) throws Exception {
		
		IntellisenseConfig conf = new IntellisenseConfig();
		conf.reportingMode = 3;
		IntellisenseSensor sens = new IntellisenseSensor(conf);
		 //cancel after sometime
        try {
            Thread.sleep(30000000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//        conf.reportingMode = 3;
//        sens.setReportingMode(conf.reportingMode);
//        try {
//            Thread.sleep(6000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//		String token = poller.getAuthToken();
//		System.err.println(token);
//		for(String id: conf.deviceIds) {
//			FloodRecord record = poller.getData(token, id);
//			System.err.println(record);
//		}
	}
	
	@Override
	public boolean isConnected() {
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
