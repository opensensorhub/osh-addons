/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.ndbc;

/**
 * 
 * @author tcook
 *
 */

public class BuoyDataRecord 
{
	// #STN       LAT      LON  YYYY MM DD hh mm WDIR WSPD   GST WVHT  DPD APD MWD   PRES  PTDY  ATMP  WTMP  DEWP  VIS   TIDE
	// #text      deg      deg   yr mo day hr mn degT  m/s   m/s   m   sec sec degT   hPa   hPa  degC  degC  degC  nmi     f
	String id;
	Double lat;
	Double lon;
	long timeMs; //  Both Realtime and Historical files show times in UTC only. See the Acquisition Time help topic for a more detailed description of observation times.
	Integer windDir; // Wind direction (the direction the wind is coming from in degrees clockwise from true N) during the same period used for WSPD. 
	Double windSpeed; // Wind speed (m/s) averaged over an eight-minute period for buoys and a two-minute period for land stations. Reported Hourly. 
	Double windGust; // Peak 5 or 8 second gust speed (m/s) measured during the eight-minute or two-minute period. The 5 or 8 second period can be determined by payload
	Double wvht; // Significant wave height (meters) is calculated as the average of the highest one-third of all of the wave heights during the 20-minute sampling period.
	Double dpd; // 	Dominant wave period (seconds) is the period with the maximum wave energy. 
	Double apd; // 	Average wave period (seconds) of all waves during the 20-minute period
	Double mwd; // 	The direction from which the waves at the dominant period (DPD) are coming. The units are degrees from true North, increasing clockwise, with North as 0 (zero) degrees and East as 90 degrees. 
	Double pressure; // Sea level pressure (hPa). For C-MAN sites and Great Lakes buoys, the recorded pressure is reduced to sea level using the method described in NWS Technical Procedures Bulletin 291 (11/14/80). 
	Double ptdy; // Pressure Tendency is the direction (plus or minus) and the amount of pressure change (hPa)for a three hour period ending at the time of observation. (not in Historical files)
	Double airTemp; // Air temperature (Celsius). For sensor heights on buoys, see Hull Descriptions. For sensor heights at C-MAN stations, see C-MAN Sensor Locations
	Double waterTemp; // Sea surface temperature (Celsius). For buoys the depth is referenced to the hull's waterline. For fixed platforms it varies with tide, but is referenced to, or near Mean Lower Low Water (MLLW).
	Double dewPt; // Dewpoint temperature taken at the same height as the air temperature measurement.
	Double visibility; // Station visibility (nautical miles). Note that buoy stations are limited to reports from 0 to 1.6 nmi.
	Double tide; // The water level in feet above or below Mean Lower Low Water (MLLW).

	Double getTimeMs() {
		return -1.0;
	}
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append(id + ","); 
		b.append(lat + ","); 
		b.append(lon + ",");
		b.append(windDir + ",");
		b.append(windSpeed + ",");
		b.append(windGust + ",");
		b.append(pressure + ",");
		b.append(airTemp + ",");
		b.append(waterTemp + ",");
		b.append(dewPt);
		
		return b.toString();
	}
}
