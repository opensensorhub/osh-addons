package org.sensorhub.impl.sensor.intellisense;

import java.time.Instant;

public class FloodRecord {
	
	FloodDouble [] depth1   ;
	FloodDouble [] NAVD88O  ;
	FloodDouble [] NAVD88D1 ;
	FloodInt [] dropSDI     ;
	FloodDouble [] soilSDI  ;
	FloodDouble [] lat      ;
	FloodDouble [] lon      ;
	FloodString [] elev     ;  // handle case of n/a
	FloodInt [] samp        ;
	FloodInt [] mode        ;
	FloodDouble [] oPressure;
	FloodDouble [] airTemp  ;
	FloodDouble [] h2oTemp  ;
	FloodDouble [] baro     ;
	FloodInt [] rssi        ;
	FloodString [] time     ;
	FloodString [] hex      ;
	FloodString [] IMEI     ;  // NOT the deviceID
	FloodDouble [] battery  ;
	FloodInt [] ffi1        ;
	
	long timeMs;
	String deviceId; // Need this
	
	class FloodDouble {
		public long ts;
		public Double value;
		@Override
		public String toString() {
			return value + "," + ts + "\n";
		}
	}
	
	class FloodInt {
		public long ts;
		public Integer value;
		@Override
		public String toString() {
			return value + "," + ts + "\n";
		}
	}
	
	class FloodString {
		public long ts;
		public String value;
		
		@Override
		public String toString() {
			return value + "," + ts + "\n";
		}
	}
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		long time = ffi1[0].ts;
		Instant date =  Instant.ofEpochMilli(time);
		b.append("deviceId: " + deviceId + "\n");
		b.append("timeUtc: " + timeMs + "\n");
		b.append("lat: " + lat[0]);
		b.append("lon: " + lon[0]);
		b.append("airTemp: " + airTemp[0]);
		b.append("h2oTemp: " + h2oTemp[0]);
		b.append("depth: " + depth1[0]);
		b.append("ffi1: " + ffi1[0]);
		return b.toString();
	}
}
