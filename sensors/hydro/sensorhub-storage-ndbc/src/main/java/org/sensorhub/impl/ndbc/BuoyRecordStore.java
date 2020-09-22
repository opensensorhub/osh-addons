/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2020 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.ndbc;

import org.sensorhub.api.persistence.IRecordStoreInfo;
import org.vast.swe.SWEBuilders.DataRecordBuilder;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;

abstract public class BuoyRecordStore implements IRecordStoreInfo 
{
    DataRecordBuilder headerBuilder;
	BuoyParam param;
    DataRecord dataStruct;
    DataEncoding encoding;
    static GeoPosHelper geoHelper = new GeoPosHelper();
    static final SWEHelper sweHelper = new SWEHelper();
    static String MMI_CF_DEF_PREFIX = "https://mmisw.org/ont/cf/parameter/";

    public BuoyRecordStore(BuoyParam param) {
    	this.param = param;
    	createHeaderBuilder();
    }
    
    public void createHeaderBuilder() {
    	headerBuilder = sweHelper.createDataRecord()
//        .definition(OOT_DEF_PREFIX + "header")
        .label("header")
        .description("Header containing message timestamp, floatID, and message number")
        // means
//        .addField("imu_altitude_mean", helper.createQuantity()
//            .uomCode("m")
//            .build())
        .addSamplingTimeIsoUTC("Time")
        .addField("floatID", sweHelper.createText()
            .label("Buoy ID")
            .description("Buoy unique identifier (IMEI modem identifier is used)")
            .definition(SWEHelper.getPropertyUri("IMEI"))
            .build())
        .addField("msgID", sweHelper.createText()
                .label("MessageID")
                .description("MessageID not available for NDBC Buoys")
                .definition(SWEHelper.getPropertyUri("msgID"))
                .build());
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
