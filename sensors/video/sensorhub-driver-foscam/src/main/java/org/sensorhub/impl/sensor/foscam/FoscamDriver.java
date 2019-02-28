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
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.comm.RobustIPConnection;
import org.sensorhub.impl.security.ClientAuth;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>
 * Implementation of sensor interface for Foscam Cameras using IP protocol Based
 * on Foscam v1.0.4 API.
 * </p>
 *
 * @author Lee Butler <labutler10@gmail.com>
 * @since September 2016
 */
public class FoscamDriver extends AbstractSensorModule<FoscamConfig> {
	private static final Logger logger = LoggerFactory.getLogger(FoscamDriver.class);
	private FoscamPtzControl ptzControlInterface;
	RobustIPConnection connection;
	FoscamVideoOutput videoDataInterface;

	boolean ptzSupported = false;
	String serialNumber;
	String modelNumber;

	String hostUrl;

	public FoscamDriver() {
	}	

	@Override
	public void init() throws SensorHubException {
		// reset internal state in case init() was already called
		super.init();
		videoDataInterface = null;
		ptzControlInterface = null;
		ptzSupported = false;

		// create connection handler
        this.connection = new RobustIPConnection(this, config.connection, "Foscam Camera")
        {
            public boolean tryConnect() throws IOException
            {
            	// just check host is reachable on specified RTSP port
                return tryConnectTCP(config.rtsp.remoteHost, config.rtsp.remotePort);
            }
        };

		// create I/O objects
		// video output
		videoDataInterface = new FoscamVideoOutput(this);
		videoDataInterface.init();
		addOutput(videoDataInterface, false);

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
		// wait for valid connection to camera
		connection.waitForConnection();

		// start video output
		videoDataInterface.start();

		// if PTZ supported
		if (ptzSupported) {
			ptzControlInterface.start();
		}
	}

	@Override
	public synchronized void stop() {

		if (connection != null)
			connection.cancel();

		if (ptzControlInterface != null)
			ptzControlInterface.stop();

		if (videoDataInterface != null)
			videoDataInterface.stop();
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
	public void setConfiguration(FoscamConfig config) {
		super.setConfiguration(config);

		// use same config for HTTP and RTSP by default
		if (config.rtsp.localAddress == null)
			config.rtsp.localAddress = config.http.localAddress;
		if (config.rtsp.remoteHost == null)
			config.rtsp.remoteHost = config.http.remoteHost;
		if (config.rtsp.user == null)
			config.rtsp.user = config.http.user;
		if (config.rtsp.password == null)
			config.rtsp.password = config.http.password;

		// compute full host URL
		hostUrl = "http://" + config.http.remoteHost + ":" + config.http.remotePort + "/cgi-bin";
	};

	@Override
	public boolean isConnected() {
		if (connection == null)
			return false;

		return connection.isConnected();
	}

	protected void createPtzInterfaces() throws SensorException, IOException {
		// connect to PTZ URL
		HttpURLConnection conn = null;
		try {
			URL optionsURL = new URL("http://" + config.http.remoteHost + ":" + Integer.toString(config.http.remotePort)
					+ "/cgi-bin/CGIProxy.fcgi?cmd=getDevInfo&usr=" + config.http.user + "&pwd=" + config.http.password);
			conn = (HttpURLConnection) optionsURL.openConnection();
			conn.setConnectTimeout(config.connection.connectTimeout);
			conn.setReadTimeout(config.connection.connectTimeout);
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

	private void setAuth() {
		ClientAuth.getInstance().setUser(config.http.user);
		if (config.http.password != null)
			ClientAuth.getInstance().setPassword(config.http.password.toCharArray());
	}

	protected String getHostUrl() {
		setAuth();
		return hostUrl;
	}

}
