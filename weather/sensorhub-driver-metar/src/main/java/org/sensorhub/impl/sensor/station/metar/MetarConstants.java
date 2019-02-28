/*
jWeather(TM) is a Java library for parsing raw weather data
Copyright (C) 2004 David Castro

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

For more information, please email arimus@users.sourceforge.net
 */
package org.sensorhub.impl.sensor.station.metar;

/**
 * Simple container for METAR weather token constants.  
 * 
 * 	https://github.com/arimus/jweather
 * 
 * @author dennis@bullamanka.com
 * @author Tony Cook 
 * 
 */
public interface MetarConstants {
	
	public enum Modifier {
		GREATER_THAN("P", "greater than"),
		LESS_THAN("M", "less than"),
		INCREASING("U", "increasing"),
		DECREASING("D", "decreasing"),
		NO_CHANGE("N", "no change"),
		UNKNOWN("", "unknown mnodifier");
		
		String code;
		String description;
		
		private Modifier(String code, String desc) {
			this.code = code;
			this.description = desc;
		}
		
		public String toString() {
			return description;
		}
	}
	
	/** Metar string value for fully automated report ('AUTO') */
	public static final String AUTOMATED = "AUTO";
	/** Metar string value for corrected report ('COR') */
	public static final String CORRECTED = "COR";
	/** Metar string value for Vertical Visibility ('VV') */
	public static final String VERTICAL_VISIBILITY = "VV";
	/** Metar string value for Sky Clear ('SKC') */
	public static final String SKY_CLEAR = "SKC";
	/** Metar string value for Clear ('CLR') */
	public static final String CLEAR = "CLR";
	/** Metar string value for Few ('FEW') */
	public static final String FEW = "FEW";
	/** Metar string value for Scattered ('SCT') */
	public static final String SCATTERED = "SCT";
	/** Metar string value for Broken ('BKN') */
	public static final String BROKEN = "BKN";
	/** Metar string value for Overcast ('OVC') */
	public static final String OVERCAST = "OVC";
	/** Metar string value for Cumulonimbus ('CB') */
	public static final String CUMULONIMBUS = "CB";
	/** Metar string value for Towering Cumulus ('TCU') */
	public static final String TOWERING_CUMULUS = "TCU";
	/** Metar string value for Heavy ('+') */
	public static final String HEAVY = "+";
	/** Metar string value for Light ('-') */
	public static final String LIGHT = "-";
	/** Metar string value for Shallow ('MI') */
	public static final String SHALLOW = "MI";
	/** Metar string value for Partial ('PR') */
	public static final String PARTIAL = "PR";
	/** Metar string value for Patches ('BC') */
	public static final String PATCHES = "BC";
	/** Metar string value for LowDrifting ('DR') */
	public static final String LOW_DRIFTING = "DR";
	/** Metar string value for Blowing ('BL') */
	public static final String BLOWING = "BL";
	/** Metar string value for Showers ('SH') */
	public static final String SHOWERS = "SH";
	public static final String SHOWERS_VICINITY = "VCSH";  // TC, added 4/8/16
	/** Metar string value for Thunderstorms ('TS') */
	public static final String THUNDERSTORMS = "TS";
	public static final String THUNDERSTORMS_VICINITY = "VCTS";  // TC, added 4/8/16
	/** Metar string value for Freezing ('FZ') */
	public static final String FREEZING = "FZ";
	/** Metar string value for Drizzle ('DZ') */
	public static final String DRIZZLE = "DZ";
	/** Metar string value for Rain ('RA') */
	public static final String RAIN = "RA";
	/** Metar string value for Snow ('SN') */
	public static final String SNOW = "SN";
	/** Metar string value for Snow Grains ('SG') */
	public static final String SNOW_GRAINS = "SG";
	/** Metar string value for Ice Crystals ('IC') */
	public static final String ICE_CRYSTALS = "IC";
	/** Metar string value for Ice Pellets ('PL') */
	public static final String ICE_PELLETS = "PL";
	/** Metar string value for Hail ('GR') */
	public static final String HAIL = "GR";
	/** Metar string value for Small Hail ('GS') */
	public static final String SMALL_HAIL = "GS";
	/** Metar string value for Unknown Precip ('UP') */
	public static final String UNKNOWN_PRECIPITATION = "UP";
	/** Metar string value for Mist ('BR') */
	public static final String MIST = "BR";
	/** Metar string value for Fog ('FG') */
	public static final String FOG = "FG";
	/** Metar string value for Smoke ('FU') */
	public static final String SMOKE = "FU";
	/** Metar string value for Volcanic Ash ('VA') */
	public static final String VOLCANIC_ASH = "VA";
	/** Metar string value for Widespread Dust ('DU') */
	public static final String WIDESPREAD_DUST = "DU";
	/** Metar string value for Sand ('SA') */
	public static final String SAND = "SA";
	/** Metar string value for Haze ('HZ') */
	public static final String HAZE = "HZ";
	/** Metar string value for Spray ('PY') */
	public static final String SPRAY = "PY";
	/** Metar string value for Dust Sand Whirls ('PO') */
	public static final String DUST_SAND_WHIRLS = "PO";
	/** Metar string value for Squalls ('SQ') */
	public static final String SQUALLS = "SQ";
	/** Metar string value for Funnel Cloud ('FC') */
	public static final String FUNNEL_CLOUD = "FC";
	/** Metar string value for Sand Storm ('SS') */
	public static final String SAND_STORM = "SS";
	/** Metar string value for Dust Storm ('DS') */
	public static final String DUST_STORM = "DS";
	/** Metar string value for Becoming ('BECMG') */
	public static final String BECOMING = "BECMG";
	/** Metar string value for Remarks ('RMK') */
	public static final String REMARKS = "RMK";
	/** Metar string value for RVR reportable */
	public static final char REPORTABLE_BELOW = 'P';
	/** Metar string value for RVR reportable */
	public static final char REPORTABLE_ABOVE = 'M';
	/** Metar string value for: 
	   Ceiling and Visibility are OK; specifically, 
	   		(1) there are no clouds below 5000 feet above aerodrome level (AAL) or minimum sector altitude (whichever is higher) and no cumulonimbus or towering cumulus; 
	   		(2) visibility is at least 10 kilometres (6 statute miles) or more; and 
	   		(3) no current or forecast significant weather such as precipitation, thunderstorms, shallow fog or low drifting snow.
	*/
	public static final String CAVOK = "CAVOK";
	/** Metar string value for Clouds and Visibility Okay ('CAVOK') */
	public static final String MAX_VISIBILITY = "9999";
	/** Metar string value for No Significant Change ('NOSIG') */
	public static final String NO_SIGNIFICANT_CHANGE = "NOSIG";
	/** Metar string value for No Significant Clouds ('NSC') */
	public static final String NO_SIGNIFICANT_CLOUDS = "NSC";
	/** Metar decoded string value for Vertical Visibility ('VV') */
	public static final String DECODED_VERTICAL_VISIBILITY = "Vertical Visibility";
	/** Metar decoded string value for Sky Clear ('SKC') */
	public static final String DECODED_SKY_CLEAR = "Sky Clear";
	/** Metar decoded string value for Clear ('CLR') */
	public static final String DECODED_CLEAR = "Clear";
	/** Metar decoded string value for Few ('FEW') */
	public static final String DECODED_FEW = "Few";
	/** Metar decoded string value for Scattered ('SCT') */
	public static final String DECODED_SCATTERED = "Scattered";
	/** Metar decoded string value for Broken ('BKN') */
	public static final String DECODED_BROKEN = "Broken";
	/** Metar decoded string value for Overcase ('OVC') */
	public static final String DECODED_OVERCAST = "Overcast";
	/** Metar decoded string value for Cumulonimbus ('CB') */
	public static final String DECODED_CUMULONIMBUS = "Cumulonimbus";
	/** Metar decoded string value for Towering Cumulonimbus ('TCU') */
	public static final String DECODED_TOWERING_CUMULONIMBUS = "Tower Cumulonimbus";
	/** Metar decoded string value for Severe */
	public static final String DECODED_SEVERE = "Severe";
	/** Metar decoded string value for Heavy ('+') */
	public static final String DECODED_HEAVY = "Heavy";
	/** Metar decoded string value for Light ('-') */
	public static final String DECODED_LIGHT = "Light";
	/** Metar decoded string value for Slight */
	public static final String DECODED_SLIGHT = "Slight";
	/** Metar decoded string value for Light ('-') */
	public static final String DECODED_MODERATE = "Moderate";	
	/** Metar decoded string value for Shallow ('MI') */
	public static final String DECODED_SHALLOW = "Shallow";	
	/** Metar decoded string value for Partial ('PR') */
	public static final String DECODED_PARTIAL = "Partial";
	/** Metar decoded string value for Patches ('BC') */
	public static final String DECODED_PATCHES = "Patches";
	/** Metar decoded string value for LowDrifting ('DR') */
	public static final String DECODED_LOW_DRIFTING = "Low Drifting";
	/** Metar decoded string value for Blowing ('BL') */
	public static final String DECODED_BLOWING = "Blowing";
	/** Metar decoded string value for Showers ('SH') */
	public static final String DECODED_SHOWERS = "Showers";
	/** Metar decoded string value for Thunderstorms ('TS') */
	public static final String DECODED_THUNDERSTORMS = "Thunderstorms";
	/** Metar decoded string value for Freezing ('FZ') */
	public static final String DECODED_FREEZING = "Freezing";
	/** Metar decoded string value for Drizzle ('DZ') */
	public static final String DECODED_DRIZZLE = "Drizzle";
	/** Metar decoded string value for Rain ('RA') */
	public static final String DECODED_RAIN = "Rain";
	/** Metar decoded string value for Snow ('SN') */
	public static final String DECODED_SNOW = "Snow";
	/** Metar decoded string value for Snow Grains ('SG') */
	public static final String DECODED_SNOW_GRAINS = "Snow Grains";
	/** Metar decoded string value for Ice Crystals ('IC') */
	public static final String DECODED_ICE_CRYSTALS = "Ice Crystals";
	/** Metar decoded string value for Ice Pellets ('PL') */
	public static final String DECODED_ICE_PELLETS = "Ice Pellets";
	/** Metar decoded string value for Hail ('GR') */
	public static final String DECODED_HAIL = "Hail";
	/** Metar decoded string value for Small Hail ('GS') */
	public static final String DECODED_SMALL_HAIL = "Small Hail";
	/** Metar decoded string value for Unknown Precip ('UP') */
	public static final String DECODED_UNKNOWN_PRECIP = "Unknown Precip";
	/** Metar decoded string value for Mist ('BR') */
	public static final String DECODED_MIST = "Mist";
	/** Metar decoded string value for Fog ('FG') */
	public static final String DECODED_FOG = "Fog";
	/** Metar decoded string value for Smoke ('FU') */
	public static final String DECODED_SMOKE = "Smoke";
	/** Metar decoded string value for Volcanic Ash ('VA') */
	public static final String DECODED_VOLCANIC_ASH = "Volcanic Ash";
	/** Metar decoded string value for Widespread Dust ('DU') */
	public static final String DECODED_WIDESPREAD_DUST = "Widespread Dust";
	/** Metar decoded string value for Sand ('SA') */
	public static final String DECODED_SAND = "Sand";
	/** Metar decoded string value for Haze ('HZ') */
	public static final String DECODED_HAZE = "Haze";
	/** Metar decoded string value for Spray ('PY') */
	public static final String DECODED_SPRAY = "Spray";
	/** Metar decoded string value for Dust Sand Whirls ('PO') */
	public static final String DECODED_DUST_SAND_WHIRLS = "Dust Sand Whirls";
	/** Metar decoded string value for Squalls ('SQ') */
	public static final String DECODED_SQUALLS = "Squalls";
	/** Metar decoded string value for Funnel Cloud ('FC') */
	public static final String DECODED_FUNNEL_CLOUD = "Funnel Cloud";
	/** Metar decoded string value for Sand Storm ('SS') */
	public static final String DECODED_SAND_STORM = "Sand Storm";
	/** Metar decoded string value for Dust Storm ('DS') */
	public static final String DECODED_DUST_STORM = "Dust Storm";
	/** Metar decoded string value for RVR reportable */
	public static final String DECODED_REPORTABLE_BELOW = "less than";
	/** Metar decoded string value for RVR reportable */
	public static final String DECODED_REPORTABLE_ABOVE = "greater than";
	/** Metar decoded string value for Clouds and Visibility Okay ('CAVOK') */
	public static final String DECODED_CAVOK = "Clouds and Visibility Okay";
	/** Metar decoded string value for No Significant Change ('NOSIG') */
	public static final String DECODED_NO_SIGNIFICANT_CHANGE = "No significant change";
	/** Metar decoded string value for No Significant Clouds ('NSC') */
	public static final String DECODED_NO_SIGNIFICANT_CLOUDS = "No significant clouds";
}
