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

import java.io.IOException;
import java.util.List;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.flightAware.DecodeFlightRouteResponse.DecodeFlightRouteResult;
import org.sensorhub.impl.sensor.flightAware.DecodeFlightRouteResponse.Waypoint;
import org.slf4j.Logger;
import com.google.common.base.Strings;


/**
 * Decoder implementation using the FlightXML API to expand the route
 * @author Alex Robin
 * @since Nov 26, 2019
 */
public class FlightRouteDecoderFlightXML implements IFlightRouteDecoder
{
    Logger log;
    FlightAwareApi api;
        
    
    public FlightRouteDecoderFlightXML(FlightAwareDriver driver)
    {
        this.log = driver.getLogger();
        String user = driver.getConfiguration().userName;
        String passwd = driver.getConfiguration().password;
        this.api = new FlightAwareApi(user, passwd);
    }
    
    
    @Override
    public List<Waypoint> decode(FlightObject fltPlan, String route) throws SensorHubException
    {
        try
        {
            long t0 = System.currentTimeMillis();
            DecodeFlightRouteResult res = api.decodeFlightRoute(fltPlan.id);
            long t1 = System.currentTimeMillis();
            log.debug("DecodeFlightRoute call took {}ms", t1-t0);
            
            if (res == null || res.data == null || res.data.isEmpty())
                throw new SensorHubException("Empty response returned by FlightXML API");
            
            // set altitude according to filed altitude
            int i = 0; 
            for (Waypoint wp: res.data)
            {
                if (i == 0 || i == res.data.size()-1)
                    wp.altitude = 0.0;
                else if (!Strings.nullToEmpty(fltPlan.alt).trim().isEmpty())
                    wp.altitude = Double.parseDouble(fltPlan.alt);
                i++;
            }
            
            return res.data;
        }
        catch (IOException e)
        {
            throw new SensorHubException("Error calling FlightXML API", e);
        }
    }

}
