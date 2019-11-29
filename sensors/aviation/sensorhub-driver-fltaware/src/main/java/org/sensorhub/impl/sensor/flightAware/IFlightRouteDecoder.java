/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2019 Delta Air Lines, Inc. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.flightAware;

import java.util.List;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.flightAware.DecodeFlightRouteResponse.Waypoint;


/**
 * Interface for Firehose flight route decoders 
 * @author Alex Robin
 * @since Nov 26, 2019
 */
public interface IFlightRouteDecoder
{

    /**
     * Expand the route into a list of navigation points
     * @param fltPlan Flight plan object
     * @param route Normalized route string
     * @return return List of navigation points with lat/lon coordinates
     * @throws SensorHubException if decoding failed
     */
    public List<Waypoint> decode(FlightObject fltPlan, String route) throws SensorHubException;
}
