/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2018 Delta Air Lines, Inc. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.utils.grid;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class EarthcastUtil
{
	// ECT_NCST_DELTA_NLDN_CG_6_5km.201710271550.grb2
	// ECT_NCST_DELTA_MESH_6_5km.201710271540.grb2
	// ECT_NCST_DELTA_GTGTURB_6_5km.201710271100.grb2
	public static long computeTime(String filename) throws IOException {
		int dotIdx = filename.indexOf('.');
		if(dotIdx == -1) {
			throw new IOException("Could not compute timestamp from filename: " + filename);
		}
		String datestr = filename.substring(dotIdx + 1);
		String yrs = datestr.substring(0,4);
		String mons = datestr.substring(4,6);
		String days = datestr.substring(6,8);
		String hrs = datestr.substring(8,10);
		String mins = datestr.substring(10,12);
		int yr = Integer.parseInt(yrs);
		int mon = Integer.parseInt(mons);
		int day = Integer.parseInt(days);
		int hr = Integer.parseInt(hrs);
		int min = Integer.parseInt(mins);
		//  Surely there is an easier way to get the timestamp... 
		//  partial issue is that ZonedDateTime can't seem to see toEpochMilli() method, 
		//  likely due to clash with joda time dependencies
		LocalDateTime date = LocalDateTime.of(yr, mon, day, hr, min);
		ZonedDateTime gmtDate = ZonedDateTime.of(date, ZoneId.of("UTC"));
		Instant instant = Instant.from(gmtDate);
		long time = instant.toEpochMilli();
		return time/1000;
	}

}
