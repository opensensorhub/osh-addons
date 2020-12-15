/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2020 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.ndbc;

import org.sensorhub.api.data.IMultiSourceDataInterface;

public class AirPressureStore extends BuoyRecordStore 
{
	public AirPressureStore() {
		super(BuoyParam.AIR_PRESSURE_AT_SEA_LEVEL);
        
        dataStruct = headerBuilder
        .addField("air_pressure", sweHelper.createQuantity()
        	.label("Air Pressure")
        	.definition(MMI_CF_DEF_PREFIX + "air_pressure_at_sea_level")
        	.uomCode("hPa")
        	.build())
        .build();

        dataStruct.setName("AIR_PRESSURE_AT_SEA_LEVEL");
        dataStruct.getFieldList().getProperty(1).setRole(IMultiSourceDataInterface.ENTITY_ID_URI);
        dataStruct.setDefinition("urn:darpa:oot:message:airpressure");  // if needed
        encoding = sweHelper.newTextEncoding();
    }
	    
}
