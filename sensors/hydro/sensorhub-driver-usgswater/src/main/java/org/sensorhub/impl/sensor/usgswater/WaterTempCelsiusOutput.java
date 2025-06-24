/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Botts Innovative Research Inc.. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.usgswater;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.TextEncoding;


/**
 * <p>
 * Implementation of sensor interface for USGS Water Data using IP
 * protocol. This particular class stores configuration parameters.
 * </p>
 * 
 * @author Lee Butler
 * @since March 22, 2017
 */

public class WaterTempCelsiusOutput extends AbstractSensorOutput <USGSWaterDriver> 
{
    DataRecord dataStruct;
    TextEncoding encoding;
    Map<String, DataBlock> latestRecords = new LinkedHashMap<String, DataBlock>();

    public WaterTempCelsiusOutput(USGSWaterDriver driver)
    {
        super("waterTemp", driver);
    }
  
    protected void init()
    {   
		  SWEHelper swe = new SWEHelper();
		  GeoPosHelper geoFac = new GeoPosHelper();

		  dataStruct = swe.createRecord()
    			.name(getName())
    			.definition("http://sensorml.com/ont/swe/property/WaterTempCelsius") 

    			.addField("time", "time", swe.createTime()
    		            .asSamplingTimeIsoUTC()
    		            .build())
    			
    			.addField("site", swe.createText()
    					.label("id")
    					.description("siteId")
//    					.definition(SWEHelper.getPropertyUri(""))
    					.build())
    			
    			.addField("location", geoFac.newLocationVectorLatLon(SWEHelper.getPropertyUri("location")))
    			
    			.addField(getName(), swe.createQuantity()
    					.label(getName())
    					.description("Water Temperature parameter, USGS code 00010")
    					.uom("Cel")  // where do I get UOM?
//    					.dataType(DataType.FLOAT)
    					.build())
    	  .build();
    	
        encoding = swe.newTextEncoding(",", "\n");    	
    }

    @Deprecated // Remove after v2 migration completed
    protected void initV1()
    {   
        SWEHelper swe = new SWEHelper();
        
        dataStruct = swe.newDataRecord(5);
        dataStruct.setName(getName());
        dataStruct.setDefinition("http://sensorml.com/ont/swe/property/WaterTempCelsius");
        dataStruct.addField("time", swe.newTimeStampIsoUTC());
        
        // Set definitions to NULL so these outputs are not observable
        dataStruct.addField("site", swe.newText(null, "Site ID", null));
//        dataStruct.getFieldList().getProperty("site").setRole(ENTITY_ID_URI); // tag with entity ID role
        dataStruct.addField("location", swe.newVector(null, SWEConstants.REF_FRAME_4326, new String[]{"lat","lon"}, new String[] {"Geodetic Latitude", "Longitude"}, new String[] {"deg", "deg"}, new String[] {"Lat", "Long"}));
        dataStruct.addField("water_temp_celsius", swe.newQuantity(null, "Water Temperature", "Water Temperature parameter, USGS code 00010", "Cel"));
        
        // use text encoding with "," separators
        encoding = swe.newTextEncoding(",", "\n");
    }
    
    
    public void publishData(List<USGSDataRecord> dataRec)
    {
    	for (USGSDataRecord rec : dataRec)
    	{
            // create and populate datablock
            DataBlock dataBlock;
            if (latestRecord == null)
                dataBlock = dataStruct.createDataBlock();
            else
                dataBlock = latestRecord.renew();
    		
    		int blockPos = 0;
    		dataBlock.setDoubleValue(blockPos++, rec.getTimeStamp()/1000);
    		dataBlock.setStringValue(blockPos++, rec.getSiteCode());
    		dataBlock.setDoubleValue(blockPos++, rec.getSiteLat());
    		dataBlock.setDoubleValue(blockPos++, rec.getSiteLon());
    		dataBlock.setFloatValue(blockPos++, rec.getDataValue());
    		
    		latestRecordTime = System.currentTimeMillis();
    		latestRecord = dataBlock;
    		latestRecords.put(USGSWaterDriver.UID_PREFIX + rec.getSiteCode(), latestRecord); 
//    		eventHandler.publish(new DataEvent(latestRecordTime, rec.getSiteCode(), DischargeOutput.this, dataBlock));
			eventHandler.publish(new DataEvent(latestRecordTime, USGSWaterDriver.UID_PREFIX + rec.getSiteCode(), getName(), latestRecord));
    	}
    }



    @Override
    public DataComponent getRecordDescription()
    {
        return dataStruct;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return encoding;
    }

    protected void start()
    {
    }

	public void stop()
	{	
	}
	
 	@Override
	public double getAverageSamplingPeriod() {
		return 0;
	}
}
