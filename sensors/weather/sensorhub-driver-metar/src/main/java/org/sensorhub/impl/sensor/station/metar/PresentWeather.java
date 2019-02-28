package org.sensorhub.impl.sensor.station.metar;

import java.util.ArrayList;

public class PresentWeather {
	Intensity intensity = Intensity.MODERATE;
	public boolean inViciinty = false;
	public Description description;
	public ArrayList<Precipitation> precipitations = new ArrayList<>();;
	public Obscuration obscuration;
	public Other other;
	
	enum Intensity { 
		LIGHT("Light"),
		MODERATE("Moderate"), 
		HEAVY("Heavy");
		
		String description;

		private Intensity (String desc) {
			this.description = desc;
		}
		
		public String getDescription() {
			return description;
		}
	} 

	
	enum Description {
		MI("Shallow"),
		BL("Blowing"),
		PR("Partial"),
		SH("Showers"),
		BC("Patches"),
		TS("Thunderstorm"),
		DR("Drifting"),
		FZ("Freezing");
		
		String description;

		private Description (String desc) {
			this.description = desc;
		}
		
		public String getDescription() {
			return description;
		}
	}
	
	enum Precipitation {
		DZ("Drizzle"),
		IC("Ice Crystals"),
		UP("Unknown Precipitation"),
		RA("Rain"),
		PL("Ice Pellets"),
		SN("Snow"),
		GR("Hail"),
		SG("Snow Grains"),
		GS("Small Hail/Snow Pellets");
		
		String description;

		private Precipitation (String desc) {
			this.description = desc;
		}
		
		public String getDescription() {
			return description;
		}
	}
	
	enum Obscuration {
		BR("Mist"),
		DU("Widespread Dust"),
		FG("Fog"),
		SA("Sand"),
		FU("Smoke"),
		HZ("Haze"),
		VA("Volcanic Ash"),
		PY("Spray");
		
		String description;

		private Obscuration (String desc) {
			this.description = desc;
		}
		
		public String getDescription() {
			return description;
		}
	}
	
	enum Other {
		PO("Well Developed Dust/Sand Whirls"),
		DS("Duststorm"),
		SQ("Squalls"),
		FC("Funnel Cloud(s)"),
		SS("Sandstorm");
		
		String description;

		private Other (String desc) {
			this.description = desc;
		}
		
		public String getDescription() {
			return description;
		}
	}
	
	public static PresentWeather parsePresentWeather(String s) {
		PresentWeather pw = new PresentWeather();
		
		// Intensity
		if(s.startsWith("+")) {
			pw.intensity = Intensity.HEAVY;
			s = s.substring(1);
		} else if(s.startsWith("-")) {
			pw.intensity = Intensity.LIGHT;
			s = s.substring(1);
		}
		if(s.startsWith("VC")) {
			pw.inViciinty = true;
			s = s.substring(2);
		}
		
		// Description
		for(Description d: Description.values()) {
			if(s.startsWith(d.name())) {
				pw.description = d;
				s = s.substring(2);
				break;
			}
		}
		
		// Precipitation
		for(Precipitation p: Precipitation.values()) {
			if(s.contains(p.name())) {
				pw.precipitations.add(p);
			}
		}
		
		// Observation
		for(Obscuration o: Obscuration.values()) {
			if(s.startsWith(o.name())) {
				pw.obscuration = o;
				s = s.substring(2);
				break;
			}
		}

		for(Other o: Other.values()) {
			if(s.startsWith(o.name())) {
				pw.other = o;
				break;
			}
		}
		return pw;
		
	}

	public static boolean isPresentWeather(String s) {
		if(s.startsWith("+") || s.startsWith("-"))
			return true;
		if(s.startsWith("VC"))
			return true;

		for(Description d: Description.values()) {
			if(s.startsWith(d.name())) 
				return true;
		}
		
		// Precipitation
		for(Precipitation p: Precipitation.values()) {
			if(s.startsWith(p.name())) 
				return true;
		}
		
		// Observation
		for(Obscuration o: Obscuration.values()) {
			if(s.startsWith(o.name())) 
				return true;
		}

		for(Other o: Other.values()) {
			if(s.startsWith(o.name())) 
				return true;
		}
		
		return false;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append(intensity.description + " ");
		if(description != null)
			sb.append(description.description + " ");
		for(Precipitation p: precipitations)
			sb.append(p.description + " ");
		if(obscuration != null)
			sb.append(obscuration.description + " ");
		if(other != null)
			sb.append(other.description + " ");
		if(inViciinty)
			sb.append("in vicinity");
		return sb.toString().trim();
	}
	
	public static void main(String[] args) throws Exception {
		String s = "+SNRA";
		PresentWeather pw = parsePresentWeather(s);
		
		System.err.println(pw);
	}
}
