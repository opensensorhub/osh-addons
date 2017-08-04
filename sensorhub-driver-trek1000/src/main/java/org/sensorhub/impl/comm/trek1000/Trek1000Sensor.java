/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.comm.trek1000;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.comm.rxtx.RxtxSerialCommProvider;
import org.sensorhub.impl.sensor.AbstractSensorModule;

/**
 * <p>
 * Implementation of DecaWave's Trek1000 sensor. This particular class stores 
 * configuration parameters.
 * </p>
 * 
 * @author Joshua Wolfe <developer.wolfe@gmail.com>
 * @since March 24, 2017
 */
public class Trek1000Sensor extends AbstractSensorModule<Trek1000Config>
{
	public final String urn = "urn:osh:sensor:trek1000";
	public final String xmlID = "TREK1000_";
	
	Trek1000Output dataInterface;
	
	public Trek1000Sensor() {}
	
	@Override
	public void setConfiguration(final Trek1000Config config)
	{
		super.setConfiguration(config);
	}
	
	@Override
	public void init() throws SensorHubException
	{
		super.init();
		
		// generate identifiers
		generateUniqueID(urn, config.sensorId);
		generateXmlID(xmlID, config.sensorId);

		// init main data interface
		dataInterface = new Trek1000Output(this);
		addOutput(dataInterface, false);
		dataInterface.init();
	}

	@Override
	public void start() throws SensorHubException
	{
		try {
			dataInterface.start(config);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void stop() throws SensorHubException
	{
		dataInterface.stop();
	}

	@Override
	public String getName()
	{
		return "Trek1000_Sensor";
	}

	@Override
	public boolean isConnected()
	{
		return dataInterface.isConnect();
	}
}
