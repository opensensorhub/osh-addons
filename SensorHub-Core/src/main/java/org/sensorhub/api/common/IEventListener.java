/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are copyright (C) 2010, Sensia Software LLC
 All Rights Reserved.

 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.common;


/**
 * <p><b>Title:</b>
 * IEventListener
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Generic event listener
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @date Nov 5, 2010
 */
public interface IEventListener
{
    public void handleEvent(Event e);
}
