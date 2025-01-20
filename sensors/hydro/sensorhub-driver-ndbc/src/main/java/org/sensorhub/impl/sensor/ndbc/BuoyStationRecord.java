package org.sensorhub.impl.sensor.ndbc;

/**
 * <station id="21413" lat="30.514" lon="152.123" elev="0" name="SOUTHEAST TOKYO - 700NM ESE of Tokyo, JP" 
 * owner="NDBC" pgm="Tsunami" type="dart" met="n" currents="n" waterquality="n" dart="n"/>


 * @author tcook
 *
 */

public class BuoyStationRecord 
{
	String id; // or int
	Double lat;
	Double lon;
	Double elevation;
	String name;
	String owner;
	String pgm;
	String type;
	String met;
	String currents;
	String waterQuality;
	String dart;
	
	public BuoyStationRecord() {
		
	}
	
	@Override
	public String toString() {
		return id + "," + lat + "," + lon + "," + elevation + "," + name; 
	}
}