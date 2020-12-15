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

// TODO- add depth to almost all outputs
//  https://mmisw.org/ont/secoora/parameter/depth
abstract public class BuoyRecordStore implements IRecordStoreInfo 
{
    DataRecordBuilder headerBuilder;
	BuoyParam param;
    DataRecord dataStruct;
    DataEncoding encoding;
    static GeoPosHelper geoHelper = new GeoPosHelper();
    static final SWEHelper sweHelper = new SWEHelper();
    static String MMI_CF_DEF_PREFIX = "https://mmisw.org/ont/cf/parameter/";
    static String MMI_SECOORA_DEF_PREFIX = "https://mmisw.org/ont/secoora/parameter/";
    static String MMI_IOOS_DEF_PREFIX = "https://mmisw.org/ont/ioos/parameter/";

    public BuoyRecordStore(BuoyParam param) {
    	this.param = param;
    	createHeaderBuilder();
    }
    
    public void createHeaderBuilder() {
    	headerBuilder = sweHelper.createDataRecord()
        .label("header")
        .description("Header containing message timestamp, floatID, and message number")
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
        return dataStruct;
	}

	@Override
	public DataEncoding getRecommendedEncoding() {
        return encoding;
	}
	  
}
