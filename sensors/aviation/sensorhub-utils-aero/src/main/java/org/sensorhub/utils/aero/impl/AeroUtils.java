/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2025 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.utils.aero.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.event.IEventListener;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.system.ISystemDriver;
import org.sensorhub.api.system.ISystemGroupDriver;
import org.sensorhub.utils.Lambdas;
import org.sensorhub.utils.aero.IFlightIdentification;
import org.vast.ogc.om.MovingFeature;
import org.vast.ogc.om.SamplingPoint;
import org.vast.sensorML.SMLHelper;
import org.vast.swe.SWEConstants;
import com.google.common.cache.CacheBuilder;
import net.opengis.sensorml.v20.AbstractProcess;


/**
 * <p>
 * Common aeronautical functions and databases.
 * </p>
 *
 * @author Alex Robin
 * @since Feb 21, 2025
 */
public class AeroUtils
{
    // System URIs
    public static final String AERO_SYSTEM_URI_PREFIX = "urn:osh:system:aero:";
    public static final String AERO_FOI_REGISTRY_UID = AERO_SYSTEM_URI_PREFIX + "foiregistry";
    
    // FOI URIs
    public static final String AERO_FOI_URI_PREFIX = "urn:osh:foi:aero:";
    public static final String FOI_TAIL_UID_PREFIX = AERO_FOI_URI_PREFIX + "tail:";
    public static final String FOI_FLIGHT_UID_PREFIX = AERO_FOI_URI_PREFIX + "flight:";
    public static final String FOI_AIRPORT_UID_PREFIX = AERO_FOI_URI_PREFIX + "airport:";
        
    public static final Pattern FLIGHTID_REGEX = Pattern.compile("[A-Z0-9]{3}[0-9]{1,4}_[A-Z]{4}_[0-9]{4}-[0-9]{2}-[0-9]{2}");
    
    
    // map of ICAO airport codes to time zone identifiers
    static Map<String, String> icaoTzData;
        
    // cache for most recent FOI IDs to speed things up
    static Map<String, String> latestFois = CacheBuilder.newBuilder()
        .concurrencyLevel(2)
        .expireAfterAccess(1, TimeUnit.HOURS)
        .<String, String>build().asMap();
    
    static AtomicBoolean registryCreated = new AtomicBoolean();
    
    
    /**
     * @return A map of ICAO airport codes to time zone identifiers.
     */
    public static synchronized Map<String, String> getAirportTimeZones()
    {
        if (icaoTzData == null)
        {
            icaoTzData = new HashMap<>();
            
            // icao.tzmap downloaded from https://www.fresse.org/dateutils/tzmaps.html
            var is = AeroUtils.class.getResourceAsStream("icao.tzmap.txt");
            try (var reader = new BufferedReader(new InputStreamReader(is)))
            {
                String line;
                while ((line = reader.readLine()) != null)
                {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        var tokens = line.split("\\t");
                        icaoTzData.put(tokens[0], tokens[1]);
                    }
                }
            }
            catch (Exception e)
            {
                throw new IllegalStateException("Error reading airport time zone data", e);
            }
        }
        
        return icaoTzData;
    }
    
    
    public static synchronized void ensureAeroFoiRegistry(ISensorHub hub)
    {
        if (registryCreated.compareAndSet(false, true))
        {
            var reg = hub.getSystemDriverRegistry();
            if (reg != null && !reg.isRegistered(AERO_FOI_REGISTRY_UID))
            {
                var validTime = OffsetDateTime.parse("2025-01-01T00:00:00Z");
                
                var sml = new SMLHelper();
                var sys = sml.createSimpleProcess()
                    .uniqueID(AERO_FOI_REGISTRY_UID)
                    .name("Aero FOI Registry")
                    .description("Parent system for all common aviation FOIs such as flights and aircrafts")
                    .definition(SWEConstants.DEF_SAMPLER)
                    .validFrom(validTime)
                    .build();
                
                reg.register(new ISystemDriver() {
                    public void registerListener(IEventListener listener) { }
                    public void unregisterListener(IEventListener listener) { }
                    public String getName() { return sys.getName(); }
                    public String getDescription() { return sys.getDescription(); }
                    public String getUniqueIdentifier() { return sys.getUniqueIdentifier(); }
                    public String getParentSystemUID() { return null; }
                    public ISystemGroupDriver<? extends ISystemDriver> getParentSystem() { return null; }
                    public AbstractProcess getCurrentDescription() { return sys; }
                    public long getLatestDescriptionUpdate() { return validTime.toEpochSecond()*1000; }
                    public boolean isEnabled() { return true; }
                });
            }
        }
    }
    
    
    public static String ensureFlightFoi(IModule<?> m, String flightId)
    {
        return ensureFlightFoi(m.getParentHub(), flightId);
    }
    
    
    public static String ensureFlightFoi(ISensorHub hub, String flightId)
    {
        if (hub.getSystemDriverRegistry() != null)
        {
            ensureAeroFoiRegistry(hub);
            
            String uid = FOI_FLIGHT_UID_PREFIX + flightId;
            
            // register FOI if ID is not in cache
            return latestFois.computeIfAbsent(uid, Lambdas.checked(fid -> {
                
                // generate small FOI object
                MovingFeature foi = new MovingFeature();
                foi.setId(flightId);
                foi.setUniqueIdentifier(fid);
                foi.setName("Flight " + flightId);
                
                // register it
                hub.getSystemDriverRegistry().register(AERO_FOI_REGISTRY_UID, foi).get();
                return fid;
            }));
        }
        
        return null;
    }
    
    
    public static String ensureTailFoi(IModule<?> m, String tailId)
    {
        return ensureTailFoi(m.getParentHub(), tailId);
    }
    
    
    public static String ensureTailFoi(ISensorHub hub, String tailId)
    {
        if (hub.getSystemDriverRegistry() != null)
        {
            ensureAeroFoiRegistry(hub);
            
            String uid = FOI_TAIL_UID_PREFIX + tailId;
            
            // register FOI if ID is not in cache
            return latestFois.computeIfAbsent(uid, Lambdas.checked(fid -> {
                
                // generate small FOI object
                MovingFeature foi = new MovingFeature();
                foi.setId(tailId);
                foi.setUniqueIdentifier(fid);
                foi.setName("Tail " + tailId);
                
                // register it
                hub.getSystemDriverRegistry().register(AERO_FOI_REGISTRY_UID, foi).get();
                return fid;
            }));
        }
        
        return null;
    }
    
    
    public static String ensureAirportFoi(IModule<?> m, String icao)
    {
        return ensureAirportFoi(m.getParentHub(), icao);
    }
    
    
    public static String ensureAirportFoi(ISensorHub hub, String icao)
    {
        if (hub.getSystemDriverRegistry() != null)
        {
            ensureAeroFoiRegistry(hub);
        
            String uid = FOI_AIRPORT_UID_PREFIX + icao;
            
            // register FOI if ID is not in cache
            return latestFois.computeIfAbsent(uid, Lambdas.checked(fid -> {
                
                // generate small FOI object
                var foi = new SamplingPoint();
                foi.setId(icao);
                foi.setUniqueIdentifier(fid);
                foi.setName("Airport " + icao);
                
                // register it
                hub.getSystemDriverRegistry().register(AERO_FOI_REGISTRY_UID, foi).get();
                return fid;
            }));
        }
        
        return null;
    }
    
    
    public static String getFlightID(String flightNum, String destIcao, LocalDate flightDate)
    {
        return String.format("%s_%s_%04d%02d%02d", flightNum, destIcao,
            flightDate.getYear(), flightDate.getMonthValue(), flightDate.getDayOfMonth());
    }
    
    
    public static IFlightIdentification parseFlightIdentification(String flightId)
    {
        // validate format 
        if (!FLIGHTID_REGEX.matcher(flightId).matches())
            throw new IllegalArgumentException("Invalid flight ID: " + flightId);
        
        var tokens = flightId.split("_");
        
        return new IFlightIdentification()
        {
            public String getFlightNumber() { return tokens[0]; }                        
            public String getOriginAirport() { return null; }            
            public String getDestinationAirport() { return tokens[1]; }
            
            public Instant getFlightDate()
            {
                var date = LocalDate.parse(tokens[2]);
                return date.atStartOfDay().toInstant(ZoneOffset.UTC);
            }
        };
    }
}
