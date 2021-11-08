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
import org.sensorhub.impl.sensor.isa.ISASensor.StatusType;
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
    
    
    ISADriver driver;
    ISensorHub hub;
    Clock clock;
    List<TriggerSource> eventSources = new ArrayList<>();
    GeometryFactory jts = new GeometryFactory();
    long lastTime = Long.MIN_VALUE;
    
    
    ISASimulation(ISADriver driver)
    {
        this.driver = driver;
        this.hub = driver.getParentHub();
    }
    
    
    void init()
    {
        // setup simulation clock to synchronize with MISB
        // + set handler to reset outputs when simulation restarts
        this.clock = new SynchronizedClock(hub, "urn:osh:sensor:uas:predator001", "sensorLocation", time -> {
            
            // reset all sensor output time if simulation is restarting
            if (time.toEpochMilli() < lastTime)
            {
                for (var sensor: driver.allSensors.values())
                {
                    for (var output: sensor.getOutputs().values())
                    {
                        if (output instanceof ISAOutput)
                            ((ISAOutput)output).nextRecordTime = Long.MIN_VALUE;
                    }
                }
            }
            
            lastTime = time.toEpochMilli();
        });
        
        var timeStamp = Instant.ofEpochMilli(driver.getConfiguration().lastUpdated.getTime() / 1000);
        double[][] sensorLocations;
        
        // radio sensors
        sensorLocations = new double[][] {
            { 34.706226, -86.671218, 193 },
            { 34.698848, -86.671382, 193 },
            { 34.694702, -86.671473, 193 },
            { 34.686162, -86.671778, 193 }
        };
        
        for (int i = 0; i < sensorLocations.length; i++)
        {
            var loc = sensorLocations[i];
            driver.registerSensor(new RadiologicalSensor(driver, String.format("RADIO%03d", i+1))
                .addTriggerSource("urn:osh:process:vmti", "targetLocation", RadioReadingOutput.RADIO_MATERIAL_CATEGORY_CODE[1], 1200.0f)
                .setManufacturer("Radiological Sensors, Inc.")
                .setModelNumber("RD123")
                .setSoftwareVersion("FW21.23.89")
                .addStatusOutputs(StatusType.RADIO)
                .setFixedLocationWithRadius(timeStamp, loc[0], loc[1], loc[2], 100));
        }
        
        // bio sensors
        sensorLocations = new double[][] {
            { 34.708926, -86.679157, 193 },
            { 34.698967, -86.675548, 193 },
            { 34.699533, -86.664192, 193 },
            { 34.688416, -86.677495, 193 }
        };
        
        for (int i = 0; i < sensorLocations.length; i++)
        {
            var loc = sensorLocations[i];
            driver.registerSensor(new BiologicalSensor(driver, String.format("BIO%03d", i+1))
                .setManufacturer("Bio Sensors, Inc.")
                .setModelNumber("BD456")
                .setSoftwareVersion("FW2021.32.156")
                .addStatusOutputs(StatusType.RADIO)
                .setFixedLocation(timeStamp, loc[0], loc[1], loc[2]));
        }
        
        // chem sensors
        sensorLocations = new double[][] {
            { 34.695782, -86.675593, 193 },
            { 34.691229, -86.666021, 193 }
        };
        
        for (int i = 0; i < sensorLocations.length; i++)
        {
            var loc = sensorLocations[i];
            driver.registerSensor(new ChemicalSensor(driver, String.format("CHEM%03d", i+1))
                .setManufacturer("Chem Sensors, Inc.")
                .setModelNumber("CD456")
                .setSoftwareVersion("FW2021.32.156")
                .addStatusOutputs(StatusType.RADIO)
                .setFixedLocation(timeStamp, loc[0], loc[1], loc[2]));
        }
        
        // weather sensors
        sensorLocations = new double[][] {
            { 34.677990, -86.682758, 193 },
            { 34.705657, -86.674131, 193 }
        };
        
        for (int i = 0; i < 2; i++)
        {
            var loc = sensorLocations[i];
            driver.registerSensor(new MeteorologicalSensor(driver, String.format("ATM%03d", i+1))
                .setManufacturer("Vaisala, Inc.")
                .setModelNumber("AWS310")
                .setSoftwareVersion("V51.458a")
                .addStatusOutputs(StatusType.ELEC_DC, StatusType.RADIO)
                .setFixedLocation(timeStamp, loc[0], loc[1], loc[2]));
        }
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
                    
                    // reset if simulation loops around
                    if (obs.getPhenomenonTime().toEpochMilli() < src.lastTriggerTime.toEpochMilli())
                        src.lastTriggerTime = Instant.EPOCH;
                        
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
