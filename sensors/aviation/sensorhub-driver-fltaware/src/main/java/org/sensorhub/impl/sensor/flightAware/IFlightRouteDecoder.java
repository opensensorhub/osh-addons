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


/**
 * Interface for Firehose flight route decoders 
 * @author Alex Robin
 * @since Nov 26, 2019
 */
public interface IFlightRouteDecoder
{

    /**
     * Expand the route into a list of waypoints
     * @param fltObj object containing the route
     * @return the list of waypoints, each with at least a name and coordinates
     */
    public FlightPlan decode(FlightObject fltObj);
}
