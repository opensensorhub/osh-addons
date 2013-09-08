/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are copyright (C) 2010, Sensia Software LLC
 All Rights Reserved.

 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.common;

import java.io.Serializable;


/**
 * <p><b>Title:</b>
 * Event
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Base class for all sensor hub events
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @date Nov 5, 2010
 */
public class Event implements Serializable
{
    private static final long serialVersionUID = 9176636296502966036L;
    
    protected Object source;
    protected long timeStamp;
    protected int code;
    protected String description;
    
    
    public Object getSource()
    {
        return source;
    }


    public void setSource(Object source)
    {
        this.source = source;
    }


    public long getTimeStamp()
    {
        return timeStamp;
    }


    public void setTimeStamp(long timeStamp)
    {
        this.timeStamp = timeStamp;
    }


    public int getCode()
    {
        return code;
    }
    
    
    public void setCode(int code)
    {
        this.code = code;
    }
    
    
    public String getDescription()
    {
        return description;
    }
    
    
    public void setDescription(String description)
    {
        this.description = description;
    }
    
}
