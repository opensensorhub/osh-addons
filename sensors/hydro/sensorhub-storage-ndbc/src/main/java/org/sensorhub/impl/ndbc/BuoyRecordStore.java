package org.sensorhub.impl.ndbc;

import org.sensorhub.api.data.IMultiSourceDataInterface;
import org.sensorhub.api.persistence.IRecordStoreInfo;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;

public class BuoyRecordStore implements IRecordStoreInfo 
{
    DataRecord dataStruct;
    DataEncoding encoding;
    static String MMI_CF_DEF_PREFIX = "https://mmisw.org/ont/cf/parameter/";

    public BuoyRecordStore() {
        SWEHelper helper = new SWEHelper();
        GeoPosHelper geo = new GeoPosHelper();
        
        helper.createDataRecord()
        .label("header")
        .name("header")
//        .description("Header containing message timestamp, floatID, and message number")
        .addSamplingTimeIsoUTC("Time")
        .addField("BuoyID", helper.createText()
            .label("Buoy Identifier")
            .description("IOOS Identifier for this buoy")
            .definition(SWEHelper.getPropertyUri("buoyID"))
            .build())
        .addField("location",helper.createVector()
			.from(geo.newLocationVectorLatLon(SWEConstants.DEF_PLATFORM_LOC))
			.label("Float GPS Location")
			.build())
        .addField("buoy_depth", helper.createQuantity()
        	.label("Buoy Depth")
        	.definition(MMI_CF_DEF_PREFIX + "buoy_depth")  // check this
        	.uomCode("m")
        	.build())
        .addField("air_pressure_at_sea_level", helper.createQuantity()
        	.label("Air Pressure")
        	.description("Air Pressure At Sea Level")
        	.definition(MMI_CF_DEF_PREFIX + "air_pressure_at_sea_level")
        	.uomCode("hPa")
        	.build())
        .addField("air_temperature", helper.createQuantity()
        	.label("Air Temperature")
        	.definition(MMI_CF_DEF_PREFIX + "air_temperature")
        	.uomCode("degC")
        	.build())
        .addField("conductivity", helper.createQuantity()
        	.label("Electrical Conductivity")
        	.definition(MMI_CF_DEF_PREFIX + "sea_water_electrical_conductivity")
        	.uomCode("mS/cm")
        	.build())
        .addField("salinity", helper.createQuantity()
        	.label("sea_water_salinity")
        	.definition(MMI_CF_DEF_PREFIX + "sea_water_salinity")
        	.uomCode("psu")
        	.build())
        .addField("water_temperature", helper.createQuantity()
        	.label("Water Temperature")
        	.definition(MMI_CF_DEF_PREFIX + "sea_water_temperature")
        	.uomCode("degC")
        	.build());

        
        dataStruct.getFieldList().getProperty(1).setRole(IMultiSourceDataInterface.ENTITY_ID_URI);
        encoding = helper.newTextEncoding();
    }
    
    
	@Override
	public String getName() {
		 return dataStruct.getName();
	}

	@Override
	public DataComponent getRecordDescription() {
		// TODO Auto-generated method stub
        return dataStruct;
	}

	@Override
	public DataEncoding getRecommendedEncoding() {
		// TODO Auto-generated method stub
        return encoding;
	}
	  
}
