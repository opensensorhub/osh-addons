package org.sensorhub.impl.sensor.usgswater;

import org.sensorhub.impl.usgs.water.CodeEnums.ObsParam;

public class USGSDataRecord
{
	private ObsParam dataType;
	private long timeStamp;
	private String siteCode;
	private double siteLat;
	private double siteLon;
	private float dataValue;
	
	public USGSDataRecord(ObsParam dataType, long timeStamp, String siteCode, double siteLat, double siteLon, float dataValue)
	{
		this.dataType = dataType;
		this.timeStamp = timeStamp;
		this.siteCode = siteCode;
		this.siteLat = siteLat;
		this.siteLon = siteLon;
		this.dataValue = dataValue;
	}

	
	public ObsParam getDataType()
	{
		return dataType;
	}



	public void setDataType(ObsParam dataType)
	{
		this.dataType = dataType;
	}



	public long getTimeStamp()
	{
		return timeStamp;
	}

	public void setTimeStamp(long timeStamp)
	{
		this.timeStamp = timeStamp;
	}

	public String getSiteCode()
	{
		return siteCode;
	}

	public void setSiteCode(String siteCode)
	{
		this.siteCode = siteCode;
	}

	public double getSiteLat()
	{
		return siteLat;
	}

	public void setSiteLat(double siteLat)
	{
		this.siteLat = siteLat;
	}

	public double getSiteLon()
	{
		return siteLon;
	}

	public void setSiteLon(double siteLon)
	{
		this.siteLon = siteLon;
	}

	public float getDataValue()
	{
		return dataValue;
	}

	public void setDataValue(float dataValue)
	{
		this.dataValue = dataValue;
	}
}
