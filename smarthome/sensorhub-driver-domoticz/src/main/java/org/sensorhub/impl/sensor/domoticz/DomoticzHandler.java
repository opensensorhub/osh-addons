package org.sensorhub.impl.sensor.domoticz;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import org.sensorhub.api.common.SensorHubException;

import com.google.gson.Gson;

public class DomoticzHandler
{	
	public DomoticzResponse getFromJSON(String jsonURLfromDriver) throws SensorHubException
	{
		String jsonL = null;
		try
		{
			jsonL = getDomDevices(jsonURLfromDriver);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		Gson gsonL = new Gson();
		DomoticzResponse infoDom = gsonL.fromJson(jsonL, DomoticzResponse.class);
		return infoDom;
	}
	
	public String getDomDevices(String jsonURL)  throws IOException
	{
    	URL urlGetDevices = new URL(jsonURL);
//    	System.out.println("Issuing request: " + urlGetDevices);
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
    static public class DomoticzResponse
    {
    	int ActTime;
    	Result[] result;
    	String status;
    	String title;
    	
    	// Setter & Getter for ActTime
    	public void setActTime(int actTime)
    	{
    		this.ActTime = actTime;
    	}
    	
    	public int getActTime()
    	{
    		return ActTime;
    	}
    	
    	// Setter & Getter for result
    	public void setResult(Result[] result)
    	{
    		this.result = result;
    	}
    	
    	public Result[] getResult()
    	{
    		return result;
    	}
    	
    	// Setter & Getter for Status
    	public void setStatus(String status)
    	{
    		this.status = status;
    	}
    	
    	public String getStatus()
    	{
    		return status;
    	}
    	
    	// Setter & Getter for Title
    	public void setTitle(String title)
    	{
    		this.title = title;
    	}
    	
    	public String getTitle()
    	{
    		return title;
    	}
    }
    
    static public class Result
    {
//    	double AddjMulti;
//    	double AddjMulti2;
//    	double AddjValue;
//    	double AddjValue2;
    	int BatteryLevel;
//    	int CustomImage;
    	String Data;
//    	String Description;
//    	int Favorite;
//    	int HardwareID;
//    	String HardwareName;
//    	String HardwareType;
//    	int HardwareTypeVal;
    	boolean HaveDimmer;
//    	boolean HaveGroupCmd;
//    	boolean HaveTimeout;
    	int Humidity = Integer.MIN_VALUE;
//    	String ID;
//    	String Image;
//    	boolean IsSubDevice;
    	String LastUpdate;
    	int Level = Integer.MIN_VALUE;
//    	int LevelInt;
//    	int MaxDimLevel;
    	String Name;
//    	String Notifications;
//    	String PlanID;
//    	String[] PlanIDs;
//    	boolean Protected;
//    	boolean ShowNotifications;
//    	String SignalLevel;
    	String Status = null;
//    	String StrParam1;
//    	String StrParam2;
//    	String SubType;
    	double Temp = Double.NaN;
//    	String SwitchType;
//    	String SwitchTypeVal;
//    	String Timers;
    	String Type;
//    	String TypeImg;
    	String UVI = null;
//    	int Unit;
//    	int Used;
//    	boolean UsedByCamera;
//    	String XOffset;
//    	String YOffset;
    	String idx;
    	
    	// Setter & Getter for Battery Level
    	public void setBatteryLevel(int batteryLevel)
    	{
    		this.BatteryLevel = batteryLevel;
    	}
    	
    	public int getBatteryLevel()
    	{
    		return BatteryLevel;
    	}
    	
    	// Setter & Getter for Data
    	public void setData(String data)
    	{
    		this.Data = data;
    	}
    	
    	public String getData()
    	{
    		return Data;
    	}
    	
    	// Setter & Getter for Have Dimmer
    	public void setHaveDimmer(boolean haveDimmer)
    	{
    		this.HaveDimmer = haveDimmer;
    	}
    	
    	public boolean getHaveDimmer()
    	{
    		return HaveDimmer;
    	}
    	
    	// Setter & Getter for Humidity
    	public void setHumidity(int humidity)
    	{
    		this.Humidity = humidity;
    	}
    	
    	public int getHumidity()
    	{
    		return Humidity;
    	}
    	
    	// Setter & Getter for Last Update
    	public void setLastUpdate(String lastUpdate)
    	{
    		this.LastUpdate = lastUpdate;
    	}
    	
    	public String getLastUpdate()
    	{
    		return LastUpdate;
    	}
    	
    	// Setter & Getter for Level
    	public void setLevel(int level)
    	{
    		this.Level = level;
    	}
    	
    	public int getLevel()
    	{
    		return Level;
    	}
    	
    	// Setter & Getter for Name
    	public void setName(String name)
    	{
    		this.Name = name;
    	}
    	
    	public String getName()
    	{
    		return Name;
    	}
    	
    	// Setter & Getter for Status
    	public void setStatus(String status)
    	{
    		this.Status = status;
    	}
    	
    	public String getStatus()
    	{
    		return Status;
    	}
    	
    	// Setter & Getter for Temp
    	public void setTemp(double temp)
    	{
    		this.Temp = temp;
    	}
    	
    	public double getTemp()
    	{
    		return Temp;
    	}
    	
    	// Setter & Getter for UVI
    	public void setUVI(String uvi)
    	{
    		this.UVI = uvi;
    	}
    	
    	public String getUVI()
    	{
    		return UVI;
    	}
    	
    	// Setter & Getter for idx
    	public void setIdx(String idx)
    	{
    		this.idx = idx;
    	}
    	
    	public String getIdx()
    	{
    		return idx;
    	}
    	
    	// Setter & Getter for Type
    	public void setType(String type)
    	{
    		this.Type = type;
    	}
    	
    	public String getType()
    	{
    		return Type;
    	}
    }
}
