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

import org.sensorhub.impl.sensor.flightAware.FlightAwareDriver.FlightInfo;
import org.slf4j.Logger;
import org.vast.util.Asserts;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;


/**
 * Process Flight Plan messages from FlightAware firehose feed.
 * <p>
 * We also maintain a cache of previously processed flight info objects for 2 reasons:
 * 1. Some position messages are missing the dest field so we need to look it up
 *    from the cache using the faFlightId
 * 2. Many flightplan messages contain duplicate routes so we use the cache to
 *    detect duplicates and avoid sending a new message
 * </p>
 * @author Tony Cook
 * @author Alex Robin
 */
public class ProcessPlanTask implements Runnable
{
    Logger log;    
    FlightObject fltPlan;
    MessageHandler msgHandler;
    Cache<String, FlightInfo> flightCache;
    IFlightRouteDecoder flightRouteDecoder;
    
    public ProcessPlanTask(MessageHandler msgHandler, FlightObject fltObj) {
        this.log = msgHandler.log;
        this.msgHandler = msgHandler;
        this.flightCache = Asserts.checkNotNull(msgHandler.driver.flightCache, Cache.class);
        this.flightRouteDecoder = msgHandler.driver.flightRouteDecoder;
        this.fltPlan = fltObj;        
    }
    
    @Override
    public void run() {
        try {            
            // skip if airport codes or route are missing
            if (Strings.nullToEmpty(fltPlan.orig).trim().isEmpty() ||
                Strings.nullToEmpty(fltPlan.dest).trim().isEmpty() ||
                Strings.nullToEmpty(fltPlan.route).trim().isEmpty())
                return;
            
            // save at least the dest airport in cache
            FlightInfo cachedInfo = flightCache.get(fltPlan.id, () -> {
                FlightInfo info = new FlightInfo();
                info.dest = fltPlan.dest;
                return info;
            });
            
            // keep only flight plans with status flag set to "F: filed" or "A: active"
            if (!("F".equalsIgnoreCase(fltPlan.status)))// || "A".equalsIgnoreCase(fltPlan.status)))
                return;
            
            // need to synchronize on cache entry so we can properly detect duplicates
            synchronized (cachedInfo)
            {
                // if route hasn't changed, don't process further
                String newRoute = normalizeRouteString(fltPlan);
                if (cachedInfo.route == null || !newRoute.equals(cachedInfo.route))
                {
                    if (cachedInfo.route != null)
                        log.debug("{}_{}: Route changed ({}): {} -> {}", fltPlan.ident, fltPlan.dest, fltPlan.facility_name, cachedInfo.route, newRoute);
                    
                    // decode route or just use already decoded one
                    // i.e. it may have been decoded by another server and sent to us via mq
                    if (flightRouteDecoder != null && fltPlan.decodedRoute == null)
                    {
                        fltPlan.decodedRoute = flightRouteDecoder.decode(fltPlan, newRoute);
                        if (log.isDebugEnabled())
                            log.debug("{}_{}: Route decoded ({}): {} -> {}", fltPlan.ident, fltPlan.dest, fltPlan.facility_name, newRoute, fltPlan.decodedRoute);
                    }
                    
                    // publish flight plan
                    cachedInfo.route = newRoute;
                    msgHandler.newFlightPlan(fltPlan);
                }
                else
                {
                    log.debug("{}_{}: Skipping duplicate route", fltPlan.ident, fltPlan.dest);
                }
            }
            
        } catch (Exception e) {
            log.error("Error while processing flight plan", e);
        }     
    }
    
    
    String normalizeRouteString(FlightObject fltObj)
    {
        log.debug("{}_{}: FA route is: {}", fltObj.ident, fltObj.dest, fltObj.route);
        return fltObj.route
            .trim()
            .replaceAll("/[0-9]{4}$", "")
            .replaceAll("/|\\*", "");
    }
}
