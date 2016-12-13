package org.sensorhub.impl.sensor.zwavedom;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import org.sensorhub.api.common.SensorHubException;
import com.google.gson.Gson;

public class ZWaveDomHandler
{	
	public LightInfoArray getLightFromJSON(String jsonURLfromDriver) throws SensorHubException
	{
		String jsonL = null;
		try
		{
			jsonL = getZWaveDevices(jsonURLfromDriver);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		Gson gsonL = new Gson();
		LightInfoArray infoLight = gsonL.fromJson(jsonL, LightInfoArray.class);
		return infoLight;
	}
	
	public WeatherInfoArray getWeatherFromJSON(String jsonURLfromDriver) throws SensorHubException
	{
		String jsonW = null;
		try
		{
			jsonW = getZWaveDevices(jsonURLfromDriver);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		Gson gson = new Gson();
		WeatherInfoArray infoWeather = gson.fromJson(jsonW, WeatherInfoArray.class);
		return infoWeather;
	}
	
	public TempInfoArray getTempFromJSON(String jsonURLfromDriver) throws SensorHubException
	{
		String jsonT = null;
		try
		{
			jsonT = getZWaveDevices(jsonURLfromDriver);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		Gson gson = new Gson();
		TempInfoArray infoTemp = gson.fromJson(jsonT, TempInfoArray.class);
		return infoTemp;
	}
	
	public UtilityInfoArray getUtilityFromJSON(String jsonURLfromDriver) throws SensorHubException
	{
		String jsonT = null;
		try
		{
			jsonT = getZWaveDevices(jsonURLfromDriver);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		Gson gson = new Gson();
		UtilityInfoArray infoUtil = gson.fromJson(jsonT, UtilityInfoArray.class);
		return infoUtil;
	}
	
	public String getZWaveDevices(String jsonURL)  throws IOException
	{
    	URL urlGetDevices = new URL(jsonURL);
    	System.out.println("Issuing request: " + urlGetDevices);
    	InputStream isGetDevices = urlGetDevices.openStream();
    	BufferedReader reader = null;
    	try
    	{
    		reader = new BufferedReader(new InputStreamReader(isGetDevices));
    		StringBuffer response = new StringBuffer();
	    	String line;
	    	while ((line = reader.readLine()) != null)
	    	{
	    		response.append(line);
	    	}
	    	return response.toString();
    	}
    	catch (IOException e)
        {
            throw new IOException("Cannot read server response", e);
        }
    	finally
    	{
    		if (reader != null)
    			reader.close();
    		if (isGetDevices != null)
    			isGetDevices.close();
    	}
	}
    
    // Class to serialize JSON response
    static public class LightInfo
    {
    	String AddjMulti;
    	String AddjMulti2;
    	String AddjValue;
    	String AddjValue2;
    	String BatteryLevel;
    	String CustomImage;
    	String Data;
    	String Description;
    	String Favorite;
    	String HardwareID;
    	String HardwareName;
    	String HardwareType;
    	String HardwareTypeVal;
    	String HaveDimmer;
    	String HaveGroupCmd;
    	String HaveTimeout;
    	String ID;
    	String Image;
    	String IsSubDevice;
    	String LastUpdate;
    	String Level;
    	String LevelInt;
    	String MaxDimLevel;
    	String Name;
    	String Notifications;
    	String PlanID;
    	String[] PlanIDs;
    	String Protected;
    	String ShowNotifications;
    	String SignalLevel;
    	String Status;
    	String StrParam1;
    	String StrParam2;
    	String SubType;
    	String SwitchType;
    	String SwitchTypeVal;
    	String Timers;
    	String Type;
    	String TypeImg;
    	String Unit;
    	String Used;
    	String UsedByCamera;
    	String XOffset;
    	String YOffset;
    	String idx;
    }
    
    static public class LightInfoArray
    {
    	LightInfo[] result;
    }
   
  // Class to serialize JSON response
    static public class WeatherInfo
    {    	
    	String AddjMulti;
	  	String AddjMulti2;
	  	String AddjValue;
	  	String AddjValue2;
	  	String Barometer;
	  	String BatteryLevel;
	  	String Chill;
	  	String CustomImage;
	  	String Data;
	  	String Description;
	  	String DewPoint;
	  	String Direction;
	  	String DirectionStr;
	  	String Gust;
	  	String Favorite;
	  	String Forecast;
	  	String ForecastStr;
	  	String forecast_url;
	  	String HardwareID;
	  	String HardwareName;
	  	String HardwareType;
	  	String HardwareTypeVal;
	  	String HaveTimeout;
	  	String Humidity;
	  	String HumidityStatus;
	  	String ID;
	  	String LastUpdate;
	  	String Name;
	  	String Notifications;
	  	String PlanID;
	  	String[] PlanIDs;
	  	String Protected;
	  	String Rain;
	  	String RainRate;
	  	String ShowNotifications;
	  	String SignalLevel;
	  	String Speed;
	  	String SubType;
	  	String Temp;
	  	String Timers;
	  	String Type;
	  	String TypeImg;
	  	String Unit;
	  	String Used;
	  	String UVI;
	  	String Visibility;
	  	String XOffset;
	  	String YOffset;
	  	String idx;
	}
    
    static public class WeatherInfoArray
    {
    	WeatherInfo[] result;
    }
    
    // Class to serialize JSON response
    static public class TempInfo
    {    	
    	String AddjMulti;
    	String AddjMulti2;
    	String AddjValue;
    	String AddjValue2;
    	String Barometer;
    	String BatteryLevel;
    	String Chill;
    	String CustomImage;
    	String Data;
    	String Description;
    	String DewPoint;
    	String Direction;
    	String DirectionStr;
    	String Gust;
    	String Favorite;
    	String Forecast;
    	String ForecastStr;
    	String forecast_url;
    	String HardwareID;
    	String HardwareName;
    	String HardwareType;
    	String HardwareTypeVal;
    	String HaveTimeout;
    	String Humidity;
    	String HumidityStatus;
    	String ID;
    	String LastUpdate;
    	String Name;
    	String Notifications;
    	String PlanID;
    	String[] PlanIDs;
    	String Protected;
    	String ShowNotifications;
    	String SignalLevel;
    	String Speed;
    	String SubType;
    	String Temp;
    	String Timers;
    	String Type;
    	String TypeImg;
    	String Unit;
    	String Used;
    	String XOffset;
    	String YOffset;
    	String idx;  	  	
    }
      
    static public class TempInfoArray
    {
    	TempInfo[] result;
    }
    
    // Class to serialize JSON response
    static public class UtilityInfo
    {    	
		String AddjMulti;
		String AddjMulti2;
		String AddjValue;
		String AddjValue2;
		String BatteryLevel;
		String CustomImage;
		String Data;
		String Description;
		String Favorite;
		String HardwareID;
		String HardwareName;
		String HardwareType;
		String HardwareTypeVal;
		String HaveTimeout;
		String ID;
		String Image;
		String LastUpdate;
		String Name;
		String Notifications;
		String PlanID;
		String[] PlanIDs;
		String Protected;
		String ShowNotifications;
		String SignalLevel;
		String SubType;
		String Timers;
		String Type;
		String TypeImg;
		String Unit;
		String Used;
		String XOffset;
		String YOffset;
		String idx;  	    	
    }
      
    static public class UtilityInfoArray
    {
    	UtilityInfo[] result;
    }
}
