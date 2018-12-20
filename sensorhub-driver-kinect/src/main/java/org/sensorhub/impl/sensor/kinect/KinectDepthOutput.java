package org.sensorhub.impl.sensor.kinect;

import java.nio.ByteBuffer;

import org.openkinect.freenect.DepthHandler;
import org.openkinect.freenect.Device;
import org.openkinect.freenect.FrameMode;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.data.DataBlockMixed;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.VectorHelper;

import net.opengis.swe.v20.Count;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.Vector;

class KinectDepthOutput extends AbstractSensorOutput<KinectSensor> {

	private Device device = null;

	private DataEncoding encoding;

	private DataComponent pointCloudFrameData;
	
	private static final int IMAGE_BUFFER_WIDTH = 640;
	private static final int IMAGE_BUFFER_HEIGHT = 480;
	private static final int NUM_DATA_COMPONENTS = 3;
	private static final int IDX_TIME_DATA_COMPONENT = 0;
	private static final int IDX_POINT_COUNT_DATA_COMPONENT = 1;
	private static final int IDX_POINTS_COMPONENT = 2;
	
	private enum EuclideanCoords {
		X(0),
		Y(1),
		Z(2),
		NUM_DIMENSIONS(3);
		
		private int value;
		
		private EuclideanCoords(int value) {
			
			this.value = value;
		}
		
		public int valueOf() {
			
			return value;
		}
	}

	private static final String STR_POINT_UNITS_OF_MEASURE = new String("meters (m)");
	private static final String STR_POINT_DEFINITION = new String("A 3-dimensional point in space");
	private static final String STR_POINT_REFERENCE_FRAME = new String("Volume which extends in front of and "
			+ "beyond the Kinect depth sensor with the X-component being the width, Y-component being "
			+ "the height, and the Z-component being the depth or distance from the sensor's receptor surface.");
	private static final String STR_POINT_DESCRIPTION = new String("A 3-dimensional point, part of the raw "
			+ "point cloud data retrieved from Kinect depth sensor.");
	
	private static final String STR_POINT = new String("Point");
	private static final String STR_POINT_COUNT_ID = new String("NUM_POINTS");
	
	private static final String STR_MODEL_NAME = new String("Point Cloud Model");
	private static final String STR_MODEL_DESCRIPTION = new String("");
	private static final String STR_MODEL_DEFINITION = new String("");
	
	private static final String STR_TIME_DATA_COMPONENT = new String("time");
	private static final String STR_POINT_COUNT_DATA_COMPONENT = STR_POINT_COUNT_ID.toLowerCase();
	private static final String STR_POINT_DATA_COMPONENT = new String("points");

	public KinectDepthOutput(KinectSensor parentSensor, Device kinectDevice) {

		super(parentSensor);

		device = kinectDevice;
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
	public String getName()
	{
		return STR_MODEL_NAME;
	}
	
	public void init() {

		device.setDepthFormat(parentSensor.getDeviceParams().getDepthFormat(), 
				parentSensor.getDeviceParams().getDepthSensorResolution());

		VectorHelper factory = new VectorHelper();

		
		pointCloudFrameData = factory.newDataRecord(NUM_DATA_COMPONENTS);
		pointCloudFrameData.setDescription(STR_MODEL_DESCRIPTION);
		pointCloudFrameData.setDefinition(STR_MODEL_DEFINITION);
		pointCloudFrameData.setName(STR_MODEL_NAME);

		Vector point = factory.newLocationVectorXYZ(STR_POINT_DEFINITION, STR_POINT_REFERENCE_FRAME, STR_POINT_UNITS_OF_MEASURE);
		point.setDescription(STR_POINT_DESCRIPTION);

		Count numPoints = factory.newCount();
		numPoints.setId(STR_POINT_COUNT_ID);

		DataArray pointArray = factory.newDataArray();
		pointArray.setElementType(STR_POINT, point);
		pointArray.setElementCount(numPoints);
		

		pointCloudFrameData.addComponent(STR_TIME_DATA_COMPONENT, factory.newTimeStampIsoUTC());
		pointCloudFrameData.addComponent(STR_POINT_COUNT_DATA_COMPONENT, numPoints);
		pointCloudFrameData.addComponent(STR_POINT_DATA_COMPONENT, pointArray);

		encoding = SWEHelper.getDefaultBinaryEncoding(pointCloudFrameData);
	}

	public void start() {

		device.startDepth(new DepthHandler() {
			
			@Override
			public void onFrameReceived(FrameMode mode, ByteBuffer frame, int timestamp) {
			    
				short[][][] pointCloudData = new short[IMAGE_BUFFER_HEIGHT][IMAGE_BUFFER_WIDTH][EuclideanCoords.NUM_DIMENSIONS.valueOf()];

				for (short height = 0; height < IMAGE_BUFFER_HEIGHT; ++height) {
					
					for (short width = 0; width < IMAGE_BUFFER_WIDTH; ++width) {
					
						pointCloudData[height][width][EuclideanCoords.X.valueOf()] = width;
						pointCloudData[height][width][EuclideanCoords.Y.valueOf()] = height;
						pointCloudData[height][width][EuclideanCoords.Z.valueOf()] = frame.getShort(height * IMAGE_BUFFER_HEIGHT + width);
					}
				}
				
				DataBlock dataBlock = pointCloudFrameData.createDataBlock();
				dataBlock.setIntValue(IDX_POINT_COUNT_DATA_COMPONENT, IMAGE_BUFFER_HEIGHT * IMAGE_BUFFER_WIDTH);
				dataBlock.setDoubleValue(IDX_TIME_DATA_COMPONENT, 0);
				((DataBlockMixed)dataBlock).getUnderlyingObject()[IDX_POINTS_COMPONENT].setUnderlyingObject(pointCloudData);
				
		        // update latest record and send event
		        latestRecord = dataBlock;
		        latestRecordTime = System.currentTimeMillis();
		        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, KinectDepthOutput.this, dataBlock));        
			}
		});
	}
}
