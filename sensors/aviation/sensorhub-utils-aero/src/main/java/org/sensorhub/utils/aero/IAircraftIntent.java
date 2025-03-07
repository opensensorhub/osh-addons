/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2025 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.utils.aero;

import java.time.Instant;


/**
 * <p>
 * Read-only interface for aircraft intent
 * </p>
 *
 * @author Alex Robin
 * @since Jan 27, 2025
 */
public interface IAircraftIntent extends IAircraftIdentification
{

    /**
     * @return Target Altitude (ft, pressure altitude)
     */
    double getTargetAltitude();
    
    
    /**
     * @return ICAO code of the next waypoint (null if unknown)
     */
    String getNextWaypoint();
    
    
    /**
     * @return ETA at next waypoint (UTC, null if unknown)
     */
    Instant getNextWaypointETA();
    
    
    /**
     * @return ICAO code of the following waypoint (i.e. waypoint after next, null if unknown)
     */
    String getFollowingWaypoint();
}
