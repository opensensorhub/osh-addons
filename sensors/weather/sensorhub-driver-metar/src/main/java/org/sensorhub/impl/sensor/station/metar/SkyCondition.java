package org.sensorhub.impl.sensor.station.metar;

public class SkyCondition {
	enum Coverage {
		SKC("Clear"),  // Manual 0/8
		CLR("Clear"),  // Auto 0/8
		FEW("Few"),  //  1/8 - 2/8
		SCT("Scattered"), // 3/8 - 4/8
		BKN("Broken"), // 5/8- 7/8 
		OVC("Overcast"),
		VV("Vertical Visibility"),  // 8/8
		NCD("No Cloud Detected");
		
		String description;
		
		Coverage(String desc) {
			this.description = desc;
		}
		
		public String getDescription() {
			return description;
		}
	}
	
	Coverage coverage;
	Integer visibilityFeet;
	boolean isTcu = false;
	boolean isCb = false;
	
	public static SkyCondition parseSkyCondition(String s) {
		SkyCondition sc = new SkyCondition();
		for(Coverage c: Coverage.values()) {
			if(s.startsWith(c.name())) {
				sc.coverage = c;
				break;
			}
		}
		if(sc.coverage == null) {
			System.err.println("SkyCondistions.parseCondition. Unrecognized format: " + s);
			return null; // ?? Should aways call isSkyCondition first
		}
		if(sc.coverage == Coverage.SKC || sc.coverage == Coverage.CLR) {
			return sc;  // no Vis value
		}
		if(sc.coverage == Coverage.VV) {
			s = s.substring(2);
		} else {
			s = s.substring(3);
		}
		if(s.equals("///")) {    // undetermined VV 
			return sc;
		}
		if(s.length() == 0) { // or NoCloudDetctd
			// set visFt to maxvis???
			return sc;
		}
		
		// Vis ft and make sure we check for TCU/CB 
		String vft = s.substring(0, 3);
		s = s.substring(3);
		sc.visibilityFeet = Integer.parseInt(vft) * 100;
		if(s.length() == 0)
			return sc;
		if(s.equals("TCU"))
			sc.isTcu = true;
		else if (s.equals("CB"))
			sc.isCb = true;
		return sc;
	}
	
	public static boolean isSkyCondition(String s) {
		for(Coverage c: Coverage.values()) {
			if(s.startsWith(c.name())) {
				return true;
			}
		}
		
		return false;
	}
	
	public String toString() {
		StringBuilder b = new StringBuilder();
		
		b.append(coverage.getDescription());
		if(coverage != Coverage.CLR && coverage != Coverage.SKC)
			b.append( "- Visibility (ft): " + (  visibilityFeet != null ? visibilityFeet : " Uknown"));
		if(isTcu)  b.append(" isTCU");
		if(isCb)  b.append(" isCB");
		
		return b.toString();
	}
	
	public static void main(String[] args) throws Exception {
		String s = "CLR";
		boolean isSc = isSkyCondition(s);
		SkyCondition sc = parseSkyCondition(s);
		
		System.err.println(isSc + ": " + sc);
	}
}
