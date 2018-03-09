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
        dataStruct.addComponent("depth", helper.newQuantity("http://sensorml.com/ont/swe/property/BuoyDepth", "Buoy Depth", null, "m", DataType.FLOAT));
        dataStruct.getFieldList().getProperty(1).setRole(IMultiSourceDataInterface.ENTITY_ID_URI);
        
        for (ObsParam param: parameters)
        {
            String paramName = param.name().toLowerCase();
            
            DataComponent c;
            switch (paramName) {
            	case "winds":
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
//            		c = helper.newQuantity(SWEHelper.getPropertyUri("upward_air_velocity"),
//            				"Wind Upward Air Velocity",
//            				"NDBC Buoy Station Upward Air Velocity",
//            				"[m/s]",
//            				DataType.FLOAT);
//            		dataStruct.addComponent("upward_air_velocity", c);
            		break;
            	default:
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
