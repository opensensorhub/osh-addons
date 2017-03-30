/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
Developer are Copyright (C) 2016 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.foscam;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.rtpcam.RTPCameraDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>
 * Implementation of sensor interface for Foscam Cameras using IP
 * protocol Based on Foscam v1.0.4 API.
 * </p>
 *
 * @author Lee Butler <labutler10@gmail.com>
 * @since September 2016
 */
public class FoscamDriver extends AbstractSensorModule<FoscamConfig> {
	private static final Logger logger = LoggerFactory.getLogger(FoscamDriver.class);
	private RTPCameraDriver rtpCameraDriver;
	private FoscamPtzControl ptzControlInterface;

	boolean ptzSupported = false;
	String serialNumber;
	String modelNumber;

	public FoscamDriver() {
	}

	@Override
	public void init() throws SensorHubException {
		// reset internal state in case init() was already called
		super.init();
		ptzControlInterface = null;
		ptzSupported = false;

		config.rtp.id = config.id;
		config.rtp.cameraID = config.cameraID;

		// create generic RTPDriver
		rtpCameraDriver = new RTPCameraDriver();
		// init generic RTP driver
		rtpCameraDriver.init(config.rtp);

		// PTZ control and status
		try {
			createPtzInterfaces();
		} catch (IOException e) {
			logger.error("Cannot create ptz interfaces", e);
			throw new SensorException("Cannot create ptz interfaces", e);
		}
	}

	@Override
	public synchronized void start() throws SensorHubException {
		rtpCameraDriver.start();
	}

	@Override
	public synchronized void stop() {
		rtpCameraDriver.stop();

		if (ptzControlInterface != null)
			ptzControlInterface.stop();
	}

	@Override
	protected void updateSensorDescription() {
		synchronized (sensorDescLock) {
			super.updateSensorDescription();
			if (!sensorDescription.isSetDescription())
				sensorDescription.setDescription("Foscam Network Camera");
		}
	}

	@Override
	public boolean isConnected() {
		return rtpCameraDriver.isConnected();
	}

	protected void createPtzInterfaces() throws SensorException, IOException {
		// connect to PTZ URL
		HttpURLConnection conn = null;
		try {
			URL optionsURL = new URL("http://" + config.http.remoteHost + ":" + Integer.toString(config.http.remotePort)
					+ "/cgi-bin/CGIProxy.fcgi?cmd=getDevInfo&usr=" + config.http.user + "&pwd=" + config.http.password);
			conn = (HttpURLConnection) optionsURL.openConnection();
			conn.setConnectTimeout(config.rtp.connection.connectTimeout);
			conn.setReadTimeout(config.rtp.connection.connectTimeout);
			conn.connect();
		} catch (IOException e) {
			throw new SensorException("Cannot connect to camera PTZ service", e);
		}

		// add PTZ controller
		this.ptzControlInterface = new FoscamPtzControl(this);
		addControlInput(ptzControlInterface);
		ptzControlInterface.init();
	}

	@Override
	public void cleanup() {
	}

	@Override
	public Map<String, ISensorDataInterface> getObservationOutputs() {
		return rtpCameraDriver.getObservationOutputs();
	}
}
