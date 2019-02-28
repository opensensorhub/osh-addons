package org.sensorhub.impl.sensor.domoticz;

import org.sensorhub.impl.sensor.domoticz.DomoticzConfig.DeviceTypeEnum;

public class DomoticzDevice
{
	public String deviceIdx;
//	public int deviceType;
	public LocationLLA locationLLA;
	public String locDesc;
	public boolean provideAlert;
	public DeviceTypeEnum deviceType;

	public DomoticzDevice()
	{	    
	}
	
	public DomoticzDevice(String deviceIdx, DeviceTypeEnum deviceType, LocationLLA locationLLA, String locDesc, boolean provideAlert)
	{
		this.deviceIdx = deviceIdx;
		this.deviceType = deviceType;
		this.locationLLA = locationLLA;
		this.locDesc = locDesc;
		this.provideAlert = provideAlert;
	}

	public String getDeviceIdx()
	{
		return this.deviceIdx;
	}

	public void setDeviceIdx(String deviceIdx)
	{
		this.deviceIdx = deviceIdx;
	}

	public DeviceTypeEnum getDeviceType()
	{
		return this.deviceType;
	}

	public void setDeviceType(DeviceTypeEnum deviceType)
	{
		this.deviceType = deviceType;
	}

	public LocationLLA getLocationLLA()
	{
		return this.locationLLA;
	}

	public void setLocationLLA(LocationLLA locationLLA)
	{
		this.locationLLA = locationLLA;
	}

	public String getLocDesc()
	{
		return this.locDesc;
	}

	public void setLocDesc(String locDesc)
	{
		this.locDesc = locDesc;
	}

	public boolean getProvideAlert()
	{
		return this.provideAlert;
	}

	public void setProvideAlert(boolean provideAlert)
	{
		this.provideAlert = provideAlert;
	}
}