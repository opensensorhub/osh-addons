package org.sensorhub.impl.sensor.kinect;

import org.sensorhub.api.common.CommandStatus;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorControl;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;

public class KinectControl extends AbstractSensorControl<KinectSensor>{

	public KinectControl(KinectSensor parentSensor) {
		
		super(parentSensor);

	}

	@Override
	public DataComponent getCommandDescription() {
		
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CommandStatus execCommand(DataBlock command) throws SensorException {
		
		// TODO Auto-generated method stub
		return null;
	}

}
