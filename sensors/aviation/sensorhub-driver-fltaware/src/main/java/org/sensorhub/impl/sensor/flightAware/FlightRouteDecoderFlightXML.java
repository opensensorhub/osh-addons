package org.sensorhub.impl.sensor.flightAware;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.sensorhub.impl.sensor.flightAware.DecodeFlightRouteResponse.DecodeFlightRouteResult;
import org.slf4j.Logger;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;


/**
 * Decoder implementation using the FlightXML API to expand the route
 * @author Alex Robin
 * @since Nov 26, 2019
 */
public class FlightRouteDecoderFlightXML implements IFlightRouteDecoder
{
    private static final int MAX_FP_CACHE_SIZE = 10000;
    private static final int MAX_FP_CACHE_AGE = 24; // hours
    
    Logger log;
    FlightAwareApi api;
    Cache<String, String> flightPlanCache; // faFlightID -> route string
        
    
    public FlightRouteDecoderFlightXML(FlightAwareDriver driver)
    {
        this.log = driver.getLogger();
        String user = driver.getConfiguration().userName;
        String passwd = driver.getConfiguration().password;
        this.api = new FlightAwareApi(user, passwd);
        
        this.flightPlanCache = CacheBuilder.newBuilder()
                .maximumSize(MAX_FP_CACHE_SIZE)
                .concurrencyLevel(2)
                .expireAfterWrite(MAX_FP_CACHE_AGE, TimeUnit.HOURS)
                .build();
    }
    
    
    @Override
    public boolean decode(FlightObject fltPlan)
    {
        try
        {
            // canonicalize route string
            String newRoute = normalizeRouteString(fltPlan);
            
            // check if we have a route in cache
            String cachedRoute = flightPlanCache.getIfPresent(fltPlan.id);
            
            // if route hasn't changed, don't publish new flight plan
            if (cachedRoute != null && newRoute.equals(cachedRoute))
            {
                log.debug("{}_{}: Skipping duplicate route", fltPlan.ident, fltPlan.dest);
                return false;
            }
            
            // if it's a new route, decode and cache it
            if (cachedRoute != null)
                log.debug("{}_{}: Route change ({}): {} -> {}", fltPlan.ident, fltPlan.dest, fltPlan.facility_name, cachedRoute, newRoute);
            
            DecodeFlightRouteResult res = api.decodeFlightRoute(fltPlan.id);
            if (res == null)
                return false;
            
            fltPlan.decodedRoute = res.data;
            flightPlanCache.put(fltPlan.id, newRoute);            
            if (log.isDebugEnabled())
                log.debug("{}_{}: New route decoded ({}): {} -> {}", fltPlan.ident, fltPlan.dest, fltPlan.facility_name, newRoute, res.getWaypointNames());
            
            return true;
        }
        catch (IOException e)
        {
            log.error("Error while decoding flight route", e);
            return false;
        }
    }
    
    
    String normalizeRouteString(FlightObject fltObj)
    {
        log.debug("{}_{}: Raw route is: {}", fltObj.ident, fltObj.dest, fltObj.route);
        return fltObj.route
            .trim()
            .replaceAll("/[0-9]{4}$", "")
            .replaceAll("/|\\*", "");
    }

}
