package org.sensorhub.impl.sensor.flightAware;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import com.google.common.base.Strings;
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
    Cache<String, FlightPlan> flightPlanCache; // key is faFlightID
        
    
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
    public FlightPlan decode(FlightObject fltObj)
    {
        try
        {
            // skip if origin, destination or route is missing
            if (Strings.isNullOrEmpty(fltObj.orig) ||
                Strings.isNullOrEmpty(fltObj.dest) ||
                Strings.isNullOrEmpty(fltObj.route))
                return null;
            
            // canonicalize route string
            String newRoute = normalizeRouteString(fltObj);
            
            // try to get the one in cache
            FlightPlan cachedPlan = flightPlanCache.getIfPresent(fltObj.id);
            String cachedRoute = cachedPlan == null ? null : cachedPlan.routeString;
            
            // if route hasn't changed, don't publish new flight plan
            if (cachedRoute != null && newRoute.equals(cachedRoute))
            {
                log.debug("{}_{}: Skipping duplicate route", fltObj.ident, fltObj.dest);
                return null;
            }
            
            // if new route
            if (cachedRoute != null)
            {
                Instant cachedDt = Instant.ofEpochSecond((long)cachedPlan.departureTime);
                Instant newDt = Instant.ofEpochSecond(Long.parseLong(fltObj.fdt));
                log.debug("{}_{}: Route change ({}): {}@{} -> {}@{}", fltObj.ident, fltObj.dest, fltObj.facility_name, cachedRoute, cachedDt, newRoute, newDt);
            }
            FlightPlan plan = api.getFlightPlan(fltObj.id);
            if (plan == null)
                return null;
            
            // by convention, use message receive time as issueTime
            // since Flight Aware does not include it in feed
            plan.issueTime = System.currentTimeMillis() / 1000;
            plan.originAirport = fltObj.orig;
            plan.destinationAirport = fltObj.dest;
            plan.departureTime = fltObj.getDepartureTime();
            plan.routeString = newRoute;
            
            flightPlanCache.put(fltObj.id, plan);
            
            if (log.isDebugEnabled())
                log.debug("{}_{}: New route decoded ({}): {} -> {}", fltObj.ident, fltObj.dest, fltObj.facility_name, newRoute, plan.getWaypointNames());
            
            return plan;
        }
        catch (IOException e)
        {
            log.error("Error while decoding flight route", e);
            return null;
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
