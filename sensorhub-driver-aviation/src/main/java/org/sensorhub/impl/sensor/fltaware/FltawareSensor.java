/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2012-2017 Sensia Software LLC. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.fltaware;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;

/**
 * 
 * @author tcook
 * @since Sep 13, 2017
 * 
 * TODO:  Need a thread that monitors the watcherThread and restarts it if it dies
 *
 */
public class FltawareSensor extends AbstractSensorModule<FltawareConfig> 
{
	FlightPlanOutput fltawareInterface;
	Thread watcherThread;

	@Override
	public void init() throws SensorHubException
	{
		super.init();

		System.err.println("dataPath: " + config.dataPath);
		// IDs
		this.uniqueID = "urn:osh:sensor:earthcast:flightPlan";
		this.xmlID = "EarthcastFltaware";

		// Initialize interface
		this.fltawareInterface = new FlightPlanOutput(this);

		addOutput(fltawareInterface, false);
		fltawareInterface.init();
	}

	@Override
	public void start() throws SensorHubException
	{
//		String dummyPlan = {"pitr":"1506547848","type":"flightplan","ident":"DAL1646","aircrafttype":"MD90","alt":"31000","dest":"KCMH","edt":"1506544980","eta":"1506548880","facility_hash":"4d2c96d17587e273143d08a2c8c6952808763061","facility_name":"Indianapolis Center","fdt":"1506543720","hexid":"ACFC4B","id":"DAL1646-1506317145-airline-0176","orig":"KATL","reg":"N936DN","route":"KATL.PADGT2.SMTTH.Q67.JONEN..HNN.BREMN5.KCMH/2148","speed":"457","status":"A","waypoints":[{"lat":33.64000,"lon":-84.43000},{"lat":33.68000,"lon":-84.18000},{"lat":33.85000,"lon":-84.18000},{"lat":33.94000,"lon":-84.23000},{"lat":34.14000,"lon":-84.33000},{"lat":34.15000,"lon":-84.33000},{"lat":34.30000,"lon":-84.40000},{"lat":34.71000,"lon":-84.30000},{"lat":34.77000,"lon":-84.29000},{"lat":35.12000,"lon":-84.20000},{"lat":35.28000,"lon":-84.16000},{"lat":35.91000,"lon":-84.01000},{"lat":36.68000,"lon":-83.46000},{"lat":36.76000,"lon":-83.39000},{"lat":36.99000,"lon":-83.23000},{"lat":37.25000,"lon":-83.03000},{"lat":37.85000,"lon":-82.63000},{"lat":37.99000,"lon":-82.55000},{"lat":38.07000,"lon":-82.49000},{"lat":38.32000,"lon":-82.32000},{"lat":38.75000,"lon":-82.03000},{"lat":38.86000,"lon":-82.07000},{"lat":38.91000,"lon":-82.09000},{"lat":38.94000,"lon":-82.10000},{"lat":39.42000,"lon":-82.29000},{"lat":39.48000,"lon":-82.32000},{"lat":39.51000,"lon":-82.33000},{"lat":39.59000,"lon":-82.36000},{"lat":39.63000,"lon":-82.38000},{"lat":39.70000,"lon":-82.40000},{"lat":39.78000,"lon":-82.53000},{"lat":39.88000,"lon":-82.70000},{"lat":39.93000,"lon":-82.78000},{"lat":39.98000,"lon":-82.87000},{"lat":40.00000,"lon":-82.89000}],"ete":"3900"}
		//  The Fltaware Firehose process should be on? OR do we start it here.  Needs to run 24/7, but if not here then we missing data
		FlightObject obj = FlightObject.getSampleFlightPlan();
		fltawareInterface.sendMeasurement(obj);
	}


	@Override
	public void stop() throws SensorHubException
	{

	}


	@Override
	public boolean isConnected() {
		// TODO Auto-generated method stub
		return false;
	}
}
