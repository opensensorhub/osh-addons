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

import java.util.List;


public class DecodeFlightRouteResponse extends FlightAwareResult
{
	DecodeFlightRouteResult DecodeFlightRouteResult = new DecodeFlightRouteResult();  // Gson class name doesn't matter, just the variable name.  And Case matters!!!
    
	static class DecodeFlightRouteResult {
		int next_offset;
		List<Waypoint> data;
	    
	    @Override
	    public String toString() {
	        StringBuilder b = new StringBuilder();
	        for (Waypoint d: data) {
	            b.append(d.name + " (" + d.type + "," + d.latitude + "," + d.longitude + ")\n");
	        }
	        return b.toString();
	    }

	    public String getWaypointNames() {
	        StringBuilder b = new StringBuilder();
	        for (Waypoint d: data) {
	            b.append(d.name).append(',');
	        }
	        b.setLength(b.length()-1);
	        return b.toString();
	    }
	}
	
	static class Waypoint {
        String name;
        String type;
        double latitude;
        double longitude;

        public Waypoint(String name, String type, double lat, double lon) {
            this.name = name;
            this.type = type;
            this.latitude = lat;
            this.longitude = lon;
        }
    }
}
