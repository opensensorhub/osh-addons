package org.sensorhub.impl.ndbc;

import org.sensorhub.api.data.IMultiSourceDataInterface;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

public class WaterTemperatureStore extends BuoyRecordStore 
{
	// TODO - split into separate stores- time may be different for each parameter
	
	public WaterTemperatureStore() {
			super(BuoyParam.SEA_WATER_TEMPERATURE);
	        SWEHelper helper = new SWEHelper();
	        GeoPosHelper geo = new GeoPosHelper();
	        
	        dataStruct = headerBuilder
//	        .addField("location",helper.createVector()
//				.from(geo.newLocationVectorLatLon(SWEConstants.DEF_PLATFORM_LOC))
//				.label("Float GPS Location")
//				.build())
//	        .addField("buoy_depth", helper.createQuantity()
//	        	.label("Buoy Depth")
//	        	.definition(MMI_CF_DEF_PREFIX + "buoy_depth")  // check this
//	        	.uomCode("m")
//	        	.build())
//	        .addField("air_pressure_at_sea_level", helper.createQuantity()
//	        	.label("Air Pressure")
//	        	.description("Air Pressure At Sea Level")
//	        	.definition(MMI_CF_DEF_PREFIX + "air_pressure_at_sea_level")
//	        	.uomCode("hPa")
//	        	.build())
//	        .addField("air_temperature", helper.createQuantity()
//	        	.label("Air Temperature")
//	        	.definition(MMI_CF_DEF_PREFIX + "air_temperature")
//	        	.uomCode("degC")
//	        	.build())
//	        .addField("conductivity", helper.createQuantity()
//	        	.label("Electrical Conductivity")
//	        	.definition(MMI_CF_DEF_PREFIX + "sea_water_electrical_conductivity")
//	        	.uomCode("mS/cm")
//	        	.build())
//	        .addField("salinity", helper.createQuantity()
//	        	.label("sea_water_salinity")
//	        	.definition(MMI_CF_DEF_PREFIX + "sea_water_salinity")
//	        	.uomCode("psu")
//	        	.build())
	        .addField("water_temperature", helper.createQuantity()
	        	.label("Water Temperature")
	        	.definition(MMI_CF_DEF_PREFIX + "sea_water_temperature")
	        	.uomCode("degC")
	        	.build())
	        .build();

	        dataStruct.getFieldList().getProperty(1).setRole(IMultiSourceDataInterface.ENTITY_ID_URI);
	        // dataStruct.setDefinition("blah");  // if needed
	        encoding = helper.newTextEncoding();
	    }
	    
}
