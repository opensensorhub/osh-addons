package org.sensorhub.impl.sensor.flightAware;

import org.sensorhub.api.comm.IMessageQueuePush;
import org.vast.util.Asserts;

/**
 * Wraps the FlightAware message handler and forwards message to the
 * pub/sub queue 
 */
public class MessageHandlerWithForward extends MessageHandler
{    
    IMessageQueuePush msgQueue;
    
    
    public MessageHandlerWithForward(String user, String pwd, final IMessageQueuePush msgQueue) 
    {
        super(user, pwd);
        
        Asserts.checkNotNull(msgQueue, IMessageQueuePush.class);
        this.msgQueue = msgQueue;
    }
    

    @Override
    public void handle(String message)
    {
        msgQueue.publish(message.getBytes());        
        super.handle(message);
    }

}
