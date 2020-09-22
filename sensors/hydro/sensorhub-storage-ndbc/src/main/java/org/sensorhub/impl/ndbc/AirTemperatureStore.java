/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2020 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.ndbc;

import org.sensorhub.api.data.IMultiSourceDataInterface;

public class AirTemperatureStore extends BuoyRecordStore 
{
	public AirTemperatureStore() {
		super(BuoyParam.AIR_TEMPERATURE);
        
        dataStruct = headerBuilder
        .addField("air_temperature", sweHelper.createQuantity()
        	.label("Air Temperature")
        	.definition(MMI_CF_DEF_PREFIX + "air_temperature")
        	.uomCode("degC")
        	.build())
        .build();

        dataStruct.setName("AIR_TEMPERATURE");
        dataStruct.getFieldList().getProperty(1).setRole(IMultiSourceDataInterface.ENTITY_ID_URI);
        dataStruct.setDefinition("urn:darpa:oot:message:airtemp");  // if needed
        encoding = sweHelper.newTextEncoding();
    }
	    
}
