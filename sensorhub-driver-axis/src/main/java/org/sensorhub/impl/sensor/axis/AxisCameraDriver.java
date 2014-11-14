package org.sensorhub.impl.sensor.axis;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModuleStateLoader;
import org.sensorhub.api.module.IModuleStateSaver;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.vast.sensorML.SMLProcess;
import org.vast.sensorML.system.SMLSystem;

/**
 * <p>
 * Implementation of configuration interface for generic Axis Cameras using IP
 * protocol
 * </p>
 *
 * <p>
 * Copyright (c) 2014
 * </p>
 * 
 * @author Mike Botts <mike.botts@botts-inc.com>
 * @since October 30, 2014
 */

public class AxisCameraDriver extends AbstractSensorModule<AxisCameraConfig>
{
	AxisVideoOutput videoDataInterface;
	AxisSettingsOutput ptzDataInterface;
	AxisVideoControl videoControlInterface;
	AxisPtzControl ptzControlInterface;

	/* *** here begins the specific sensor module stuff */

	public AxisCameraDriver()
	{
		videoDataInterface = new AxisVideoOutput(this);
		obsOutputs.put("videoOutput", videoDataInterface);

		ptzDataInterface = new AxisSettingsOutput(this);
		obsOutputs.put("ptzOutput", ptzDataInterface);
	}

	@Override
	public SMLProcess getCurrentSensorDescription() throws SensorException
	{

		// TODO: We will read SensorML document from configuration file and send
		// here
		return new SMLSystem();
	}

	@Override
	public boolean isConnected()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void start() throws SensorHubException
	{
		ptzDataInterface.init();
		videoDataInterface.init();
		
		ptzDataInterface.startPolling();
		videoDataInterface.startStream();
	}

	@Override
	public void stop()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void saveState(IModuleStateSaver saver) throws SensorHubException
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void loadState(IModuleStateLoader loader) throws SensorHubException
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void cleanup() throws SensorHubException
	{
		// TODO Auto-generated method stub

	}

}
