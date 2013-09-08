package org.sensorhub.api.sensor;

import org.sensorhub.api.common.CommandStatus;


/**
 * <p><b>Title:</b>
 * SensorControlEvent
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Special type of immutable event carrying status data by reference
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @date Nov 5, 2010
 */
public class SensorControlEvent extends SensorEvent
{
	private static final long serialVersionUID = -1682605821666177558L;
	
	
	/**
	 * Status of the command that triggered this event
	 */
    protected CommandStatus status;
		
	
    /**
     * Default constructor
     */
	public SensorControlEvent(String sensorId, CommandStatus status)
	{
	    super(sensorId, Type.COMMAND_STATUS);
		this.status = status;
	}
	

    public CommandStatus getStatus()
    {
        return status;
    }
}
