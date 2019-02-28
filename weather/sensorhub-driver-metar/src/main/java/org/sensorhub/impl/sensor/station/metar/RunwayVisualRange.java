package org.sensorhub.impl.sensor.station.metar;


import java.util.ArrayList;

import org.sensorhub.impl.sensor.station.metar.MetarConstants.Modifier;

public class RunwayVisualRange {
	private String rawRvr;  // mainly using this for testingg
	String runwayId;

	public String units = "feet"; // feet or meters only

	public Range range;
	public Range lowerRange;
	public Range upperRange;
	
	public class Range {
		int value; 
		private ArrayList<Modifier> modifiers = new ArrayList<>();
		
		public ArrayList<Modifier> getModifiers() {
			return modifiers;
		}

		public void addModifier(Modifier m) {
			modifiers.add(m);
		}
		
		public String toString()  {
			StringBuilder b = new StringBuilder();
			for(Modifier m: getModifiers()) {
				b.append(m.toString() + " ");
			}
			b.append(value + " ");
			return b.toString();
		}
	}
	
	public RunwayVisualRange(String rvr) {
		this.rawRvr = rvr;
	}

	//  TODO determine how to report this 
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("RunwayID: " + runwayId + ",");
		if(range != null) {
			b.append("Range: " );
			b.append(range.toString());
			b.append(units + ",");
		}
		if(lowerRange != null) {
			b.append("Lower Range: " );
			b.append(lowerRange.toString());
			b.append(units + ",");
		}
		if(upperRange != null) {
			b.append("Upper Range: " );
			b.append(upperRange.toString());
			b.append(units + ",");
		}

		
		b.append(";");
		return b.toString();
	}
	
	public static void main(String[] args) throws Exception {
		RunwayVisualRange rvr = MetarParserNew.parseRunwayVis("R01/3500VP6000FT");
		System.err.println(rvr);
	}
}

