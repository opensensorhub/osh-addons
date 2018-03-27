package org.sensorhub.impl.ndbc;

import java.util.Set;
import java.util.StringJoiner;

import org.sensorhub.api.data.IMultiSourceDataInterface;
import org.sensorhub.api.persistence.IRecordStoreInfo;
import org.sensorhub.impl.ndbc.BuoyEnums.ObsParam;
import org.vast.swe.SWEHelper;

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
        
        // TODO sort params by code?        
        
        // build record structure with requested parameters
        dataStruct = helper.newDataRecord();
        dataStruct.setName(name);
        
        dataStruct.addField("time", helper.newTimeStampIsoUTC());
        dataStruct.addField("station", helper.newText("http://sensorml.com/ont/swe/property/StationID", "Station ID", null));
//        dataStruct.addComponent("depth", helper.newQuantity("http://sensorml.com/ont/swe/property/BuoyDepth", "Buoy Depth", null, "m", DataType.FLOAT));
        dataStruct.getFieldList().getProperty(1).setRole(IMultiSourceDataInterface.ENTITY_ID_URI);
        
        for (ObsParam param: parameters)
        {
            String paramName = param.name().toLowerCase();
            
            DataComponent c;
            switch (paramName) {
            	case "winds":
            		dataStruct.addComponent("depth", helper.newQuantity("http://sensorml.com/ont/swe/property/BuoyDepth", "Buoy Depth", null, "m", DataType.FLOAT));
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
            		
            		c = helper.newQuantity(SWEHelper.getPropertyUri("sea_water_temperature"),
            				"Sea Water Temperature",
            				"NDBC Buoy Station Sea Water Temperature",
            				"degC",
            				DataType.FLOAT);
            		dataStruct.addComponent("sea_water_temperature", c);
            		
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
            		break;
            	default:
            		dataStruct.addComponent("depth", helper.newQuantity("http://sensorml.com/ont/swe/property/BuoyDepth", "Buoy Depth", null, "m", DataType.FLOAT));
            		c = helper.newQuantity(getDefUri(param), getLabel(param), getDesc(param), getUom(param), DataType.FLOAT);
            		dataStruct.addComponent(paramName, c);
            }
//            DataComponent c = helper.newQuantity(
//                    getDefUri(param),
//                    getLabel(param),
//                    getDesc(param),
//                    getUom(param),
//                    DataType.FLOAT);
            
//            dataStruct.addComponent(paramName, c);
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
    
    
    protected String getDesc(ObsParam param)
    {
        return "NDBC Buoy Station " + param.toString();
    }
    
    
    protected String getUom(ObsParam param)
    {
        switch (param)
        {
            case AIR_PRESSURE_AT_SEA_LEVEL:
                return "hPa";                
            case AIR_TEMPERATURE:
                return "Cel";
            case CURRENTS:
                return "[m/s]";
            case SEA_FLOOR_DEPTH_BELOW_SEA_SURFACE:
                return "m";
            case SEA_WATER_ELECTRICAL_CONDUCTIVITY:
                return "mS/cm";
            case SEA_WATER_SALINITY:
                return "psu";
            case SEA_WATER_TEMPERATURE:
            	return "Cel";
            case WAVES:
            	return "1";
            case WINDS:
            	return "1";
        }
        
        return null;
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
