package org.sensorhub.impl.sensor.usgswater;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

import org.sensorhub.impl.sensor.usgswater.CodeEnums.ObsParam;
import org.sensorhub.impl.sensor.usgswater.CodeEnums.StateCode;

import com.google.gson.Gson;

public class USGSSitePoller {
	
	WaterML2 waterData;
	
	// Define format of incoming time stamp
	DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
	
	// Pull Data By Site Code
	public WaterDataRecord pullSiteData (String siteCode) throws IOException
	{
		WaterDataRecord rec = new WaterDataRecord();
		
//		URL waterURL = new URL(USGSWaterDriver.USGS_BASE_URL +
//				"&sites=" + siteCode + "&parameterCd=" + 
//				CodeEnums.ObsParam.DISCHARGE.code + "," +
//				CodeEnums.ObsParam.GAGE_HEIGHT.code);
		URL waterURL = new URL(USGSWaterDriver.USGS_BASE_URL + "&sites=" + siteCode);
		USGSWaterDriver.log.debug("Requesting info by site from: " + waterURL);
		InputStream isData = waterURL.openStream();
		BufferedReader reader = null;
		try
		{
			reader = new BufferedReader(new InputStreamReader(isData));
			StringBuffer response = new StringBuffer();
	    	String line;
	    	
	    	while ((line = reader.readLine()) != null)
	    	{
	    		response.append(line);
	    	}
	    	
	    	Gson gsonL = new Gson();
	    	waterData = gsonL.fromJson(response.toString(), WaterML2.class);
	    	
	    	System.out.println("Time Series Length: " + waterData.value.timeSeries.length);
	    	
	    	//dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
	    	Date date = dateFormat.parse(waterData.value.timeSeries[0].values[0].value[0].dateTime);
	    	long seconds = date.getTime();
	    	
	    	rec.setMeasurementTime(seconds/1000);
	    	rec.setSiteCode(waterData.value.timeSeries[0].sourceInfo.siteCode[0].value);
	    	rec.setSiteName(waterData.value.timeSeries[0].sourceInfo.siteName);
	    	rec.setSiteLat(waterData.value.timeSeries[0].sourceInfo.geoLocation.geogLocation.latitude);
	    	rec.setSiteLon(waterData.value.timeSeries[0].sourceInfo.geoLocation.geogLocation.longitude);
	    	
	    	for (int k = 0; k < waterData.value.timeSeries.length; k++)
	    	{
	    		if (ObsParam.DISCHARGE.code.equalsIgnoreCase(waterData.value.timeSeries[k].variable.variableCode[0].value))
	    		{
	    			rec.setDischarge(Double.parseDouble(waterData.value.timeSeries[k].values[0].value[0].value));
	    			break;
	    		}
	    		
	    		else if (ObsParam.GAGE_HEIGHT.code.equalsIgnoreCase(waterData.value.timeSeries[k].variable.variableCode[0].value))
	    		{
	    			rec.setGageHeight(Double.parseDouble(waterData.value.timeSeries[k].values[0].value[0].value));
	    			break;
	    		}
	    		
	    		else if (ObsParam.CONDUCTANCE.code.equalsIgnoreCase(waterData.value.timeSeries[k].variable.variableCode[0].value))
	    		{
	    			rec.setConductance(Double.parseDouble(waterData.value.timeSeries[k].values[0].value[0].value));
	    			break;
	    		}
	    		
	    		else if (ObsParam.OXY.code.equalsIgnoreCase(waterData.value.timeSeries[k].variable.variableCode[0].value))
	    		{
	    			rec.setDissOxygen(Double.parseDouble(waterData.value.timeSeries[k].values[0].value[0].value));
	    			break;
	    		}
	    		
	    		else if (ObsParam.PH.code.equalsIgnoreCase(waterData.value.timeSeries[k].variable.variableCode[0].value))
	    		{
	    			rec.setWaterPH(Double.parseDouble(waterData.value.timeSeries[k].values[0].value[0].value));
	    			break;
	    		}
	    		
	    		else if (ObsParam.WATER_TEMP.code.equalsIgnoreCase(waterData.value.timeSeries[k].variable.variableCode[0].value))
	    		{
	    			rec.setWaterTemp(Double.parseDouble(waterData.value.timeSeries[k].values[0].value[0].value));
	    			break;
	    		}
	    	}
	    	
	    	return rec;
		}
		
		catch (Exception e)
	    {
			USGSWaterDriver.log.debug("Error Parsing Response From: " + waterURL);
	    }
		finally
		{
			if (reader != null)
				reader.close();
			if (isData != null)
				isData.close();
		}
		return null;
	}
	
	// Pull Data By State
	public Set<WaterDataRecord> pullStateData (StateCode stateCode) throws IOException
	{
		WaterDataRecord rec;
		Set<WaterDataRecord> recSet = new LinkedHashSet<WaterDataRecord>();
		
		URL waterURL = new URL(USGSWaterDriver.USGS_BASE_URL + "&stateCd=" + stateCode);
		USGSWaterDriver.log.debug("Requesting info by state from: " + waterURL);
		InputStream isData = waterURL.openStream();
		BufferedReader reader = null;
		try
		{
			reader = new BufferedReader(new InputStreamReader(isData));
			StringBuffer response = new StringBuffer();
	    	String line;
	    	
	    	while ((line = reader.readLine()) != null)
	    	{
	    		response.append(line);
	    	}
	    	
	    	Gson gsonL = new Gson();
	    	waterData = gsonL.fromJson(response.toString(), WaterML2.class);
	    	
	    	//dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
	    	System.out.println("Total Num State Objects: " + waterData.value.timeSeries.length);
	    	System.out.println("First Site: " + waterData.value.timeSeries[0].sourceInfo.siteName);
	    	System.out.println("Last Site: " + waterData.value.timeSeries[waterData.value.timeSeries.length-1].sourceInfo.siteName);	    	
	    	
	    	for (int k = 0; k < waterData.value.timeSeries.length; k++)
	    	{
	    		rec = new WaterDataRecord();
	    		
	    		Date date = dateFormat.parse(waterData.value.timeSeries[k].values[0].value[0].dateTime);
	    		long seconds = date.getTime();
	    		
	    		rec.setMeasurementTime(seconds/1000);
		    	rec.setSiteCode(waterData.value.timeSeries[k].sourceInfo.siteCode[0].value);
		    	rec.setSiteName(waterData.value.timeSeries[k].sourceInfo.siteName);
		    	rec.setSiteLat(waterData.value.timeSeries[k].sourceInfo.geoLocation.geogLocation.latitude);
		    	rec.setSiteLon(waterData.value.timeSeries[k].sourceInfo.geoLocation.geogLocation.longitude);
		    	
	    		if (ObsParam.DISCHARGE.code.equalsIgnoreCase(waterData.value.timeSeries[k].variable.variableCode[0].value))
	    		{
	    			rec.setDischarge(Double.parseDouble(waterData.value.timeSeries[k].values[0].value[0].value));
	    			recSet.add(rec);
	    			continue;
	    		}
	    		
	    		else if (ObsParam.GAGE_HEIGHT.code.equalsIgnoreCase(waterData.value.timeSeries[k].variable.variableCode[0].value))
	    		{
	    			rec.setGageHeight(Double.parseDouble(waterData.value.timeSeries[k].values[0].value[0].value));
	    			recSet.add(rec);
	    			continue;
	    		}
	    		
	    		else if (ObsParam.CONDUCTANCE.code.equalsIgnoreCase(waterData.value.timeSeries[k].variable.variableCode[0].value))
	    		{
	    			rec.setConductance(Double.parseDouble(waterData.value.timeSeries[k].values[0].value[0].value));
	    			recSet.add(rec);
	    			continue;
	    		}
	    		
	    		else if (ObsParam.OXY.code.equalsIgnoreCase(waterData.value.timeSeries[k].variable.variableCode[0].value))
	    		{
	    			rec.setDissOxygen(Double.parseDouble(waterData.value.timeSeries[k].values[0].value[0].value));
	    			recSet.add(rec);
	    			continue;
	    		}
	    		
	    		else if (ObsParam.PH.code.equalsIgnoreCase(waterData.value.timeSeries[k].variable.variableCode[0].value))
	    		{
	    			rec.setWaterPH(Double.parseDouble(waterData.value.timeSeries[k].values[0].value[0].value));
	    			recSet.add(rec);
	    			continue;
	    		}
	    		
	    		else if (ObsParam.WATER_TEMP.code.equalsIgnoreCase(waterData.value.timeSeries[k].variable.variableCode[0].value))
	    		{
	    			rec.setWaterTemp(Double.parseDouble(waterData.value.timeSeries[k].values[0].value[0].value));
	    			recSet.add(rec);
	    			continue;
	    		}
	    	}
	    	return recSet;
		}
		
		catch (Exception e)
	    {
			USGSWaterDriver.log.debug("Error Parsing Response From: " + waterURL);
	    }
		finally
		{
			if (reader != null)
				reader.close();
			if (isData != null)
				isData.close();
		}
		return null;
	}
	
	// Pull Data By County
	public Set<WaterDataRecord> pullCountyData (String countyCode) throws IOException
	{
		WaterDataRecord rec;
		Set<WaterDataRecord> recSet = new LinkedHashSet<WaterDataRecord>();

		URL waterURL = new URL(USGSWaterDriver.USGS_BASE_URL + "&countyCd=" + countyCode);
		USGSWaterDriver.log.debug("Requesting info by county from: " + waterURL);
		InputStream isData = waterURL.openStream();
		BufferedReader reader = null;
		try
		{
			reader = new BufferedReader(new InputStreamReader(isData));
			StringBuffer response = new StringBuffer();
	    	String line;
	    	
	    	while ((line = reader.readLine()) != null)
	    	{
	    		response.append(line);
	    	}
	    	
	    	Gson gsonL = new Gson();
	    	waterData = gsonL.fromJson(response.toString(), WaterML2.class);
	    	
	    	//dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
	    	System.out.println("Total Num County Objects: " + waterData.value.timeSeries.length);
	    	System.out.println("First Site: " + waterData.value.timeSeries[0].sourceInfo.siteName);
	    	System.out.println("Last Site: " + waterData.value.timeSeries[waterData.value.timeSeries.length-1].sourceInfo.siteName);
	    	
	    	for (int k = 0; k < waterData.value.timeSeries.length; k++)
	    	{
	    		rec = new WaterDataRecord();
	    		
		    	Date date = dateFormat.parse(waterData.value.timeSeries[k].values[0].value[0].dateTime);
		    	long seconds = date.getTime();
		    	
		    	rec.setMeasurementTime(seconds/1000);
		    	rec.setSiteCode(waterData.value.timeSeries[k].sourceInfo.siteCode[0].value);
		    	rec.setSiteName(waterData.value.timeSeries[k].sourceInfo.siteName);
		    	rec.setSiteLat(waterData.value.timeSeries[k].sourceInfo.geoLocation.geogLocation.latitude);
		    	rec.setSiteLon(waterData.value.timeSeries[k].sourceInfo.geoLocation.geogLocation.longitude);
		    	
	    		if (ObsParam.DISCHARGE.code.equalsIgnoreCase(waterData.value.timeSeries[k].variable.variableCode[0].value))
	    		{
	    			rec.setDischarge(Double.parseDouble(waterData.value.timeSeries[k].values[0].value[0].value));
	    			recSet.add(rec);
	    			continue;
	    		}
	    		
	    		else if (ObsParam.GAGE_HEIGHT.code.equalsIgnoreCase(waterData.value.timeSeries[k].variable.variableCode[0].value))
	    		{
	    			rec.setGageHeight(Double.parseDouble(waterData.value.timeSeries[k].values[0].value[0].value));
	    			recSet.add(rec);
	    			continue;
	    		}
	    		
	    		else if (ObsParam.CONDUCTANCE.code.equalsIgnoreCase(waterData.value.timeSeries[k].variable.variableCode[0].value))
	    		{
	    			rec.setConductance(Double.parseDouble(waterData.value.timeSeries[k].values[0].value[0].value));
	    			recSet.add(rec);
	    			continue;
	    		}
	    		
	    		else if (ObsParam.OXY.code.equalsIgnoreCase(waterData.value.timeSeries[k].variable.variableCode[0].value))
	    		{
	    			rec.setDissOxygen(Double.parseDouble(waterData.value.timeSeries[k].values[0].value[0].value));
	    			recSet.add(rec);
	    			continue;
	    		}
	    		
	    		else if (ObsParam.PH.code.equalsIgnoreCase(waterData.value.timeSeries[k].variable.variableCode[0].value))
	    		{
	    			rec.setWaterPH(Double.parseDouble(waterData.value.timeSeries[k].values[0].value[0].value));
	    			recSet.add(rec);
	    			continue;
	    		}
	    		
	    		else if (ObsParam.WATER_TEMP.code.equalsIgnoreCase(waterData.value.timeSeries[k].variable.variableCode[0].value))
	    		{
	    			rec.setWaterTemp(Double.parseDouble(waterData.value.timeSeries[k].values[0].value[0].value));
	    			recSet.add(rec);
	    			continue;
	    		}
	    	}
	    	return recSet;
		}
		
		catch (Exception e)
	    {
			USGSWaterDriver.log.debug("Error Parsing Response From: " + waterURL);
	    }
		finally
		{
			if (reader != null)
				reader.close();
			if (isData != null)
				isData.close();
		}
		return null;
	}
}
