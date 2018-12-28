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
import org.openkinect.freenect.LedStatus;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.kinect.KinectConfig.Mode;
import org.vast.sensorML.SMLHelper;

public class KinectSensor extends AbstractSensorModule<KinectConfig> {

	private static final String ERR_STR_NO_KINECT_DEVICES_FOUND = ": No Kinect devices found";

	private static Context kinectContext = null;
	
	private static Device kinectDevice = null;

	private KinectDepthOutput depthInterface = null;
	
	private KinectVideoOutput cameraInterface = null;

	private static boolean isConnected = false;
	
	private Mode mode = Mode.DEPTH;
	
	private LedStatus ledStatus = LedStatus.BLINK_GREEN;

	@Override
	public void init() throws SensorHubException {

		super.init();

		kinectContext = Freenect.createContext();

		isConnected = true;
		
		mode = getConfiguration().videoMode;
		
		ledStatus = getConfiguration().ledStatus;

		if (0 == kinectContext.numDevices()) {

			throw new SensorHubException(CANNOT_START_MSG + ERR_STR_NO_KINECT_DEVICES_FOUND);

		} else {

			kinectDevice = kinectContext.openDevice(0);

			generateUniqueID("urn:osh:sensor:kinect:", config.serialNumber);

			generateXmlID("KINECT_", config.serialNumber);
			
			if (Mode.DEPTH == mode) {

				depthInterface = new KinectDepthOutput(this, kinectDevice);

				depthInterface.init();

				addOutput(depthInterface, false);

			} else {

				if (Mode.IR == mode) {

					cameraInterface = new KinectVideoOutput(this, kinectDevice);
					
				} else {

					cameraInterface = new KinectInfraredOutput(this, kinectDevice);
				}

				cameraInterface.init();

				addOutput(cameraInterface, false);
			}
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

		// Set Led to configured setting
		kinectDevice.setLed(ledStatus);

		if ((null != depthInterface) && (Mode.DEPTH == mode)) {

			depthInterface.start();

		} else if (null != cameraInterface) {

			cameraInterface.start();
		}
	}

	@Override
	public void stop() throws SensorHubException {

		if (null != cameraInterface) {

			cameraInterface.stop();
		}

		if (null != depthInterface) {

			depthInterface.stop();
		}

		// Turn off LED
		kinectDevice.setLed(LedStatus.OFF);

		kinectDevice.close();

		kinectContext.shutdown();

		isConnected = false;
	}

	@Override
	public boolean isConnected() {

		return isConnected;
	}
}
