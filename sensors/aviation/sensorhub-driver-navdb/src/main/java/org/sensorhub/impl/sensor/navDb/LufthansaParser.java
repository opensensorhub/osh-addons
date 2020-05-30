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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.sensorhub.impl.sensor.navDb.NavDbEntry.Type;

public class LufthansaParser
{
	// DDMMSS.SS or DDDMMSS.ss
	// N30114030,W097401160
	private static Double parseCoordString(String s) throws NumberFormatException {
		char hemi = s.charAt(0);
		String dd, mm, ss;
		if(hemi=='N' || hemi=='S') {
			dd = s.substring(1,3);
			mm = s.substring(3,5);
			ss = s.substring(5,9);
		} else if(hemi=='E' || hemi=='W') {
			dd = s.substring(1,4);
			mm = s.substring(4,6);
			ss = s.substring(6,10);
		} else {
			throw new NumberFormatException("Cannot parse coord Str: " + s);
		}
		double deg = Double.parseDouble(dd);
		double min = Double.parseDouble(mm);
		double sec = Double.parseDouble(ss);
		sec = sec/100.;

		double coord = deg + (min/60.) + (sec/3600.);
		if(hemi == 'S' || hemi == 'W')
			coord = -1 * coord;
		return coord;
	}

	private static NavDbEntry parseAirport(String l) {
		String region = l.substring(1,4);
		String icao = l.substring(6, 10);
		String lats = l.substring(32,41);
		String lons = l.substring(41,51);
		String name = l.substring(93,123).trim();
		double lat;
		double lon;
		try {
			lat = parseCoordString(lats);
			lon = parseCoordString(lons);
		} catch (NumberFormatException e) {
			System.err.println("ParseAIR EX: " + l);
			return null;
		}
		NavDbEntry airport = new NavDbEntry(Type.AIRPORT, icao, lat, lon);
		airport.name = name;
		airport.region = region;
		airport.id = airport.icao;
		return airport;
	}

	private static NavDbEntry parseWaypoint(String l) {
		String region = l.substring(1,4);
		String id = l.substring(13, 18).trim();
		String lats = l.substring(32,41);
		String lons = l.substring(41,51);
		String name = l.substring(98,122).trim();
		double lat;
		double lon;
		try {
			lat = parseCoordString(lats);
			lon = parseCoordString(lons);
		} catch (NumberFormatException e) {
			System.err.println("ParseWP EX: " + l);
			return null;
		}
		NavDbEntry waypoint = new NavDbEntry(Type.WAYPOINT, id, lat, lon);
		waypoint.name = name;
		waypoint.region = region;
		return waypoint;
	}

	private static NavDbEntry parseTerminalWaypoint(String l) {
		String region = l.substring(1,4);
		String icao = l.substring(6, 10);
		String id= l.substring(13, 18).trim();
		String lats = l.substring(32,41);
		String lons = l.substring(41,51);
		String name = l.substring(98,122).trim();
		double lat;
		double lon;
		try {
			lat = parseCoordString(lats);
			lon = parseCoordString(lons);
		} catch (NumberFormatException e) {
			System.err.println("ParseTWP EX: " + l);
			return null;
		}
		NavDbEntry waypoint = new NavDbEntry(Type.WAYPOINT, icao, lat, lon);
		waypoint.name = name;
		waypoint.region = region;
		waypoint.id = id;
		return waypoint;
	}

	private static NavDbEntry parseNavaid(String l) {
		String region = l.substring(1,4);
		String icao = l.substring(13, 17).trim();
		String lats = l.substring(32,41);
		String lons = l.substring(41,51);
		String name = l.substring(93,123).trim();
		double lat;
		double lon;
		try {
			lat = parseCoordString(lats);
			lon = parseCoordString(lons);
		} catch (NumberFormatException e) {
//			System.err.println("ParseNV EX: " + l);
			return null;
		}

		NavDbEntry navaid = new NavDbEntry(Type.NAVAID, icao, lat, lon);
		navaid.name = name;
		navaid.region = region;
		navaid.id = icao;
		return navaid;
	}


	public static List<NavDbEntry> getNavDbEntries(Path dbPath) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(dbPath.toString()));
		List<NavDbEntry> entries = new ArrayList<>();
		int lineCnt = 1;
		boolean eof = false;
		int idStartIdx = -1, idStopIdx = -1;
		String prevId = "";
		char continuationNum = 0;
		while(true) {
			String line = br.readLine();
			if(line == null)
				break;
			NavDbEntry entry = null;
			switch(line.charAt(4)) {
			case 'P':
				//  if character at 5 is not blank, this is NOT primary airport rec. Skip it
				if(line.charAt(5) != ' ')
					continue;
				String icao = line.substring(6, 10);
				char subCode = line.charAt(12);
				if(subCode == 'C') {
					entry = parseTerminalWaypoint(line);
					//  terminal waypoint has 2nd line
					br.readLine();
				} else if(prevId.equals(icao)) {
					continue;
				} else {
					entry = parseAirport(line);
				}
				prevId = entry.icao;
				break;
			case 'D':
				continuationNum = line.charAt(21);
				if(!(continuationNum == '1'))
					continue;
				entry = parseNavaid(line);
				break;
			case 'E':
				if(line.charAt(5) == 'A') {
					continuationNum = line.charAt(21);
					if(!(continuationNum == '1'))
						continue;
					entry = parseWaypoint(line);
					break;
				} else if(line.charAt(5) == 'R') {
					// parseAirway one day
					continue;
				} else {
					continue;
				}
			default:
				continue;
			}
			if(entry == null)
				continue;

			entries.add(entry);
		}
		return entries;
	}

	public static List<NavDbEntry> filterEntries(List<NavDbEntry> entries, List<String> regions) throws IOException {
		List<NavDbEntry> filtered = new ArrayList<>();
		for(NavDbEntry e: entries) {
			if(regions.contains(e.region))
				filtered.add(e);
		}

		return filtered;
	}	

	public static List<NavDbEntry> filterEntries(List<NavDbEntry> entries, Type t) {
		List<NavDbEntry> filtered = new ArrayList<>();
		for(NavDbEntry e: entries) {
			if(e.type == t)
				filtered.add(e);
		}

		return filtered;
	}
}