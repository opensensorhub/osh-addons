/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.
Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2019 Botts Innovative Research, Inc. All Rights Reserved. 

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

import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;

/**
 * Driver implementation supporting Microsoft Kinect Model 1414. The Kinect
 * device supports RGB Video, IR, and Depth point cloud sensors. This driver
 * allows for the usage of a Kinect device in a single mode of operation where
 * the modes are one of "video", "IR", or "depth". Upon connecting to the device
 * and starting the driver the LED indicator on the device is turned on to solid
 * green and turned off when the driver is stopped from the OSH Admin interface.
 * 
 * @author Nicolas "Nick" Garay <nick.garay@botts-inc.com>
 *         <nicolas.garay@icloud.com>
 * @since January 7, 2019
 *
 */
public class KinectSensor extends AbstractSensorModule<KinectConfig> {

	/**
	 * Error string for condition where no Kinect devices found to connect to.
	 */
	private static final String ERR_STR_NO_KINECT_DEVICES_FOUND = ": No Kinect devices found";

	/**
	 * The context by which the driver establishes a connection with the device.
	 */
	private static Context kinectContext = null;

	/**
	 * Device handle by which the driver interacts with the connected device.
	 */
	private static Device kinectDevice = null;

	/**
	 * Manages and produces the output from the depth sensor converting the data to
	 * a usable format for publishing and consumption.
	 */
	private KinectDepthOutput depthInterface = null;

	/**
	 * Manages and produces the output from the video or IR sensor converting the
	 * data to a usable format for publishing and consumption.
	 */
	private KinectVideoOutput cameraInterface = null;

	/**
	 * Flag to indicate the device is connected. Used in state management.
	 */
	private static boolean isConnected = false;

	/**
	 * The mode of operation for the device, by default it is DEPTH.
	 */
	private Mode mode = Mode.DEPTH;

	/**
	 * The status of the LED indicator on the device, by default it is to blink
	 * green. This is the mode in which the LED operates when first connected
	 * through USB to the computing platform.
	 */
	private LedStatus ledStatus = LedStatus.BLINK_GREEN;

	/**
	 * This static load block informs JNA to load the freenect lib from the JAR file
	 * using the registered class loader (org.sensorhub.utils.NativeClassLoader). It
	 * subsequently registers the instance of the library binding it with the
	 * Freenect class containing the native interface calls.
	 */
	static {

//		try {

		NativeLibrary instance = NativeLibrary.getInstance("freenect", ClassLoader.getSystemClassLoader());

//			System.err.println("Loaded " + instance.getName() + " from " + instance.getFile().getCanonicalPath());

		Native.register(org.openkinect.freenect.Freenect.class, instance);

//		} catch (IOException e) {
//			
//			throw new AssertionError(e);
//		}
	}

	@Override
	public void init() throws SensorHubException {

		super.init();

		kinectContext = Freenect.createContext();
//		kinectContext.setLogHandler(new LogHandler() {
//			public void onMessage(Device dev, LogLevel level, String msg) {
//			
//				System.out.println(msg);
//			}
//		});

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

		if (isConnected) {
			
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
	}

	@Override
	public boolean isConnected() {

		return isConnected;
	}
}
