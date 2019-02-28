/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2018 Delta Air Lines, Inc. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

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
