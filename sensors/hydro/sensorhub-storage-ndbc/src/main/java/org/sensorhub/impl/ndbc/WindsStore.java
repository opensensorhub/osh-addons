package org.sensorhub.impl.ndbc;

import org.sensorhub.api.data.IMultiSourceDataInterface;

public class WindsStore extends BuoyRecordStore {
// station_id,sensor_id,"latitude (degree)","longitude (degree)",date_time,"depth (m)","wind_from_direction (degree)","wind_speed (m/s)","wind_speed_of_gust (m/s)","upward_air_velocity (m/s)"
//	https://mmisw.org/ont/ioos/parameter/wind_from_direction	
//	https://mmisw.org/ont/ioos/parameter/wind_from_speed	
//	https://mmisw.org/ont/ioos/parameter/wind_from_gust	
//  https://mmisw.org/ont/cf/parameter/upward_air_velocity
	
	public WindsStore() {
		super(BuoyParam.WINDS);
        
        dataStruct = headerBuilder
        .addField("wind_direction", sweHelper.createQuantity()
        	.label("Wind Direction")
        	.definition(MMI_IOOS_DEF_PREFIX + "wind_from_direction")
        	.uomCode("deg")
        	.build())
        .addField("wind_speed", sweHelper.createQuantity()
            	.label("Wind Speed")
            	.definition(MMI_IOOS_DEF_PREFIX + "wind_from_speed")
            	.uomCode("m/s")
            	.build())
        .addField("wind_from_gust", sweHelper.createQuantity()
            	.label("Wind Gust")
            	.definition(MMI_IOOS_DEF_PREFIX + "wind_from_gust")
            	.uomCode("m/s")
            	.build())
        .addField("upward_air_velocity", sweHelper.createQuantity()
            	.label("Upward Air Velocity")
            	.definition(MMI_CF_DEF_PREFIX + "upward_air_velocity")
            	.uomCode("m/s")
            	.build())
        
        .build();

        dataStruct.setName("WINDS");
        dataStruct.getFieldList().getProperty(1).setRole(IMultiSourceDataInterface.ENTITY_ID_URI);
        dataStruct.setDefinition("urn:darpa:oot:message:winds");  // 
        encoding = sweHelper.newTextEncoding();
    }
	    
}
