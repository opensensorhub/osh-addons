package org.sensorhub.impl.sensor.domoticz;

public class DomoticzDevice
{
	private String deviceIdx;
	private int deviceType;
	private LocationLLA locationLLA;
	private String locDesc;
	private boolean provideAlert;

	public DomoticzDevice(String deviceIdx, int deviceType, LocationLLA locationLLA, String locDesc, boolean provideAlert)
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

	public int getDeviceType()
	{
		return this.deviceType;
	}

	public void setDeviceType(int deviceType)
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