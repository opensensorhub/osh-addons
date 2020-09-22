/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2020 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.ndbc;

public class BuoyRecord {

	BuoyParam recordType;
	// Common to all record types
	String stationId;
	String sensorId;
	Double lat;
	Double lon;
	Double depth;
	String timeStr;
	long timeMs;

	public BuoyRecord(BuoyParam recordType) {
		this.recordType = recordType;
		currents = new Currents();
		waves = new Waves();
		winds = new Winds();
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
	Currents currents;
	Waves waves;
	Winds winds;

	class Currents {
		Integer bin;
		Integer direction_of_sea_water_velocity;
		Double sea_water_speed;
		Double upward_sea_water_velocity;
		Double error_velocity;
		Double platform_orientation;
		Double platform_pitch_angle;
		Double platform_roll_angle;
		Double sea_water_temperature;
		Integer pct_good_3_beam;
		Integer pct_good_4_beam;
		Integer pct_rejected;
		Integer pct_bad;
		Integer echo_intensity_beam1;
		Integer echo_intensity_beam2;
		Integer echo_intensity_beam3;
		Integer echo_intensity_beam4;
		Integer correlation_magnitude_beam1;
		Integer correlation_magnitude_beam2;
		Integer correlation_magnitude_beam3;
		Integer correlation_magnitude_beam4;
		Integer quality_flags;
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
