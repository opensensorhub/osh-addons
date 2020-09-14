package org.sensorhub.impl.ndbc;

import org.sensorhub.api.data.IMultiSourceDataInterface;

import net.opengis.swe.v20.Quantity;

public class ConductivityStore extends BuoyRecordStore {

	public ConductivityStore() {
		super(BuoyParam.SEA_WATER_ELECTRICAL_CONDUCTIVITY);
		dataStruct = headerBuilder
        .addField("conductivity", createConductivity())
        .build();

        dataStruct.getFieldList().getProperty(1).setRole(IMultiSourceDataInterface.ENTITY_ID_URI);
        dataStruct.setDefinition("urn:darpa:oot:message:conductivity");  // if needed
        encoding = sweHelper.newTextEncoding();
	}

    protected static Quantity createConductivity()
    {
        return sweHelper.createQuantity()
            .definition(MMI_CF_DEF_PREFIX + "sea_water_electrical_conductivity")
            .label("Water Conductivity")
            .description("Conductivity of the sea surface water.")
            .uomCode("S.m-1")
            .build();
    }

}
