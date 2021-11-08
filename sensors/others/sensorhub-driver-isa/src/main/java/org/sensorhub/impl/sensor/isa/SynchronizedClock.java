/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.isa;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Flow.Subscription;
import java.util.function.Consumer;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.data.ObsEvent;
import org.sensorhub.api.event.EventUtils;


/**
 * <p>
 * Clock implementation used to synchronized simulated ISA measurements 
 * with other sensor streams
 * </p>
 *
 * @author Alex Robin
 * @since Oct 13, 2021
 */
public class SynchronizedClock extends Clock
{
    Subscription sub;
    volatile Instant lastObsTime;
    
    
    public SynchronizedClock(ISensorHub hub, String systemUID, String outputName, Consumer<Instant> tick)
    {
        hub.getEventBus().newSubscription(ObsEvent.class)
            .withTopicID(EventUtils.getDataStreamDataTopicID(systemUID, outputName))
            .subscribe(e -> {
                if (!e.getObservations().isEmpty()) {
                    var obs = e.getObservations().iterator().next();
                    
                    // reset if simulation looped around
                    if (lastObsTime != null && obs.getPhenomenonTime().isBefore(lastObsTime))
                        lastObsTime = null;
                    
                    // round time to whole seconds
                    var time = obs.getPhenomenonTime().truncatedTo(ChronoUnit.SECONDS);
                    if (lastObsTime == null || time.isAfter(lastObsTime))
                    {
                        lastObsTime = time;
                        tick.accept(lastObsTime);
                    }
                }
            })
            .thenAccept(s -> {
                sub = s;
                s.request(Long.MAX_VALUE);
            });
    }
    
    
    @Override
    public ZoneId getZone()
    {
        return ZoneOffset.UTC;
    }
    

    @Override
    public Clock withZone(ZoneId zone)
    {
        return null;
    }
    

    @Override
    public Instant instant()
    {
        return lastObsTime != null ? lastObsTime : Instant.EPOCH;
    }
    
    
    public void stop()
    {
        if (sub != null)
            sub.cancel();
    }

}
