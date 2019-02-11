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

import java.nio.ByteBuffer;

import org.openkinect.freenect.DepthHandler;
import org.openkinect.freenect.Device;
import org.openkinect.freenect.FrameMode;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.vast.data.DataBlockMixed;
import org.vast.data.TextEncodingImpl;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.VectorHelper;

import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.Quantity;

class KinectDepthOutput extends KinectOutputInterface {

	private static final String STR_NAME = new String("Kinect Depth");

	private static final String STR_POINT_UNITS_OF_MEASURE = new String("m");

	private static final String STR_POINT_DEFINITION = SWEHelper.getPropertyUri("Distance");

	private static final String STR_POINT_DESCRIPTION = new String(
			"A measure of distance from the sensor, the raw point cloud data retrieved from Kinect depth sensor.");

	private static final String STR_POINT_NAME = new String("Point");

	private static final String STR_POINT_LABEL = new String("Distance");

	private static final String STR_MODEL_NAME = new String("Point Cloud Model");

	private static final String STR_MODEL_DEFINITION = SWEHelper.getPropertyUri("DepthPointCloud");

	private static final String STR_MODEL_DESCRIPTION = new String("Point Cloud Data read from Kinect Depth Sensor");

	private static final String STR_TIME_DATA_COMPONENT = new String("time");
	private static final String STR_POINT_DATA_COMPONENT = new String("points");

	private DataEncoding encoding;

	private DataComponent pointCloudFrameData;

	private int numPoints = 0;

	private long lastPublishTimeMillis = System.currentTimeMillis();

	private long samplingTimeMillis = 1000;

	protected double scaleFactor = 1;

	protected int frameWidth = 640;

	protected int frameHeight = 480;

	public KinectDepthOutput(KinectSensor parentSensor, Device kinectDevice) {

		super(parentSensor, kinectDevice);

		name = STR_NAME;

		samplingTimeMillis = (long) (getParentModule().getConfiguration().samplingTime * MS_PER_S);

		numPoints = computeNumPoints();
	}

	@Override
	public DataComponent getRecordDescription() {

		return pointCloudFrameData;
	}

	@Override
	public DataEncoding getRecommendedEncoding() {

		return encoding;
	}

	@Override
	public void init() {

		numPoints = computeNumPoints();

		device.setDepthFormat(getParentModule().getConfiguration().depthFormat);

		VectorHelper factory = new VectorHelper();

		pointCloudFrameData = factory.newDataRecord(NUM_DATA_COMPONENTS);
		pointCloudFrameData.setDescription(STR_MODEL_DESCRIPTION);
		pointCloudFrameData.setDefinition(STR_MODEL_DEFINITION);
		pointCloudFrameData.setName(STR_MODEL_NAME);

		Quantity point = factory.newQuantity(STR_POINT_DEFINITION, STR_POINT_LABEL, STR_POINT_DESCRIPTION,
				STR_POINT_UNITS_OF_MEASURE);

		DataArray pointArray = factory.newDataArray(numPoints);
		pointArray.setElementType(STR_POINT_NAME, point);

		pointCloudFrameData.addComponent(STR_TIME_DATA_COMPONENT, factory.newTimeStampIsoGPS());
		pointCloudFrameData.addComponent(STR_POINT_DATA_COMPONENT, pointArray);

		encoding = new TextEncodingImpl();
	}

	@Override
	public void start() {

		device.startDepth(new DepthHandler() {

			@Override
			public void onFrameReceived(FrameMode mode, ByteBuffer frame, int timestamp) {

				if ((System.currentTimeMillis() - lastPublishTimeMillis) > samplingTimeMillis) {

					double[] pointCloudData = new double[numPoints];

					int currentPoint = 0;

					int skipStep = (int) (1 / scaleFactor);

					for (int y = 0; y < frameHeight; y += skipStep) {

						for (int x = 0; x < frameWidth; x += skipStep) {

							int index = (x + y * frameWidth);

							int depthValue = frame.getShort(index);

							pointCloudData[currentPoint] = rawDepthToMeters(depthValue);

							++currentPoint;
						}
					}

					DataBlock dataBlock = pointCloudFrameData.createDataBlock();
					
					double samplingTime = System.currentTimeMillis() / MS_PER_S;
					
					dataBlock.setDoubleValue(IDX_TIME_DATA_COMPONENT, samplingTime);
					
					((DataBlockMixed) dataBlock).getUnderlyingObject()[IDX_PAYLOAD_DATA_COMPONENT]
							.setUnderlyingObject(pointCloudData);

					// update latest record and send event
					latestRecord = dataBlock;
					
					latestRecordTime = System.currentTimeMillis();
					
					eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, KinectDepthOutput.this, dataBlock));

					frame.position(0);

					lastPublishTimeMillis = System.currentTimeMillis();
				}
			}
		});
	}

	/**
	 * Converts raw depth data to meters.
	 * 
	 * http://graphics.stanford.edu/~mdfisher/Kinect.html
	 */
	protected double rawDepthToMeters(int depthValue) {

		double result = 0.0;

		if (depthValue <= 2047) {

			result = (double) (1.0 / (depthValue * -0.0030711016 + 3.3309495161));
		}

		return result;
	}

	protected int computeNumPoints() {

		frameWidth = getParentModule().getConfiguration().frameWidth;
		frameHeight = getParentModule().getConfiguration().frameHeight;

		int numPoints = frameWidth * frameHeight;

		scaleFactor = getParentModule().getConfiguration().pointCloudScaleFactor;

		if ((scaleFactor > 0) && (scaleFactor <= 1.0)) {

			frameWidth = (int)(frameWidth * scaleFactor);
			frameHeight = (int)(frameHeight * scaleFactor);
			numPoints = frameWidth * frameHeight;
		}

		return numPoints;
	}
}
