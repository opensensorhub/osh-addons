package org.sensorhub.impl.ndbc;

import java.util.Set;
import java.util.StringJoiner;

import org.sensorhub.api.data.IMultiSourceDataInterface;
import org.sensorhub.api.persistence.IRecordStoreInfo;
import org.sensorhub.impl.ndbc.BuoyEnums.ObsParam;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.DataType;

public class RecordStore  implements IRecordStoreInfo {
	
    DataRecord dataStruct;
    DataEncoding encoding;
    
    public RecordStore(String name, Set<ObsParam> parameters)
    {
        SWEHelper helper = new SWEHelper();
        GeoPosHelper geo = new GeoPosHelper();
        
        // TODO sort params by code?        
        
        // build record structure with requested parameters
        dataStruct = helper.newDataRecord();
        dataStruct.setName(name);
        
        dataStruct.addField("time", helper.newTimeStampIsoUTC());
        dataStruct.addField("station", helper.newText("http://sensorml.com/ont/swe/property/station_id", "Station ID", null));
        dataStruct.addComponent("location", geo.newLocationVectorLatLon(SWEConstants.DEF_SENSOR_LOC));
        dataStruct.getFieldList().getProperty(1).setRole(IMultiSourceDataInterface.ENTITY_ID_URI);
        
        for (ObsParam param: parameters)
        {
        	String paramName = param.name().toLowerCase();
        	
            DataComponent c;
            switch (paramName) {
            
	        	case "air_pressure_at_sea_level":
	        		dataStruct.addComponent("depth", helper.newQuantity("http://sensorml.com/ont/swe/property/buoy_depth", "Buoy Depth", null, "m", DataType.FLOAT));
	        		c = helper.newQuantity(SWEHelper.getPropertyUri("air_pressure_at_sea_level"),
	        				"Air Pressure at Sea Level",
	        				"NDBC Buoy Station Air Pressure at Sea Level",
	        				"hPa",
	        				DataType.FLOAT);
	        		dataStruct.addComponent("air_pressure_at_sea_level", c);
	        		break;
        		
            	case "air_temperature":
            		dataStruct.addComponent("depth", helper.newQuantity("http://sensorml.com/ont/swe/property/buoy_depth", "Buoy Depth", null, "m", DataType.FLOAT));
            		c = helper.newQuantity(SWEHelper.getPropertyUri("air_temperature"),
            				"Air Temperature",
            				"NDBC Buoy Station Air Temperature",
            				"degC",
            				DataType.FLOAT);
            		dataStruct.addComponent("air_temperature", c);
            		break;
            		
            	case "currents":
            		dataStruct.addComponent("bin", helper.newQuantity("http://sensorml.com/ont/swe/property/bin", "Bin", null, "count", DataType.FLOAT));
            		dataStruct.addComponent("depth", helper.newQuantity("http://sensorml.com/ont/swe/property/buoy_depth", "Buoy Depth", null, "m", DataType.FLOAT));
            		c = helper.newQuantity(SWEHelper.getPropertyUri("direction_of_sea_water_velocity"),
            				"Direction of Sea Water Velocity",
            				"NDBC Buoy Station Direction of Sea Water Velocity",
            				"deg",
            				DataType.FLOAT);
            		dataStruct.addComponent("direction_of_sea_water_velocity", c);
            		
            		c = helper.newQuantity(SWEHelper.getPropertyUri("sea_water_speed"),
            				"Sea Water Speed",
            				"NDBC Buoy Station Sea Water Speed",
            				"cm/s",
            				DataType.FLOAT);
            		dataStruct.addComponent("sea_water_speed", c);
            		
            		c = helper.newQuantity(SWEHelper.getPropertyUri("upward_sea_water_velocity"),
            				"Upward Sea Water Velocity",
            				"NDBC Buoy Station Upward Sea Water Velocity",
            				"cm/s",
            				DataType.FLOAT);
            		dataStruct.addComponent("upward_sea_water_velocity", c);
            		
            		c = helper.newQuantity(SWEHelper.getPropertyUri("error_velocity"),
            				"Error Velocity",
            				"NDBC Buoy Station Error Velocity",
            				"cm/s",
            				DataType.FLOAT);
            		dataStruct.addComponent("error_velocity", c);
            		
            		c = helper.newQuantity(SWEHelper.getPropertyUri("platform_orientation"),
            				"Platform Orientation",
            				"NDBC Buoy Station Platform Orientation",
            				"deg",
            				DataType.FLOAT);
            		dataStruct.addComponent("platform_orientation", c);
            		
            		c = helper.newQuantity(SWEHelper.getPropertyUri("platform_pitch_angle"),
            				"Platform Pitch Angle",
            				"NDBC Buoy Station Platform Pitch Angle",
            				"deg",
            				DataType.FLOAT);
            		dataStruct.addComponent("platform_pitch_angle", c);
            		
            		c = helper.newQuantity(SWEHelper.getPropertyUri("platform_roll_angle"),
            				"Platform Roll Angle",
            				"NDBC Buoy Station Platform Roll Angle",
            				"deg",
            				DataType.FLOAT);
            		dataStruct.addComponent("platform_roll_angle", c);
            		
            		c = helper.newQuantity(SWEHelper.getPropertyUri("currents_sea_water_temperature"),
            				"Sea Water Temperature",
            				"NDBC Buoy Station Sea Water Temperature",
            				"degC",
            				DataType.FLOAT);
            		dataStruct.addComponent("currents_sea_water_temperature", c);
            		
            		c = helper.newQuantity(SWEHelper.getPropertyUri("pct_good_3_beam"),
            				"Percent Good 3 Beam",
            				"NDBC Buoy Station Percent Good 3 Beam",
            				"%",
            				DataType.FLOAT);
            		dataStruct.addComponent("pct_good_3_beam", c);
            		
            		c = helper.newQuantity(SWEHelper.getPropertyUri("pct_good_4_beam"),
            				"Percent Good 4 Beam",
            				"NDBC Buoy Station Percent Good 4 Beam",
            				"%",
            				DataType.FLOAT);
            		dataStruct.addComponent("pct_good_4_beam", c);
            		
            		c = helper.newQuantity(SWEHelper.getPropertyUri("pct_rejected"),
            				"Percent Rejected",
            				"NDBC Buoy Station Percent Rejected",
            				"%",
            				DataType.FLOAT);
            		dataStruct.addComponent("pct_rejected", c);
            		
            		c = helper.newQuantity(SWEHelper.getPropertyUri("pct_bad"),
            				"Percent Bad",
            				"NDBC Buoy Station Percent Bad",
            				"%",
            				DataType.FLOAT);
            		dataStruct.addComponent("pct_bad", c);
            		
            		c = helper.newQuantity(SWEHelper.getPropertyUri("echo_intensity_beam1"),
            				"Echo Intensity Beam 1",
            				"NDBC Buoy Station Echo Intensity Beam 1",
            				"count",
            				DataType.FLOAT);
            		dataStruct.addComponent("echo_intensity_beam1", c);
            		
            		c = helper.newQuantity(SWEHelper.getPropertyUri("echo_intensity_beam2"),
            				"Echo Intensity Beam 2",
            				"NDBC Buoy Station Echo Intensity Beam 2",
            				"count",
            				DataType.FLOAT);
            		dataStruct.addComponent("echo_intensity_beam2", c);
            		
            		c = helper.newQuantity(SWEHelper.getPropertyUri("echo_intensity_beam3"),
            				"Echo Intensity Beam 3",
            				"NDBC Buoy Station Echo Intensity Beam 3",
            				"count",
            				DataType.FLOAT);
            		dataStruct.addComponent("echo_intensity_beam3", c);
            		
            		c = helper.newQuantity(SWEHelper.getPropertyUri("echo_intensity_beam4"),
            				"Echo Intensity Beam 4",
            				"NDBC Buoy Station Echo Intensity Beam 4",
            				"count",
            				DataType.FLOAT);
            		dataStruct.addComponent("echo_intensity_beam4", c);
            		
            		c = helper.newQuantity(SWEHelper.getPropertyUri("correlation_magnitude_beam1"),
            				"Correlation Magnitude Beam 1",
            				"NDBC Buoy Station Correlation Magnitude Beam 1",
            				"count",
            				DataType.FLOAT);
            		dataStruct.addComponent("correlation_magnitude_beam1", c);
            		
            		c = helper.newQuantity(SWEHelper.getPropertyUri("correlation_magnitude_beam2"),
            				"Correlation Magnitude Beam 2",
            				"NDBC Buoy Station Correlation Magnitude Beam 2",
            				"count",
            				DataType.FLOAT);
            		dataStruct.addComponent("correlation_magnitude_beam2", c);
            		
            		c = helper.newQuantity(SWEHelper.getPropertyUri("correlation_magnitude_beam3"),
            				"Correlation Magnitude Beam 3",
            				"NDBC Buoy Station Correlation Magnitude Beam 3",
            				"count",
            				DataType.FLOAT);
            		dataStruct.addComponent("correlation_magnitude_beam3", c);
            		
            		c = helper.newQuantity(SWEHelper.getPropertyUri("correlation_magnitude_beam4"),
            				"Correlation Magnitude Beam 4",
            				"NDBC Buoy Station Correlation Magnitude Beam 4",
            				"count",
            				DataType.FLOAT);
            		dataStruct.addComponent("correlation_magnitude_beam4", c);
            		
            		c = helper.newQuantity(SWEHelper.getPropertyUri("quality_flags"),
            				"Quality Flags",
            				"NDBC Buoy Station Quality Flags",
            				"",
            				DataType.FLOAT);
            		dataStruct.addComponent("quality_flags", c);
            		break;
            		
            	case "sea_floor_depth_below_sea_surface":
            		c = helper.newQuantity(SWEHelper.getPropertyUri("sea_floor_depth_below_sea_surface"),
            				"Sea Floor Depth Below Sea Surface",
            				"NDBC Buoy Station Sea Floor Depth Below Sea Surface",
            				"m",
            				DataType.FLOAT);
            		dataStruct.addComponent("sea_floor_depth_below_sea_surface", c);
            		
            		c = helper.newQuantity(SWEHelper.getPropertyUri("averaging_interval"),
            				"Water Level Averaging Interval",
            				"NDBC Buoy Station Water Level Averaging Interval",
            				"s",
            				DataType.FLOAT);
            		dataStruct.addComponent("averaging_interval", c);
            		break;
            		
            	case "sea_water_electrical_conductivity":
            		dataStruct.addComponent("depth", helper.newQuantity("http://sensorml.com/ont/swe/property/buoy_depth", "Buoy Depth", null, "m", DataType.FLOAT));
            		c = helper.newQuantity(SWEHelper.getPropertyUri("sea_water_electrical_conductivity"),
            				"Sea Water Electrical Conductivity",
            				"NDBC Buoy Station Sea Water Electrical Conductivity",
            				"mS/cm",
            				DataType.FLOAT);
            		dataStruct.addComponent("sea_water_electrical_conductivity", c);
            		break;
            		
            	case "sea_water_salinity":
            		dataStruct.addComponent("depth", helper.newQuantity("http://sensorml.com/ont/swe/property/buoy_depth", "Buoy Depth", null, "m", DataType.FLOAT));
            		c = helper.newQuantity(SWEHelper.getPropertyUri("sea_water_salinity"),
            				"Sea Water Salinity",
            				"NDBC Buoy Station Sea Station Salinity",
            				"psu",
            				DataType.FLOAT);
            		dataStruct.addComponent("sea_water_salinity", c);
            		break;
        		
	        	case "sea_water_temperature":
	        		dataStruct.addComponent("depth", helper.newQuantity("http://sensorml.com/ont/swe/property/buoy_depth", "Buoy Depth", null, "m", DataType.FLOAT));
	        		c = helper.newQuantity(SWEHelper.getPropertyUri("sea_water_temperature"),
	        				"Sea Water Temperature",
	        				"NDBC Buoy Station Sea Water Temperature",
	        				"degC",
	        				DataType.FLOAT);
	        		dataStruct.addComponent("sea_water_temperature", c);
	        		break;
	    		
            	case "waves":
            		c = helper.newQuantity(SWEHelper.getPropertyUri("sea_surface_wave_significant_height"),
            				"Sea Surface Wave Significant Height",
            				"NDBC Buoy Station Sea Surface Wave Significant Height",
            				"m",
            				DataType.FLOAT);
            		dataStruct.addComponent("sea_surface_wave_significant_height", c);
            		
            		c = helper.newQuantity(SWEHelper.getPropertyUri("sea_surface_wave_peak_period"),
            				"Sea Surface Wave Peak Period",
            				"NDBC Buoy Station Sea Surface Wave Peak Period",
            				"s",
            				DataType.FLOAT);
            		dataStruct.addComponent("sea_surface_wave_peak_period", c);
            		
            		c = helper.newQuantity(SWEHelper.getPropertyUri("sea_surface_wave_mean_period"),
            				"Sea Surface Wave Mean Period",
            				"NDBC Buoy Station Sea Surface Wave Mean Period",
            				"s",
            				DataType.FLOAT);
            		dataStruct.addComponent("sea_surface_wave_mean_period", c);
            		
            		c = helper.newQuantity(SWEHelper.getPropertyUri("sea_surface_swell_wave_significant_height"),
            				"Sea Surface Swell Wave Significant Height",
            				"NDBC Buoy Station Sea Surface Swell Wave Significant Height",
            				"m",
            				DataType.FLOAT);
            		dataStruct.addComponent("sea_surface_swell_wave_significant_height", c);
            		
            		c = helper.newQuantity(SWEHelper.getPropertyUri("sea_surface_swell_wave_period"),
            				"Sea Surface Swell Wave Period",
            				"NDBC Buoy Station Sea Surface Swell Wave Period",
            				"s",
            				DataType.FLOAT);
            		dataStruct.addComponent("sea_surface_swell_wave_period", c);
            		
            		c = helper.newQuantity(SWEHelper.getPropertyUri("sea_surface_wind_wave_significant_height"),
            				"Sea Surface Wind Wave Significant Height",
            				"NDBC Buoy Station Sea Surface Wind Wave Significant Height",
            				"m",
            				DataType.FLOAT);
            		dataStruct.addComponent("sea_surface_wind_wave_significant_height", c);
            		
            		c = helper.newQuantity(SWEHelper.getPropertyUri("sea_surface_wind_wave_period"),
            				"Sea Surface Wind Wave Period",
            				"NDBC Buoy Station Sea Surface Wind Wave Period",
            				"s",
            				DataType.FLOAT);
            		dataStruct.addComponent("sea_surface_wind_wave_period", c);
            		
            		c = helper.newQuantity(SWEHelper.getPropertyUri("waves_sea_water_temperature"),
            				"Sea Water Temperature",
            				"NDBC Buoy Station Sea Water Temperature",
            				"degC",
            				DataType.FLOAT);
            		dataStruct.addComponent("waves_sea_water_temperature", c);
            		
            		c = helper.newQuantity(SWEHelper.getPropertyUri("sea_surface_wave_to_direction"),
            				"Sea Surface Wave To Direction",
            				"NDBC Buoy Station Sea Surface Wave To Direction",
            				"deg",
            				DataType.FLOAT);
            		dataStruct.addComponent("sea_surface_wave_to_direction", c);
            		
            		c = helper.newQuantity(SWEHelper.getPropertyUri("sea_surface_swell_wave_to_direction"),
            				"Sea Surface Swell Wave To Direction",
            				"NDBC Buoy Station Sea Surface Swell Wave To Direction",
            				"deg",
            				DataType.FLOAT);
            		dataStruct.addComponent("sea_surface_swell_wave_to_direction", c);
            		
            		c = helper.newQuantity(SWEHelper.getPropertyUri("sea_surface_wind_wave_to_direction"),
            				"Sea Surface Wind Wave To Direction",
            				"NDBC Buoy Station Sea Surface Wind Wave To Direction",
            				"deg",
            				DataType.FLOAT);
            		dataStruct.addComponent("sea_surface_wind_wave_to_direction", c);
            		break;
            		
            		// Ingnoring the below parameters for now
            		// To include them, we need to replace all semicolons in "line" to commas
            		
//            		c = helper.newQuantity(SWEHelper.getPropertyUri("number_of_frequencies"),
//            				"Number of Frequencies",
//            				"NDBC Buoy Station Number of Frequencies",
//            				"count",
//            				DataType.FLOAT);
//            		dataStruct.addComponent("number_of_frequencies", c);
//            		
//            		c = helper.newQuantity(SWEHelper.getPropertyUri("center_frequencies"),
//            				"Center Frequencies",
//            				"NDBC Buoy Station Center Frequencies",
//            				"Hz",
//            				DataType.FLOAT);
//            		dataStruct.addComponent("center_frequencies", c);
//            		
//            		c = helper.newQuantity(SWEHelper.getPropertyUri("bandwidths"),
//            				"Bandwidths",
//            				"NDBC Buoy Station Bandwidths",
//            				"Hz",
//            				DataType.FLOAT);
//            		dataStruct.addComponent("bandwidths", c);
//            		
//            		c = helper.newQuantity(SWEHelper.getPropertyUri("spectral_energy"),
//            				"Spectral Energy",
//            				"NDBC Buoy Station Spectral Energy",
//            				"m^2/Hz",
//            				DataType.FLOAT);
//            		dataStruct.addComponent("spectral_energy", c);
//            		
//            		c = helper.newQuantity(SWEHelper.getPropertyUri("mean_wave_direction"),
//            				"Mean Wave Direction",
//            				"NDBC Buoy Station Mean Wave Direction",
//            				"deg",
//            				DataType.FLOAT);
//            		dataStruct.addComponent("mean_wave_direction", c);
//            		
//            		c = helper.newQuantity(SWEHelper.getPropertyUri("principle_wave_direction"),
//            				"Principle Wave Direction",
//            				"NDBC Buoy Station Principle Wave Direction",
//            				"deg",
//            				DataType.FLOAT);
//            		dataStruct.addComponent("principle_wave_direction", c);
//            		
//            		c = helper.newQuantity(SWEHelper.getPropertyUri("polar_coordinate_r1"),
//            				"Polar Coordinate R1",
//            				"NDBC Buoy Station Polar Coordinate R1",
//            				"1",
//            				DataType.FLOAT);
//            		dataStruct.addComponent("polar_coordinate_r1", c);
//            		
//            		c = helper.newQuantity(SWEHelper.getPropertyUri("polar_coordinate_r2"),
//            				"Polar Coordinate R2",
//            				"NDBC Buoy Station Polar Coordinate R2",
//            				"1",
//            				DataType.FLOAT);
//            		dataStruct.addComponent("polar_coordinate_r2", c);
//            		
//            		c = helper.newQuantity(SWEHelper.getPropertyUri("calculation_method"),
//            				"Calculation Method",
//            				"NDBC Buoy Station Calculation Method",
//            				"1",
//            				DataType.FLOAT);
//            		dataStruct.addComponent("calculation_method", c);
//            		
//            		c = helper.newQuantity(SWEHelper.getPropertyUri("sampling_rate"),
//            				"Sampling Rate",
//            				"NDBC Buoy Station Sampling Rate",
//            				"Hz",
//            				DataType.FLOAT);
//            		dataStruct.addComponent("sampling_rate", c);
            		
            	case "winds":
            		dataStruct.addComponent("depth", helper.newQuantity("http://sensorml.com/ont/swe/property/buoy_depth", "Buoy Depth", null, "m", DataType.FLOAT));
            		c = helper.newQuantity(SWEHelper.getPropertyUri("wind_from_direction"),
            				"Wind From Direction",
            				"NDBC Buoy Station Wind From Direction",
            				"deg",
            				DataType.FLOAT);
            		dataStruct.addComponent("wind_from_direction", c);
            		
            		c = helper.newQuantity(SWEHelper.getPropertyUri("wind_speed"),
            				"Wind Speed",
            				"NDBC Buoy Station Wind Speed",
            				"[m/s]",
            				DataType.FLOAT);
            		dataStruct.addComponent("wind_speed", c);
            		
            		c = helper.newQuantity(SWEHelper.getPropertyUri("wind_speed_of_gust"),
            				"Wind Speed of Gust",
            				"NDBC Buoy Station Wind Speed of Gust",
            				"[m/s]",
            				DataType.FLOAT);
            		dataStruct.addComponent("wind_speed_of_gust", c);
            		
            		// ignore upward air velocity for now; no placeholder value is given
            		c = helper.newQuantity(SWEHelper.getPropertyUri("upward_air_velocity"),
            				"Wind Upward Air Velocity",
            				"NDBC Buoy Station Upward Air Velocity",
            				"[m/s]",
            				DataType.FLOAT);
            		dataStruct.addComponent("upward_air_velocity", c);
            		break;
            }
        }
        
        // use text encoding with default separators
        encoding = helper.newTextEncoding();
    }
    
    protected String getDefUri(ObsParam param)
    {
        String name = param.toString().replaceAll(" ", "");
        return SWEHelper.getPropertyUri(name);
    }
    
    
    protected String getLabel(ObsParam param)
    {
    	String[] label_arr = param.toString().toLowerCase().split("_");
    	StringJoiner joiner = new StringJoiner(" ");
    	String temp = "";
    	for (int i = 0; i < label_arr.length; i++) {
    		temp = label_arr[i].substring(0, 1).toUpperCase() + label_arr[i].substring(1);
    		joiner.add(temp);
    	}
        return joiner.toString();
    }
    
    @Override
    public String getName()
    {
        return dataStruct.getName();
    }


    @Override
    public DataComponent getRecordDescription()
    {
        return dataStruct;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return encoding;
    }
}
