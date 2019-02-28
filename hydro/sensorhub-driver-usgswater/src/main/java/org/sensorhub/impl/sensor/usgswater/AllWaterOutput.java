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
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.TextEncoding;
import org.sensorhub.api.data.IMultiSourceDataInterface;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.usgs.water.CodeEnums.ObsParam;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;


/**
 * <p>
 * Implementation of sensor interface for USGS Water Data using IP
 * protocol. This particular class stores configuration parameters.
 * </p>
 * 
 * @author Lee Butler <labutler10@gmail.com>
 * @since March 22, 2017
 */

public class AllWaterOutput extends AbstractSensorOutput <USGSWaterDriver> implements IMultiSourceDataInterface
{
	DataChoice dataChoice;
    DataRecord dataStruct;
    TextEncoding encoding;
    Map<String, DataBlock> latestRecords = new LinkedHashMap<String, DataBlock>();

    public AllWaterOutput(USGSWaterDriver driver)
    {
        super(driver);
    }


    @Override
    public String getName()
    {
        return "All-Data";
    }
    
    
    protected void init()
    {
        SWEHelper swe = new SWEHelper();
        
        dataChoice = swe.newDataChoice();
        dataChoice.setName(getName());
        dataChoice.setDefinition("http://sensorml.com/ont/swe/property/WaterProperties");
        
        for (ObsParam param: parentSensor.getConfiguration().exposeFilter.parameters)
        {
        	String paramName = param.name().toLowerCase();
        	dataStruct = swe.newDataRecord(5);
        	dataStruct.addField("time", swe.newTimeStampIsoUTC());
        	
            // Set definitions to NULL so these outputs are not observable
            dataStruct.addField("site", swe.newText(null, "Site ID", null));
            dataStruct.getFieldList().getProperty("site").setRole(ENTITY_ID_URI); // tag with entity ID role
            dataStruct.addField("location", swe.newVector(null, SWEConstants.REF_FRAME_4326, new String[]{"lat","lon"}, new String[] {"Geodetic Latitude", "Longitude"}, new String[] {"deg", "deg"}, new String[] {"Lat", "Long"}));
            
        	DataComponent c = swe.newQuantity(
        			null,
        			getLabel(param),
        			getDesc(param),
        			getUom(param),
        			DataType.FLOAT);
        	
        	dataStruct.addField(paramName, c);
        	dataChoice.addItem(getItemName(param), dataStruct);
        	dataStruct.clearData();
        }
        
        // use text encoding with "," separators
        encoding = swe.newTextEncoding(",", "\n");
    }
    
    protected String getDefUri(ObsParam param)
    {
        String name = param.toString().replaceAll(" ", "");
        return SWEHelper.getPropertyUri(name);
    }
    
    
    protected String getLabel(ObsParam param)
    {
        return param.toString();
    }
    
    
    protected String getDesc(ObsParam param)
    {
        return param.toString() + " parameter, USGS code " + param.getCode();
    }
    
    
    protected String getUom(ObsParam param)
    {
        switch (param)
        {
            case WATER_TEMP:
                return "Cel";                
            case DISCHARGE:
                return "[cft_i]/s";
            case GAGE_HEIGHT:
                return "[ft_i]";
            case CONDUCTANCE:
                return "uS/cm";
            case OXY:
                return "mg/L";
            case PH:
                return "1";
        }
        
        return null;
    }
    
    protected String getItemName(ObsParam param)
    {
        switch (param)
        {
            case WATER_TEMP:
                return "WaterTemperature";                
            case DISCHARGE:
                return "Discharge";
            case GAGE_HEIGHT:
                return "GageHeight";
            case CONDUCTANCE:
                return "Conductance";
            case OXY:
                return "DissolvedOxygen";
            case PH:
                return "pH";
        }
        
        return null;
    }
    
    
    protected void start()
    {
    }
    
    public void publishData(List<USGSDataRecord> dataRec)
    {
    	DataBlock dataBlock;
    	for (USGSDataRecord rec : dataRec)
    	{
    		dataChoice.setSelectedItem(getItemName(rec.getDataType()));
        	dataBlock = dataChoice.createDataBlock();
        	
    		int blockPos = 1;
    		dataBlock.setDoubleValue(blockPos++, rec.getTimeStamp()/1000);
    		dataBlock.setStringValue(blockPos++, rec.getSiteCode());
    		dataBlock.setDoubleValue(blockPos++, rec.getSiteLat());
    		dataBlock.setDoubleValue(blockPos++, rec.getSiteLon());
    		dataBlock.setFloatValue(blockPos++, rec.getDataValue());

    		latestRecordTime = System.currentTimeMillis();
    		latestRecord = dataBlock;
    		latestRecords.put(USGSWaterDriver.UID_PREFIX + rec.getSiteCode(), latestRecord); 
    		eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, rec.getSiteCode(), AllWaterOutput.this, dataBlock));
    	}
    }
    
    @Override
    public double getAverageSamplingPeriod()
    {
    	return 0;
    }


    @Override
    public DataComponent getRecordDescription()
    {
        return dataChoice;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return encoding;
    }


	public void stop()
	{	
	}
	
    @Override
    public Collection<String> getEntityIDs()
    {
        return parentSensor.getEntityIDs();
    }


    @Override
    public Map<String, DataBlock> getLatestRecords()
    {
        return Collections.unmodifiableMap(latestRecords);
    }


    @Override
    public DataBlock getLatestRecord(String entityID)
    {
        return latestRecords.get(entityID);
    }
}