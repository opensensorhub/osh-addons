package org.sensorhub.impl.ndbc;

public class BuoyRecord {
	
	BuoyEnums.ObsParam recordType;
	// Common to all record types
	String stationId;
	String sensorId;
	Double lat;
	Double lon;
	Double depth;
	String timeStr;
	long timeMs;
	
	public BuoyRecord(BuoyEnums.ObsParam recordType) {
		this.recordType = recordType;
	}
	
	// Scalars
	Double airPressure;
	Double airTemperature;
	Double conductivity;
	Double salinity;
	Double waterTemperature;
	Double seaFloorDepth;
	Integer depthAveragingInterval;
	
	//  Collections
	class Currents {
		  Integer bin;    // bin
		  Integer waterDirection;   // direction of sea water velocity (deg)
		  Double waterSpeed;     // sea water speed (cm/s)
		  Double upwardWaterVelocity; // upward sea water velocity (cm/s)
		  Double errorVelocity;       // error velocity (cm/s)
		  Double orientation;  // platform orientation (deg)
		  Double pitch;  // platform pitch angle (deg)
		  Double roll;   // platform roll angle (deg)
		  Double waterTemperature;  // sea water temperature (degC)
		  
		  // TODO- populate the rest of these 
//		  percent good 3 beam (%)     // percent good 3 beam (%)
//		  percent good 4 beam (%)     // percent good 4 beam (%)
//		  percent rejected (%)        // percent rejected (%)
//		  percent bad (%)             // percent bad (%)
//		  echo intensity beam 1 (count// echo intensity beam 1 (count)
//		  echo intensity beam 2 (count// echo intensity beam 2 (count)
//		  echo intensity beam 3 (count// echo intensity beam 3 (count)
//		  echo intensity beam 4 (count// echo intensity beam 4 (count)
//		  correlation magnitude beam 1// correlation magnitude beam 1 (count)
//		  correlation magnitude beam 2// correlation magnitude beam 2 (count)
//		  correlation magnitude beam 3// correlation magnitude beam 3 (count)
//		  correlation magnitude beam 4// correlation magnitude beam 4 (count)
//		  quality flags (au)          // quality flags (au)
	}
	
	class Waves {
		Double sea_surface_wave_significant_height; //(m)	
		Double sea_surface_wave_peak_period; //(s)	
		Double sea_surface_wave_mean_period; //(s)	
		Double sea_surface_swell_wave_significant_height; // (m)	
		Double sea_surface_swell_wave_period; //(s)	
		Double sea_surface_wind_wave_significant_height; //(m)	
		Double sea_surface_wind_wave_period;  // (s)	
		Double sea_water_temperature; //(c)	
		Double sea_surface_wave_to_direction; //(degree)	
		Double sea_surface_swell_wave_to_direction; //(degree)	
		Double sea_surface_wind_wave_to_direction; //(degree)	
		Integer number_of_frequencies; //(count)	
		Double [] center_frequencies; //(Hz)	
		Double [] bandwidths; //(Hz)	
		Double [] spectral_energy; //(m**2/Hz)	
		Double [] mean_wave_direction; //(degree)	
		Double [] principal_wave_direction; //(degree)	
		Double [] polar_coordinate_r1; //(1)	
		Double [] polar_coordinate_r2; //(1)	
		String calculation_method;	
		Integer sampling_rate; //(Hz)
		
	}
	
	class Winds {
		Integer wind_from_direction; //(degree)
		Integer wind_speed; //(m/s)
		Integer wind_speed_of_gust; //(m/s)
		Double upward_air_velocity; //(m/s)
	}
}
