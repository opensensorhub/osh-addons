/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2020 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.ndbc;

import org.sensorhub.api.data.IMultiSourceDataInterface;

//  TODO: Add depth to this and other outputs. Salinity (unlike most other params)
//  is reported for mulitple depths dwon to ~1000 m 
public class SalinityStore extends BuoyRecordStore 
{
	public SalinityStore() {
		super(BuoyParam.SEA_WATER_SALINITY);
        
        dataStruct = headerBuilder
        .addField("salinity", sweHelper.createQuantity()
        	.label("salinity")
        	.definition(MMI_CF_DEF_PREFIX + "sea_water_salinity")
        	.uomCode("1e-3")  // ucum equivalent of psu
        	.build())
        .build();

        dataStruct.setName("SEA_WATER_SALINITY");
        dataStruct.getFieldList().getProperty(1).setRole(IMultiSourceDataInterface.ENTITY_ID_URI);
        dataStruct.setDefinition("urn:darpa:oot:message:salinity");  // if needed
        encoding = sweHelper.newTextEncoding();
    }
	    
}
