/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2020 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.ndbc;

import org.sensorhub.api.data.IMultiSourceDataInterface;

public class WaterTemperatureStore extends BuoyRecordStore 
{
	// TODO - split into separate stores- time may be different for each parameter
	
	public WaterTemperatureStore() {
		super(BuoyParam.SEA_WATER_TEMPERATURE);
        
        dataStruct = headerBuilder
        .addField("water_temperature", sweHelper.createQuantity()
        	.label("Water Temperature")
        	.definition(MMI_CF_DEF_PREFIX + "sea_water_temperature")
        	.uomCode("degC")
        	.build())
        .build();

        dataStruct.setName("SEA_WATER_TEMPERATURE");
        dataStruct.getFieldList().getProperty(1).setRole(IMultiSourceDataInterface.ENTITY_ID_URI);
        dataStruct.setDefinition("urn:darpa:oot:message:watertemp");  // if needed
        encoding = sweHelper.newTextEncoding();
    }
	    
}
