/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.
Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2019 Botts Innovative Research, Inc. All Rights Reserved. 

******************************* END LICENSE BLOCK ***************************/
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.common.Event;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.kinect.KinectConfig;
import org.sensorhub.impl.sensor.kinect.KinectConfig.Mode;
import org.vast.data.DataBlockMixed;

import net.opengis.swe.v20.DataBlock;

import org.sensorhub.impl.sensor.kinect.KinectSensor;

public class TestKinectDriver implements IEventListener {

	private KinectConfig config = null;

	private KinectSensor driver = null;

	private static final int MAX_FRAMES = 100;

	private int frameCount;

	private KinectDisplayFrame displayFrame;

	private Object syncObject = new Object();

	@Before
	public void init() throws Exception {

		config = new KinectConfig();

		config.tiltAngle = Math.random() * 27.0;

		driver = new KinectSensor();

		displayFrame = new KinectDisplayFrame();

		frameCount = 0;
	}

	@After
	public void stop() throws Exception {

		displayFrame.dispose();

		driver.requestStop();
	}

	@Test
	public void testDepth() throws Exception {

		config.videoMode = Mode.DEPTH;
		config.samplingTime = 0;
		config.pointCloudScaleFactor = .1;

		driver.setConfiguration(config);
		driver.requestInit(false);
		driver.requestStart();

		displayFrame.initialize("Depth Test", config, Mode.DEPTH, false);

		// register listener on data interface
		ISensorDataInterface di = driver.getObservationOutputs().values().iterator().next();

		assertTrue("No video output", di != null);

		di.registerListener(this);

		// start capture and wait until we receive the first frame
		synchronized (syncObject) {

			while (frameCount < MAX_FRAMES) {

				syncObject.wait();
			}
		}

		di.unregisterListener(this);
	}

	@Test
	public void testDepthCameraMapped() throws Exception {

		config.videoMode = Mode.DEPTH;
		config.samplingTime = 0;
		config.pointCloudScaleFactor = .1;
		config.useCameraModel = true;

		driver.setConfiguration(config);
		driver.requestInit(false);
		driver.requestStart();

		// register listener on data interface
		ISensorDataInterface di = driver.getObservationOutputs().values().iterator().next();

		assertTrue("No video output", di != null);

		IEventListener listener = new IEventListener() {

			@Override
			public void handleEvent(Event<?> e) {

				assertTrue(e instanceof SensorDataEvent);
				SensorDataEvent newDataEvent = (SensorDataEvent) e;

				DataBlock frameBlock = ((DataBlockMixed) newDataEvent.getRecords()[0]).getUnderlyingObject()[1];

				double[] frameData = (double[]) frameBlock.getUnderlyingObject();

				assertTrue(frameData.length == (int)(3 * (config.frameWidth * config.pointCloudScaleFactor)
						* ((config.frameHeight * config.pointCloudScaleFactor))));

				++frameCount;

				synchronized (syncObject) {

					syncObject.notify();
				}
			}
		};

		di.registerListener(listener);

		// start capture and wait until we receive the first frame
		synchronized (syncObject) {

			while (frameCount < MAX_FRAMES) {

				syncObject.wait();
			}
		}

		di.unregisterListener(listener);
	}

	@Test
	public void testRgb() throws Exception {

		config.videoMode = Mode.VIDEO;

		displayFrame.initialize("RGB Test", config.frameWidth, config.frameHeight, Mode.VIDEO, false);

		driver.setConfiguration(config);
		driver.requestInit(false);
		driver.requestStart();

		// register listener on data interface
		ISensorDataInterface di = driver.getObservationOutputs().values().iterator().next();

		assertTrue("No video output", di != null);

		di.registerListener(this);

		// start capture and wait until we receive the first frame
		synchronized (syncObject) {

			while (frameCount < MAX_FRAMES) {

				syncObject.wait();
			}
		}

		di.unregisterListener(this);
	}

	@Test
	public void testIr() throws Exception {

		config.videoMode = Mode.IR;

		displayFrame.initialize("IR Test", config.frameWidth, config.frameHeight, Mode.IR, false);

		driver.setConfiguration(config);
		driver.requestInit(false);
		driver.requestStart();

		// register listener on data interface
		ISensorDataInterface di = driver.getObservationOutputs().values().iterator().next();

		assertTrue("No video output", di != null);

		di.registerListener(this);

		// start capture and wait until we receive the first frame
		synchronized (syncObject) {

			while (frameCount < MAX_FRAMES) {

				syncObject.wait();
			}
		}

		di.unregisterListener(this);
	}

	@Test
	public void testRgbMjpeg() throws Exception {

		config.videoMode = Mode.VIDEO;
		config.jpegOutput = true;

		displayFrame.initialize("MJPEG Test", config.frameWidth, config.frameHeight, Mode.VIDEO, true);

		driver.setConfiguration(config);
		driver.requestInit(false);
		driver.requestStart();

		// register listener on data interface
		ISensorDataInterface di = driver.getObservationOutputs().values().iterator().next();

		assertTrue("No video output", di != null);

		di.registerListener(this);

		// start capture and wait until we receive the first frame
		synchronized (syncObject) {

			while (frameCount < MAX_FRAMES) {

				syncObject.wait();
			}
		}

		di.unregisterListener(this);
	}
	
	@Test
	public void testIrMjpeg() throws Exception {

		config.videoMode = Mode.IR;
		config.jpegOutput = true;

		displayFrame.initialize("IR MJPEG Test", config.frameWidth, config.frameHeight, Mode.IR, true);

		driver.setConfiguration(config);
		driver.requestInit(false);
		driver.requestStart();

		// register listener on data interface
		ISensorDataInterface di = driver.getObservationOutputs().values().iterator().next();

		assertTrue("No video output", di != null);

		di.registerListener(this);

		// start capture and wait until we receive the first frame
		synchronized (syncObject) {

			while (frameCount < MAX_FRAMES) {

				syncObject.wait();
			}
		}

		di.unregisterListener(this);
	}

	@Override
	public void handleEvent(Event<?> e) {

		assertTrue(e instanceof SensorDataEvent);
		SensorDataEvent newDataEvent = (SensorDataEvent) e;

		displayFrame.drawFrame(newDataEvent.getRecords()[0]);

		++frameCount;

		synchronized (syncObject) {

			syncObject.notify();
		}
	}
}
