/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2013 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.sps;

import java.util.List;
import org.sensorhub.api.common.CommandStatus;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.sensor.ISensorControlInterface;
import org.sensorhub.api.sensor.SensorException;
import org.vast.cdm.common.DataBlock;
import org.vast.cdm.common.DataComponent;
import org.vast.util.DateTime;


/**
 * <p><b>Title:</b>
 * SPSClientControlInterface
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Control interface for a virtual sensor managed by a remote SPS service.
 * The interface configures itself automatically to mirror the remote
 * service capabilities.
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @date Sep 6, 2013
 */
public class SPSClientControlInterface implements ISensorControlInterface
{

    @Override
    public void unregisterListener(IEventListener listener)
    {
        // TODO Auto-generated method stub

    }


    @Override
    public boolean isAsyncExecSupported()
    {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean isSchedulingSupported()
    {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean isStatusHistorySupported()
    {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public DataComponent getCommandDescription()
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public CommandStatus execCommand(DataBlock command) throws SensorException
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public CommandStatus execCommandGroup(List<DataBlock> commands) throws SensorException
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public CommandStatus sendCommand(DataBlock command) throws SensorException
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public CommandStatus sendCommandGroup(List<DataBlock> commands) throws SensorException
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public CommandStatus scheduleCommand(DataBlock command, DateTime execTime) throws SensorException
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public CommandStatus scheduleCommandGroup(List<DataBlock> commands, DateTime execTime) throws SensorException
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public CommandStatus cancelCommand(String commandID) throws SensorException
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public CommandStatus getCommandStatus(String commandID) throws SensorException
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public List<CommandStatus> getCommandStatusHistory(String commandID) throws SensorException
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public void registerListener(IEventListener listener)
    {
        // TODO Auto-generated method stub

    }

}
