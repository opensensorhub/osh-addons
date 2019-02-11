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
import net.opengis.swe.v20.Vector;

public class KinectDepthMappedOutput extends KinectDepthOutput {

	private static final int IDX_X_COORD_COMPONENT = 0;
	private static final int IDX_Y_COORD_COMPONENT = 1;
	private static final int IDX_Z_COORD_COMPONENT = 2;
	private static final int DIMENSIONS = 3;

	private static final String STR_NAME = new String("Kinect Depth - Camera Model Mapped");

	private static final String STR_POINT_UNITS_OF_MEASURE = new String("m");

	private static final String STR_POINT_DEFINITION = SWEHelper.getPropertyUri("Distance");

//	private static final String STR_POINT_REF_FRAME = new String(
//			"A measure of distance from the sensor, the raw point cloud data retrieved from Kinect depth sensor in XYZ coordinates.");
	private static final String STR_POINT_REF_FRAME = new String("#DEPTH_SENSOR_FRAME");

	private static final String STR_POINT_NAME = new String("Point");

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

	public KinectDepthMappedOutput(KinectSensor parentSensor, Device kinectDevice) {

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

		Vector vector = factory.newLocationVectorXYZ(STR_POINT_DEFINITION, STR_POINT_REF_FRAME,
				STR_POINT_UNITS_OF_MEASURE);

		DataArray pointArray = factory.newDataArray(numPoints);
		pointArray.setElementType(STR_POINT_NAME, vector);

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

					double[] pointCloudData = new double[numPoints * DIMENSIONS];

					int currentPoint = 0;

					int skipStep = (int)(1/scaleFactor);
					
					for (int y = 0; y < frameHeight; y += skipStep) {

						for (int x = 0; x < frameWidth; x += skipStep) {

							int index = (x + y * frameWidth);

							int depthValue = frame.getShort(index);

							double[] coordinates = depthToWorld(x, y, depthValue);

							pointCloudData[currentPoint++] = coordinates[IDX_X_COORD_COMPONENT];
							pointCloudData[currentPoint++] = coordinates[IDX_Y_COORD_COMPONENT];
							pointCloudData[currentPoint++] = coordinates[IDX_Z_COORD_COMPONENT];
						}
					}

					DataBlock dataBlock = pointCloudFrameData.createDataBlock();
					dataBlock.setDoubleValue(IDX_TIME_DATA_COMPONENT, System.currentTimeMillis() / MS_PER_S);
					((DataBlockMixed) dataBlock).getUnderlyingObject()[IDX_PAYLOAD_DATA_COMPONENT]
							.setUnderlyingObject(pointCloudData);

					// update latest record and send event
					latestRecord = dataBlock;
					latestRecordTime = System.currentTimeMillis();
					eventHandler.publishEvent(
							new SensorDataEvent(latestRecordTime, KinectDepthMappedOutput.this, dataBlock));

					frame.position(0);

					lastPublishTimeMillis = System.currentTimeMillis();
				}
			}
		});
	}

	/**
	 * Converts raw depth data to world vector with reference as the sensor face.
	 * 
	 * http://graphics.stanford.edu/~mdfisher/Kinect.html
	 */
	private double[] depthToWorld(int x, int y, int depthValue) {

		final double fx_d = 1.0 / 5.9421434211923247e+02;

		final double fy_d = 1.0 / 5.9104053696870778e+02;

		final double cx_d = 3.3930780975300314e+02;

		final double cy_d = 2.4273913761751615e+02;

		double[] result = new double[3];

		final double depth = rawDepthToMeters(depthValue);

		result[IDX_X_COORD_COMPONENT] = ((x - cx_d) * depth * fx_d);

		result[IDX_Y_COORD_COMPONENT] = ((y - cy_d) * depth * fy_d);

		result[IDX_Z_COORD_COMPONENT] = depth;

		return result;
	}
}
