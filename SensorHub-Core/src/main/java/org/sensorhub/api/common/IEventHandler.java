/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2013 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.common;


/**
 * <p><b>Title:</b>
 * IEventHandler
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Interface for all event handling classes.
 * Typically, event dispatching is delegated by event producers to 
 * implementations of this interface. It accepts registering/unregistering of
 * listeners and also provide a publish method to be called by the producer
 * for dispatching the events.
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @date Sep 5, 2013
 */
public interface IEventHandler extends IEventProducer
{

    public abstract void publishEvent(Event e);

}