/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2018 Delta Air Lines, Inc. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.navDb;

public class NavDbEntry
{
	public enum Type { AIRPORT, NAVAID, WAYPOINT, AIRWAY};  // AIRWAYs will have multiple Lat/Lons
	public Type type;
	public String icao;
	public String id;  // id is icao for airports, VOR id for navaids, wayptId for waypoints
	public String name;
	public Double lat;
	public Double lon;
	public String latStr;
	public String lonStr;
	public String region;  // Three char region id
	
	public NavDbEntry(Type type, String id, double lat, double lon) throws NumberFormatException{
		this.type  = type;
		this.id = id.trim();
		this.lat = lat;
		this.lon = lon;
		if(type == Type.AIRPORT || type == Type.WAYPOINT)
			icao = id;
	}
	
	@Override
	public String toString() {
		return type + "," + region + "," + icao + "," + id + ","  + name + "," + lat + "," + lon;
	}
}
