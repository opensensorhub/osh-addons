/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are copyright (C) 2010, Sensia Software LLC
 All Rights Reserved.

 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.common;

import java.util.ArrayList;
import java.util.List;
import org.sensorhub.api.common.Event;
import org.sensorhub.api.common.IEventHandler;
import org.sensorhub.api.common.IEventListener;


/**
 * <p><b>Title:</b>
 * BasicEventListenerHandler
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Basic synchronous event manager
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @date Nov 16, 2010
 */
public class BasicEventHandler implements IEventHandler
{
    protected List<IEventListener> listeners = new ArrayList<IEventListener>();
    
    
    /* (non-Javadoc)
     * @see org.sensorhub.impl.common.IEventHandler#registerListener(org.sensorhub.api.common.IEventListener)
     */
    @Override
    public void registerListener(IEventListener listener)
    {
        listeners.add(listener);
    }


    /* (non-Javadoc)
     * @see org.sensorhub.impl.common.IEventHandler#removeListener(org.sensorhub.api.common.IEventListener)
     */
    @Override
    public void unregisterListener(IEventListener listener)
    {
        listeners.remove(listener);
    }
    
    
    /* (non-Javadoc)
     * @see org.sensorhub.impl.common.IEventHandler#publishEvent(org.sensorhub.api.common.Event)
     */
    @Override
    public void publishEvent(Event e)
    {
        for (IEventListener listener: listeners)
            listener.handleEvent(e);
    }
}
