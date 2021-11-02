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

import java.io.Closeable;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow.Subscription;
import java.util.function.Consumer;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.data.ObsEvent;
import org.sensorhub.api.event.EventUtils;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;


/**
 * <p>
 * Class used for simulating ISA outputs
 * </p>
 *
 * @author Alex Robin
 * @since Oct 18, 2021
 */
public class ISASimulation
{
    static class TriggerSource
    {
        String systemUID;
        String outputName;
        Consumer<TriggerEvent> listener;
        Subscription sub;
        Instant lastTriggerTime = Instant.EPOCH;
    }
    
    
    static class TriggerEvent
    {
        Instant time;
        Point pos;
    }
    
    
    static class RandomWalk
    {
        double value;
        double stepSigma;
        
        RandomWalk(double initVal, double initSigma, double stepSigma, double min, double max)
        {
            this.value = initVal + Math.random() * initSigma;
            this.stepSigma = stepSigma;
        }
        
        double next()
        {
            value += Math.random() * stepSigma;
            return value;
        }
    }
    
    
    ISensorHub hub;
    Clock clock;
    List<TriggerSource> eventSources = new ArrayList<>();
    GeometryFactory jts = new GeometryFactory();
    
    
    ISASimulation(ISADriver driver)
    {
        this.hub = driver.getParentHub();
        this.clock = new SynchronizedClock(hub, "urn:osh:sensor:uas:predator001", "sensorLocation");
    }
    
    
    void addTriggerSource(String systemUID, String outputName, Consumer<TriggerEvent> listener)
    {
        var newSrc = new TriggerSource();
        newSrc.systemUID = systemUID;
        newSrc.outputName = outputName;
        newSrc.listener = listener;
        eventSources.add(newSrc);
    }
    
    
    void start()
    {
        for (var src: eventSources)
        {
            hub.getEventBus().newSubscription(ObsEvent.class)
            .withTopicID(EventUtils.getDataStreamDataTopicID(src.systemUID, src.outputName))
            .subscribe(e -> {
                if (!e.getObservations().isEmpty()) {
                    var obs = e.getObservations().iterator().next();
                    
                    // throttle at 1Hz
                    if (obs.getPhenomenonTime().toEpochMilli() - src.lastTriggerTime.toEpochMilli() < 1000)
                        return;
                    
                    var result = obs.getResult();
                    var lat = result.getDoubleValue(1);
                    var lon = result.getDoubleValue(2);
                    
                    var t = new TriggerEvent();
                    t.time = src.lastTriggerTime = obs.getPhenomenonTime();
                    t.pos = jts.createPoint(new Coordinate(lat, lon));
                    src.listener.accept(t);
                }
            })
            .thenAccept(s -> {
                src.sub = s;
                s.request(Long.MAX_VALUE);
            });
        }
    }
    
    
    void stop()
    {
        for (var src: eventSources)
        {
            if (src.sub != null)
                src.sub.cancel();
            src.lastTriggerTime = Instant.EPOCH;
        }
        
        if (clock != null && clock instanceof Closeable)
        {
            try {
                ((Closeable)clock).close();
            } catch (IOException e) {
            }
            clock = null;
        }
    }
}
