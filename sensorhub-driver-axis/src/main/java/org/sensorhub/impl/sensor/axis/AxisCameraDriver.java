
package org.sensorhub.impl.sensor.axis;

import net.opengis.sensorml.v20.AbstractProcess;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorModule;


/**
 * <p>
 * Implementation of sensor interface for generic Axis Cameras using IP
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
    public AbstractProcess getCurrentSensorDescription() throws SensorException
    {
        // parent class reads SensorML from config is provided
        // and then sets unique ID, outputs and controllable parameters
        return super.getCurrentSensorDescription();
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

    }


    @Override
    public void cleanup() throws SensorHubException
    {

    }

}
