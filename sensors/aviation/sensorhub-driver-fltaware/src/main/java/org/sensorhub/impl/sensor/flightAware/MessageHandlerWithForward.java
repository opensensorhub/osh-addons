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
    
    
    public MessageHandlerWithForward(FlightAwareDriver driver, IMessageQueuePush msgQueue) 
    {
        super(driver);        
        Asserts.checkNotNull(msgQueue, IMessageQueuePush.class);
        this.msgQueue = msgQueue;
    }
    

    @Override
    protected void newFlightObject(FlightObject fltObj)
    {
        // don't forward flightplan messages here
        // we need to forward them once the route has been decoded
        if (!FLIGHTPLAN_MSG_TYPE.equals(fltObj.type))
            msgQueue.publish(fltObj.json.getBytes());
        
        for (FlightObjectListener l: objectListeners)
            l.processMessage(fltObj);
    }
    

    @Override
    protected void newFlightPlan(FlightObject fltPlan)
    {
        publish(fltPlan);
        
        for (FlightPlanListener l: planListeners)
            l.newFlightPlan(fltPlan);
    }
    
    
    private void publish(FlightObject fltObj)
    {
        String msg = gson.toJson(fltObj);
        System.err.println(msg);
        msgQueue.publish(msg.getBytes());
    }

}
