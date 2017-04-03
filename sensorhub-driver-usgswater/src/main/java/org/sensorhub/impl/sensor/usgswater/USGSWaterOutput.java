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

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.TextEncoding;
import org.sensorhub.api.data.IMultiSourceDataInterface;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.usgs.water.RecordStore;
import org.sensorhub.impl.usgs.water.WaterDataRecord;
import org.sensorhub.impl.usgs.water.CodeEnums.ObsParam;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;


/**
 * <p>
 * Implementation of sensor interface for USGS Water Data using IP
 * protocol. This particular class stores configuration parameters.
 * </p>
 * 
 * @author Lee Butler <labutler10@gmail.com>
 * @since March 22, 2017
 */

public class USGSWaterOutput extends AbstractSensorOutput<USGSWaterDriver> implements IMultiSourceDataInterface
{
    DataRecord dataStruct;
    TextEncoding encoding;
    Timer timer;
    Map<String, Long> latestUpdateTimes;
    Map<String, DataBlock> latestRecords = new LinkedHashMap<String, DataBlock>();

    public USGSWaterOutput(USGSWaterDriver driver)
    {
        super(driver);
        latestUpdateTimes = new HashMap<String, Long>();
    }


    @Override
    public String getName()
    {
        return "waterData";
    }
  
    
    protected void init()
    {   
        SWEHelper swe = new SWEHelper();
        GeoPosHelper geo = new GeoPosHelper();
        
        dataStruct = swe.newDataRecord();
        dataStruct.setName(getName());
        
        dataStruct.addField("time", swe.newTimeStampIsoUTC());
        dataStruct.addField("site", swe.newText("http://sensorml.com/ont/swe/property/SiteID", "Site ID", null));
        dataStruct.getFieldList().getProperty(1).setRole(IMultiSourceDataInterface.ENTITY_ID_URI);
        dataStruct.addField("location", geo.newLocationVectorLatLon(SWEConstants.DEF_SENSOR_LOC));
        
        for (ObsParam param: parentSensor.getConfiguration().exposeFilter.parameters)
        {
            String paramName = param.name().toLowerCase();
            
            DataComponent c = swe.newQuantity(
                    getDefUri(param),
                    getLabel(param),
                    getDesc(param),
                    getUom(param),
                    DataType.FLOAT);
            
            dataStruct.addComponent(paramName, c);
        }
        
        // use text encoding with "$$" separators
        encoding = swe.newTextEncoding("$$", "\n");
        
//        // build SWE Common record structure
//        waterRecordStruct = fac.newDataRecord(10);
//        waterRecordStruct.setName(getName());
//        waterRecordStruct.setDefinition("http://sensorml.com/ont/swe/property/StreamData");
//        waterRecordStruct.addComponent("time", fac.newTimeStampIsoUTC());
//        waterRecordStruct.addComponent("siteCode", fac.newText("http://sensorml.com/ont/swe/property/StationID", "Site ID", null));
//        waterRecordStruct.addComponent("siteName", fac.newText("http://sensorml.com/ont/swe/property/StationName", "Site Name", null));
//        waterRecordStruct.addComponent("location", geo.newLocationVectorLatLon(SWEConstants.DEF_SENSOR_LOC));
//        waterRecordStruct.addComponent("discharge", fac.newQuantity("http://sensorml.com/ont/swe/property/StreamDischarge", "Stream Discharge", null, "ft3/s"));
//        waterRecordStruct.addComponent("gageHeight", fac.newQuantity("http://sensorml.com/ont/swe/property/GageHeight", "Gage Height", null, "ft"));
//        waterRecordStruct.addComponent("specificConductance", fac.newQuantity("http://sensorml.com/ont/swe/property/SpecificConductance", "Specific Conductance", null, "uS/cm @ 25degC"));
//        waterRecordStruct.addComponent("dissolvedOxygen", fac.newQuantity("http://sensorml.com/ont/swe/property/DissolvedOxygen", "Dissolved Oxygen", null, "mg/L"));
//        waterRecordStruct.addComponent("waterPH", fac.newQuantity("http://sensorml.com/ont/swe/property/WaterPH", "Water PH", null, null));
//        waterRecordStruct.addComponent("waterTemperature", fac.newQuantity("http://sensorml.com/ont/swe/property/WaterTemperature", "Water Temperature", null, "degC"));
//        
//        // mark component providing entity ID
//        waterRecordStruct.getFieldList().getProperty(1).setRole(ENTITY_ID_URI);
//        
//        // Encode using $$ because site names contain commas
//        waterRecordEncoding = fac.newTextEncoding("$$", "\n");
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
    
//    private DataBlock waterRecordToDataBlock(String siteCode, WaterDataRecord rec) throws ParseException
//    {
//    	DataBlock dataBlock = dataStruct.createDataBlock();
//    	
//    	dataBlock.setDoubleValue(0, rec.getMeasurementTime());
//    	dataBlock.setStringValue(1, rec.getSiteCode());
//    	dataBlock.setStringValue(2, rec.getSiteName());
//    	dataBlock.setDoubleValue(3, rec.getSiteLat());
//    	dataBlock.setDoubleValue(4, rec.getSiteLon());
//    	dataBlock.setDoubleValue(5, rec.getDischarge());
//    	dataBlock.setDoubleValue(6, rec.getGageHeight());
//    	dataBlock.setDoubleValue(7, rec.getConductance());
//    	dataBlock.setDoubleValue(8, rec.getDissOxygen());
//    	dataBlock.setDoubleValue(9, rec.getWaterPH());
//    	dataBlock.setDoubleValue(10, rec.getWaterTemp());
//        
//        return dataBlock;
//    }
    
    
    protected void start()
    {
    	
    	if (timer != null)
            return;
    	timer = new Timer();
        
    	TimerTask timerTask = new TimerTask()
    	{
			public void run()
    		{
    			//URL waterURL = new URL(parentSensor.jsonURL);
			    for (String siteCode : parentSensor.getConfiguration().exposeFilter.siteIds)
			    {
			    	WaterDataRecord rec = null;
					// wait a bit before querying next station
                	try { Thread.sleep(1000); }
                    catch (InterruptedException e) { }
			    }
			}
		};
		
		timer.scheduleAtFixedRate(timerTask, 0, (long)(getAverageSamplingPeriod()*1000));
    }
    
    
    @Override
    public double getAverageSamplingPeriod()
    {
        // generating 1 record per second for PTZ settings
        //return 1.0*60*15;
    	return 2.0;
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


	public void stop()
	{
	    if (timer != null)
        {
            timer.cancel();
            timer = null;
        }		
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
