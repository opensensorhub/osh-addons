/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2013 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.common;


/**
 * <p><b>Title:</b>
 * IEventProducer
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Base interface for all objects accepting producing events and accepting
 * registration of event listeners 
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @date Sep 5, 2013
 */
public interface IEventProducer
{
    
    /**
     * Registers a listener with this event producer.
     * The listener is reponsible for filtering received events.
     * @param listener
     */
    public abstract void registerListener(IEventListener listener);


    /**
     * Unregisters a listener from this event producer.
     * No more event will be sent to the listener from this producer.
     * @param listener
     */
    public abstract void unregisterListener(IEventListener listener);
}
