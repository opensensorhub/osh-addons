/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are copyright (C) 2010, Sensia Software LLC
 All Rights Reserved.

 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.persistence;


/**
 * <p><b>Title:</b>
 * IMessageKey
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Interface for keys used to index messages (such as commands)
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @date Nov 12, 2010
 */
public interface IMessageKey
{
    /**
     * Sets ID of module that the message is targeted to
     * @param localID
     */
    public void setTarget(String localID);
    
    
    /**
     * @return ID of module that the message is targeted to
     */
    public String getTarget();
}
