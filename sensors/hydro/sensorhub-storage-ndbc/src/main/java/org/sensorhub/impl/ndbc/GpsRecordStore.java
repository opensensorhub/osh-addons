package org.sensorhub.impl.ndbc;

import org.sensorhub.api.data.IMultiSourceDataInterface;
import org.vast.swe.SWEConstants;
import org.vast.swe.helper.GeoPosHelper;

import net.opengis.swe.v20.Vector;

public class GpsRecordStore extends BuoyRecordStore {

	//  TODO: how to handle? GPS comes in every message from NDBC SOS
    static GeoPosHelper swe = new GeoPosHelper();

	public GpsRecordStore() {
		super(BuoyParam.GPS);
		
		dataStruct = headerBuilder
			.addField("Gps", createGpsLocation())
			.build();
		dataStruct.setDefinition("urn:darpa:oot:message:gps");
		dataStruct.setDescription("Standard OOT Header and GPS position");
		dataStruct.setLabel("GPS");
		dataStruct.setName("GPS");
		dataStruct.getFieldList().getProperty(1).setRole(IMultiSourceDataInterface.ENTITY_ID_URI);
		encoding = sweHelper.newTextEncoding();
	}

	protected static Vector createGpsLocation() {
    	return swe.createVector()
    		.from(swe.newLocationVectorLatLon(SWEConstants.DEF_PLATFORM_LOC))
    		.label("Float GPS Location")
   		.build();
	}

}
