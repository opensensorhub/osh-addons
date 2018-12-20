/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2017 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.kinect;

import org.openkinect.freenect.Context;
import org.openkinect.freenect.Device;
import org.openkinect.freenect.Freenect;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.vast.sensorML.SMLHelper;

public class KinectSensor extends AbstractSensorModule<KinectConfig> {

	private static Context kinectContext = null;
	private static Device kinectDevice = null;

	private static final String ERR_NO_KINECT_DEVICES_FOUND_STR = ": No Kinect devices found";

	private KinectInfraredOutput irInterface;
	private KinectDepthOutput depthInterface;
	private KinectCameraOutput cameraInterface;

	private static boolean isConnected = false;
	
	private KinectDeviceParams deviceParams = new KinectDeviceParams();

	@Override
	public void init() throws SensorHubException {

		super.init();

		kinectContext = Freenect.createContext();

		isConnected = true;

		if (0 == kinectContext.numDevices()) {

			throw new SensorHubException(CANNOT_START_MSG + ERR_NO_KINECT_DEVICES_FOUND_STR);

		} else {

			kinectDevice = kinectContext.openDevice(0);

			generateUniqueID("urn:osh:sensor:kinect:", config.serialNumber);
			generateXmlID("KINECT_", config.serialNumber);

			irInterface = new KinectInfraredOutput(this, kinectDevice);
			addOutput(irInterface, false);
			irInterface.init();

			depthInterface = new KinectDepthOutput(this, kinectDevice);
			addOutput(depthInterface, false);
			depthInterface.init();

			cameraInterface = new KinectCameraOutput(this, kinectDevice);
			addOutput(cameraInterface, false);
			cameraInterface.init();
		}
	}

	@Override
	protected void updateSensorDescription() {

		synchronized (sensorDescLock) {

			super.updateSensorDescription();

			if (!sensorDescription.isSetDescription()) {

				sensorDescription.setDescription(
						"Kinect sensor providing IR, RGB, and Point Cloud data from device camera and depth sensor");
			}

			SMLHelper helper = new SMLHelper(sensorDescription);

			helper.addSerialNumber(config.serialNumber);
		}
	}

	@Override
	public void start() throws SensorHubException {

		if (null != cameraInterface) {

			cameraInterface.start();
		}

		if (null != irInterface) {

			irInterface.start();
		}

		if (null != depthInterface) {

			depthInterface.start();
		}
	}

	@Override
	public void stop() throws SensorHubException {

		if (null != cameraInterface) {

			cameraInterface.stop();
		}

		if (null != irInterface) {

			irInterface.stop();
		}

		if (null != depthInterface) {

			depthInterface.stop();
		}

		kinectContext.shutdown();

		isConnected = false;
	}

	@Override
	public boolean isConnected() {

		return isConnected;
	}
	
	protected KinectDeviceParams getDeviceParams() {
		
		return deviceParams;
	}
	
	protected void updatedCameraParams() throws SensorException {
		
		if (null != cameraInterface) {

			cameraInterface.stop();
			cameraInterface.init();
			cameraInterface.start();
		}
	}
	
	protected void updatedInfraredParams() throws SensorException {
		
		if (null != irInterface) {

			irInterface.stop();
			irInterface.init();
			irInterface.start();
		}
	}
	
	protected void updatedDepthParams() {
		
		if (null != depthInterface) {

			depthInterface.stop();
			depthInterface.init();
			depthInterface.start();
		}
	}
}
