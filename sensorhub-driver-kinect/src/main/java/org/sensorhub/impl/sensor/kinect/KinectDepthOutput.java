package org.sensorhub.impl.sensor.kinect;

import java.nio.ByteBuffer;

import org.openkinect.freenect.DepthHandler;
import org.openkinect.freenect.Device;
import org.openkinect.freenect.FrameMode;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.data.DataBlockMixed;
import org.vast.data.TextEncodingImpl;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.VectorHelper;

import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.Vector;

class KinectDepthOutput extends AbstractSensorOutput<KinectSensor> {

	private static final int NUM_DATA_COMPONENTS = 2;
	private static final int IDX_TIME_DATA_COMPONENT = 0;
	private static final int IDX_POINTS_COMPONENT = 1;
	private static final double MM_PER_M = 1000.0;
	private static final double MS_PER_S = 1000.0;

	private static final String STR_POINT_UNITS_OF_MEASURE = new String("m");
	private static final String STR_POINT_DEFINITION = new String(SWEHelper.getPropertyUri("Distance"));
	private static final String STR_POINT_REFERENCE_FRAME = new String("Volume which extends in front of and "
			+ "beyond the Kinect depth sensor with the X-component being the width, Y-component being "
			+ "the height, and the Z-component being the depth or distance from the sensor's receptor surface.");
	private static final String STR_POINT_DESCRIPTION = new String(
			"A 3-dimensional point, part of the raw point cloud data retrieved from Kinect depth sensor.");

	private static final String STR_POINT_NAME = new String("Point");

	private static final String STR_MODEL_NAME = new String("Point Cloud Model");
	private static final String STR_MODEL_DEFINITION = new String(SWEHelper.getPropertyUri("DepthPointCloud"));
	private static final String STR_MODEL_DESCRIPTION = new String("Point Cloud Data read from Kinect Depth Sensor");

	private static final String STR_TIME_DATA_COMPONENT = new String("time");
	private static final String STR_POINT_DATA_COMPONENT = new String("points");

	private static final int NUM_DATA_ELEMENTS_PER_POINT = 3;

	private Device device = null;

	private DataEncoding encoding;

	private DataComponent pointCloudFrameData;

	private int numPoints = 0;

	private int decimationFactor = 0;

	public KinectDepthOutput(KinectSensor parentSensor, Device kinectDevice) {

		super(parentSensor);

		device = kinectDevice;

		numPoints = getParentModule().getConfiguration().frameWidth * getParentModule().getConfiguration().frameHeight;
		
		decimationFactor = getParentModule().getConfiguration().pointCloudDecimationFactor;

		numPoints /= decimationFactor;
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
	public double getAverageSamplingPeriod() {

		return device.getDepthMode().framerate;
	}

	@Override
	protected void stop() {

		device.stopDepth();
	}

	@Override
	public String getName() {
		
		return STR_MODEL_NAME;
	}

	public void init() {

		device.setDepthFormat(getParentModule().getConfiguration().depthFormat);

		VectorHelper factory = new VectorHelper();

		pointCloudFrameData = factory.newDataRecord(NUM_DATA_COMPONENTS);
		pointCloudFrameData.setDescription(STR_MODEL_DESCRIPTION);
		pointCloudFrameData.setDefinition(STR_MODEL_DEFINITION);
		pointCloudFrameData.setName(STR_MODEL_NAME);

		Vector point = factory.newLocationVectorXYZ(STR_POINT_DEFINITION, STR_POINT_REFERENCE_FRAME,
				STR_POINT_UNITS_OF_MEASURE);
		point.setDescription(STR_POINT_DESCRIPTION);

		DataArray pointArray = factory.newDataArray(numPoints);
		pointArray.setElementType(STR_POINT_NAME, point);

		pointCloudFrameData.addComponent(STR_TIME_DATA_COMPONENT, factory.newTimeStampIsoUTC());
		pointCloudFrameData.addComponent(STR_POINT_DATA_COMPONENT, pointArray);

		encoding = new TextEncodingImpl();
	}

	public void start() {

		device.startDepth(new DepthHandler() {

			@Override
			public void onFrameReceived(FrameMode mode, ByteBuffer frame, int timestamp) {

				double[] pointCloudData = new double[numPoints * NUM_DATA_ELEMENTS_PER_POINT];

				int count = 0;

				for (int height = 0; height < getParentModule().getConfiguration().frameHeight; height += decimationFactor) {

					for (int width = 0; width < getParentModule().getConfiguration().frameWidth; width += decimationFactor) {

						int lo = frame.get(width + height * getParentModule().getConfiguration().frameWidth) & 255;
						int hi = frame.get(width + height * getParentModule().getConfiguration().frameWidth) & 255;

						pointCloudData[count] = (double) width;
						pointCloudData[++count] = (double) height;
						pointCloudData[++count] = (double) (((hi << 8 | lo) & 2047) / MM_PER_M);
					}
				}

				DataBlock dataBlock = pointCloudFrameData.createDataBlock();
				dataBlock.setDoubleValue(IDX_TIME_DATA_COMPONENT, timestamp / MS_PER_S);
				((DataBlockMixed) dataBlock).getUnderlyingObject()[IDX_POINTS_COMPONENT]
						.setUnderlyingObject(pointCloudData);

				// update latest record and send event
				latestRecord = dataBlock;
				latestRecordTime = timestamp;
				eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, KinectDepthOutput.this, dataBlock));

				frame.position(0);
			}
		});
	}
}
