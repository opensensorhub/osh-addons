/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are copyright (C) 2010, Sensia Software LLC
 All Rights Reserved.

 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.service;

import org.vast.cdm.common.DataBlock;
import org.vast.cdm.common.DataComponent;


/**
 * <p><b>Title:</b>
 * CommandEvent
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Type of event generated when new command are received by control services.
 * It is immutable and carries command data by reference
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @date Nov 5, 2010
 */
public class CommandEvent extends ServiceEvent
{
    private static final long serialVersionUID = 6423151181007376051L;
    
    
    /**
     * ID of command associated to this event
     */
    protected String commandID;
    
    
    /**
     * Reference to command message descriptor
     */
    protected DataComponent commandDescriptor;
    
    
    /**
     * Reference to command message data 
     */
    protected DataBlock commandData;
    
    
    /**
     * Sole constructor
     * @param serviceId
     * @param commandDescriptor
     * @param commandData
     */
    public CommandEvent(String serviceId, String commandID, DataComponent commandDescriptor, DataBlock commandData)
    {
        super(serviceId, EventType.COMMAND_AVAILABLE);
        this.commandID = commandID;
        this.commandDescriptor = commandDescriptor;
        this.commandData = commandData;
    }


    /**
     * @return id of command that triggered the event
     */
    public String getCommandID()
    {
        return commandID;
    }


    /**
     * @return command message descriptor
     */
    public DataComponent getCommandDescriptor()
    {
        return commandDescriptor;
    }


    /**
     * @return command message data
     */
    public DataBlock getCommandData()
    {
        return commandData;
    }
}
