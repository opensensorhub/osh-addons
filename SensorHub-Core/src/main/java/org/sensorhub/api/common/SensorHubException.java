/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are copyright (C) 2010, Sensia Software LLC
 All Rights Reserved.

 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.common;


/**
 * <p><b>Title:</b>
 * SensorHubException
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Base exception class for all exceptions generated in SensorHub
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @date Nov 5, 2010
 */
public class SensorHubException extends Exception
{
    private static final long serialVersionUID = -5262139949248131452L;
    
    
    /**
     * Numerical code to allow polymorphism w/o subclassing
     * Code values are defined by concrete exception class
     */
    private int code;
    
    
    public SensorHubException(String message)
    {
        super(message);
    }


    public SensorHubException(String message, Throwable cause)
    {
        super(message, cause);
    }


    public SensorHubException(String message, int code, Throwable cause)
    {
        super(message, cause);
        this.code = code;
    }
    
    
    /**
     * @return exception code
     */
    public int getCode()
    {
        return code;
    }
}
