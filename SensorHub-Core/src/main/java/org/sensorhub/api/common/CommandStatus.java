package org.sensorhub.api.common;


/**
 * <p><b>Title:</b>
 * CommandStatus
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Simple data structure to hold status information for a command
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @date Nov 5, 2010
 */
public class CommandStatus
{
	public enum StatusCode {PENDING, ACCEPTED, REJECTED, COMPLETED, FAILED, CANCELLED};
	
	public String id;
	public StatusCode status;
	public String subCode;
	public String message;
	public long updateTime;
	public CommandStatus previousStatus;
}
