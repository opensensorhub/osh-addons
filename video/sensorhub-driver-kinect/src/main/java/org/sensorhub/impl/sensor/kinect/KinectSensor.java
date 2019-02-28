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

import net.opengis.sensorml.v20.PhysicalSystem;
import net.opengis.sensorml.v20.SpatialFrame;
import net.opengis.sensorml.v20.impl.SpatialFrameImpl;

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
	private static final String ERR_STR_NO_KINECT_DEVICES_FOUND = new String(": No Kinect devices found");

	/**
	 * Describes the sensor, used in SensorML document
	 */
	private static final String STR_SENSOR_DESCRIPTION = new String(
			"Kinect sensor providing IR, RGB, and Point Cloud data from device camera and depth sensor");

	/**
	 * The context by which the driver establishes a connection with the device.
	 */
	private static Context kinectContext = null;

	/**
	 * Device handle by which the driver interacts with the connected device.
	 */
	private static Device kinectDevice = null;

	/**
	 * Manages and produces the output from the sensor converting the data to a
	 * usable format for publishing and consumption.
	 */
	KinectOutputInterface outputInterface = null;

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

	private double tiltAngle = 0.0;

	/**
	 * This static load block informs JNA to load the freenect lib from the JAR file
	 * using the registered class loader (org.sensorhub.utils.NativeClassLoader). It
	 * subsequently registers the instance of the library binding it with the
	 * Freenect class containing the native interface calls.
	 */
	static {

		NativeLibrary instance = NativeLibrary.getInstance("freenect", ClassLoader.getSystemClassLoader());

		Native.register(org.openkinect.freenect.Freenect.class, instance);
	}

	@Override
	public void init() throws SensorHubException {

		super.init();

		kinectContext = Freenect.createContext();

		isConnected = true;

		mode = getConfiguration().videoMode;

		ledStatus = getConfiguration().ledStatus;

		tiltAngle = getConfiguration().tiltAngle;

		if (tiltAngle < -27.0) {

			tiltAngle = -27.0;

		} else if (tiltAngle > 27.0) {

			tiltAngle = 27.0;
		}

		if (0 == kinectContext.numDevices()) {

			throw new SensorHubException(CANNOT_START_MSG + ERR_STR_NO_KINECT_DEVICES_FOUND);

		} else {

			kinectDevice = kinectContext.openDevice(0);

			generateUniqueID("urn:osh:sensor:kinect:", config.serialNumber);

			generateXmlID("KINECT_", config.serialNumber);

			if (Mode.DEPTH == mode) {

				if (getConfiguration().useCameraModel) {

					outputInterface = new KinectDepthMappedOutput(this, kinectDevice);

				} else {

					outputInterface = new KinectDepthOutput(this, kinectDevice);
				}

			} else if (Mode.VIDEO == mode) {

				if (getConfiguration().jpegOutput) {

					outputInterface = new KinectVideoOutputMJPEG(this, kinectDevice);

				} else {

					outputInterface = new KinectVideoOutput(this, kinectDevice);
				}

			} else {

				if (getConfiguration().jpegOutput) {

					outputInterface = new KinectInfraredOutputMJPEG(this, kinectDevice);

				} else {

					outputInterface = new KinectInfraredOutput(this, kinectDevice);
				}
			}

			outputInterface.init();

			addOutput(outputInterface, false);
		}
	}

	@Override
	protected void updateSensorDescription() {

		synchronized (sensorDescLock) {

			super.updateSensorDescription();

			if (!sensorDescription.isSetDescription()) {

				sensorDescription.setDescription(STR_SENSOR_DESCRIPTION);

				// Reference Frame
				SpatialFrame localRefFrame = new SpatialFrameImpl();
				localRefFrame.setId("LOCAL_FRAME");
				localRefFrame
						.setOrigin("Center of the Kinect Device facet containing apertures for emitter and sensors");
				localRefFrame.addAxis("x",
						"The X axis is in the plane of the of the facet containing the apertures for emitter and sensors and points to the right");
				localRefFrame.addAxis("y",
						"The Y axis is in the plane of the of the facet containing the apertures for emitter and sensors and points up");
				localRefFrame.addAxis("z",
						"The Z axis points towards the outside of the facet containing the apertures for emitter and sensors");
				((PhysicalSystem) sensorDescription).addLocalReferenceFrame(localRefFrame);

				SpatialFrame irSensorFrame = new SpatialFrameImpl();
				irSensorFrame.setId("IR_SENSOR_FRAME");
				irSensorFrame.setOrigin("12.5 mm on the positive X-Axis from the origin of the #LOCAL_FRAME");
				irSensorFrame.addAxis("x",
						"The X axis is in the plane of the of the facet containing the apertures for emitter and sensors and points to the right");
				irSensorFrame.addAxis("y",
						"The Y axis is in the plane of the of the facet containing the apertures for emitter and sensors and points up");
				irSensorFrame.addAxis("z",
						"The Z axis points towards the outside of the facet containing the apertures for emitter and sensors");
				((PhysicalSystem) sensorDescription).addLocalReferenceFrame(irSensorFrame);

				SpatialFrame depthSensorFrame = new SpatialFrameImpl();
				depthSensorFrame.setId("DEPTH_SENSOR_FRAME");
				depthSensorFrame.setOrigin("12.5 mm on the positive X-Axis from the origin of the #LOCAL_FRAME");
				depthSensorFrame.addAxis("x",
						"The X axis is in the plane of the of the facet containing the apertures for emitter and sensors and points to the right");
				depthSensorFrame.addAxis("y",
						"The Y axis is in the plane of the of the facet containing the apertures for emitter and sensors and points up");
				depthSensorFrame.addAxis("z",
						"The Z axis points towards the outside of the facet containing the apertures for emitter and sensors");
				((PhysicalSystem) sensorDescription).addLocalReferenceFrame(depthSensorFrame);

				SpatialFrame rgbCameraSensorFrame = new SpatialFrameImpl();
				rgbCameraSensorFrame.setId("RGB_SENSOR_FRAME");
				rgbCameraSensorFrame.setOrigin("12.5 mm on the negative X-Axis from the origin of the #LOCAL_FRAME");
				rgbCameraSensorFrame.addAxis("x",
						"The X axis is in the plane of the of the facet containing the apertures for emitter and sensors and points to the right");
				rgbCameraSensorFrame.addAxis("y",
						"The Y axis is in the plane of the of the facet containing the apertures for emitter and sensors and points up");
				rgbCameraSensorFrame.addAxis("z",
						"The Z axis points towards the outside of the facet containing the apertures for emitter and sensors");
				((PhysicalSystem) sensorDescription).addLocalReferenceFrame(rgbCameraSensorFrame);
			}

			SMLHelper helper = new SMLHelper(sensorDescription);

			helper.addSerialNumber(config.serialNumber);
		}
	}

	@Override
	public void start() throws SensorHubException {

		// Set Led to configured setting
		kinectDevice.setLed(ledStatus);

		kinectDevice.setTiltAngle(tiltAngle);

		if (null != outputInterface) {

			outputInterface.start();
		}
	}

	@Override
	public void stop() throws SensorHubException {

		if (isConnected) {

			if (null != outputInterface) {

				outputInterface.stop();
			}

			// Turn off LED
			kinectDevice.setLed(LedStatus.OFF);

			kinectDevice.setTiltAngle(0.0);

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
