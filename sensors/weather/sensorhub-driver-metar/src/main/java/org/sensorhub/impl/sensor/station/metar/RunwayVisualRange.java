package org.sensorhub.impl.sensor.station.metar;


import java.util.ArrayList;

import org.sensorhub.impl.sensor.station.metar.MetarConstants.Modifier;

public class RunwayVisualRange {
	String runwayId;

	public String units = "feet"; // feet or meters only

	public Range range;
	public Range lowerRange;
	public Range upperRange;
	public Character trend; // D for Down, N for No change, U for Up 
	public boolean isMissing = false;
	
	public class Range {
		int value; 
		private ArrayList<Modifier> modifiers = new ArrayList<>();
		
		public ArrayList<Modifier> getModifiers() {
			return modifiers;
		}

		public void addModifier(Modifier m) {
			modifiers.add(m);
		}
	}
	
	public RunwayVisualRange(String id) {
		this.runwayId = id;
	}

	//  TODO determine how to report this 
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("RunwayID: " + runwayId);
		return b.toString();
	}
}

