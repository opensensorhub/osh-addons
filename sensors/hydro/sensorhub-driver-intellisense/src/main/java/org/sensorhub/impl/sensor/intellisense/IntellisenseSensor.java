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

import java.util.Timer;
import java.util.concurrent.TimeUnit;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.vast.sensorML.SMLHelper;

import net.opengis.gml.v32.Point;
import net.opengis.gml.v32.impl.GMLFactory;
import net.opengis.sensorml.v20.PhysicalSystem;


public class IntellisenseSensor extends AbstractSensorModule<IntellisenseConfig>
{
//	static final Logger log = LoggerFactory.getLogger(IntellisenseSensor.class);
	static final String SENSOR_UID_PREFIX = "urn:osh:sensor:intellisense:";
	static final String DEVICE_UID_PREFIX = SENSOR_UID_PREFIX + "station:";
	
	IntellisenseOutput output;
	FloodPoller floodPoller;
	Timer timer;
	long pollingPeriod;
	
	// Test constructor
	protected IntellisenseSensor(IntellisenseConfig config) throws SensorHubException {
		this.config = config;
		doInit();
	}
	
	public IntellisenseSensor() {
	}
	
	@Override
    protected void doInit() throws SensorHubException
	{
		super.doInit();
		
		// generate identifiers
		this.uniqueID = SENSOR_UID_PREFIX + "network";
		this.output = new IntellisenseOutput(this);
		addOutput(output, false);
				
		// generate station FOIs and full descriptions
        int cnt = 0;
        SMLHelper smlFac = new SMLHelper();
        GMLFactory gmlFac = new GMLFactory(true);
        for (String deviceId: config.deviceIds)
        {
            String uid = DEVICE_UID_PREFIX + deviceId;
            String description = "Intellisense Flood Sensor.. deviceID, UID: " + deviceId + ", " + uid;

            // generate small SensorML for FOI (in this case the system is the FOI)
            PhysicalSystem foi = smlFac.newPhysicalSystem();
            foi.setId(deviceId);
            foi.setUniqueIdentifier(uid);
            foi.setName(deviceId); // use name from spreadsheet?
            foi.setDescription(description);

            Point stationLoc = gmlFac.newPoint();
            stationLoc.setPos(new double[] {config.deviceLats[cnt], config.deviceLons[cnt++]});
            foi.setLocation(stationLoc);
            addFoi(foi);
        }
	}

	@Override
	protected void doStart() throws SensorHubException
	{
	    setReportingMode(config.reportingMode);
	}

	@Override
	protected void doStop() throws SensorHubException
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
}
