package org.sensorhub.impl.sensor.domoticz;

import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.Text;
import net.opengis.swe.v20.Vector;

public class DomoticzSWEHelper
{
	
	public Quantity getIdxSWE()
	{
    	SWEHelper sweHelpIdx = new SWEHelper();

    	Quantity idx = sweHelpIdx.newQuantity("http://sensorml.com/ont/swe/property/SensorID", 
        		"Sensor ID", 
        		"ID of Sensor", 
        		null, DataType.ASCII_STRING);
        
        return idx;
	}
	
	public Text getNameSWE()
	{
    	SWEHelper sweHelpName = new SWEHelper();

    	Text name = sweHelpName.newText("http://sensorml.com/ont/swe/property/SensorName", 
        		"Sensor Name", "Name of Sensor from Domoticz");
        
        return name;
	}
	
	public Quantity getBatteryLevelSWE()
	{
		SWEHelper sweHelpBatt = new SWEHelper();
		
		Quantity battery = sweHelpBatt.newQuantity("http://sensorml.com/ont/swe/property/BatteryLevel", 
        		"Battery Level", 
        		"Battery Level of Switch", 
        		"%", DataType.INT);
		
		return battery;
	}
	
	public Quantity getDataSWE()
	{
		SWEHelper sweHelpData = new SWEHelper();
		
        Quantity data = sweHelpData.newQuantity("http://sensorml.com/ont/swe/property/Data",
        		"Current Data", 
        		"Current data value offered by sensor", 
        		null, DataType.ASCII_STRING);
        
        return data;
	}
	
	public Quantity getEnviroDataSWE()
	{
		SWEHelper sweHelpEnviroData = new SWEHelper();
		
        Quantity enviroData = sweHelpEnviroData.newQuantity("http://sensorml.com/ont/swe/property/EnvironmentData",
        		"Environment Data", 
        		"Current data value offered by environmental sensor", 
        		null, DataType.ASCII_STRING);
        
        return enviroData;
	}
	
	public Quantity getTempSWE()
	{
		SWEHelper sweHelpTemp = new SWEHelper();
		
		Quantity temp = sweHelpTemp.newQuantity("http://sensorml.com/ont/swe/property/Temperature", 
        		"Air Temperature", 
        		"Temperature of Surrounding Air", 
        		"Cel", DataType.DOUBLE);
		
		return temp;
	}
	
	public Quantity getRelHumSWE()
	{
		SWEHelper sweHelpHum = new SWEHelper();
		
		Quantity relhum = sweHelpHum.newQuantity("http://sensorml.com/ont/swe/property/RelativeHumidity", 
        		"Relative Humidity", 
        		"Relative Humidity", 
        		"%", DataType.INT);
		
		return relhum;
	}
	
	public Quantity getLumSWE()
	{
		SWEHelper sweHelpLum = new SWEHelper();
		
		Quantity lux = sweHelpLum.newQuantity("http://sensorml.com/ont/swe/property/Illuminance", 
        		"Illuminance", 
        		"Luminous Flux per Area", 
        		"lx", DataType.INT);
		
		return lux;
	}
	
	public Quantity getUVISWE()
	{
		SWEHelper sweHelpUV = new SWEHelper();
		
		Quantity uvi = sweHelpUV.newQuantity("http://sensorml.com/ont/swe/property/UVI", 
        		"UV Index", 
        		"Index of Ultraviolet Radiation", 
        		"UVI", DataType.DOUBLE);
		
		return uvi;
	}
	
	public Quantity getSwitchStateSWE()
	{
		SWEHelper sweHelpSwitch = new SWEHelper();
		
		Quantity state = sweHelpSwitch.newQuantity("http://sensorml.com/ont/swe/property/SwitchState", 
        		"Switch Status", 
        		"Status of Switch", 
        		null, DataType.ASCII_STRING);
		
		return state;
	}
	
	public Quantity getMotionStatusSWE()
	{
		SWEHelper sweHelpMotion = new SWEHelper();
		
		Quantity motion = sweHelpMotion.newQuantity("http://sensorml.com/ont/swe/property/MotionStatus", 
        		"Motion Status", 
        		"Status of Motion Switch", 
        		null, DataType.ASCII_STRING);
		
		return motion;
	}
	
	public Quantity getSetLevelSWE()
	{
		SWEHelper sweHelpSetLevel = new SWEHelper();
		
		Quantity level = sweHelpSetLevel.newQuantity("http://sensorml.com/ont/swe/property/SetLevel", 
        		"Set Level", 
        		"Level of Selector Switch", 
        		"%", DataType.INT);
		
		return level;
	}
	
	public Vector getLocVecSWE()
	{
		GeoPosHelper posHelp = new GeoPosHelper();
		
		Vector locVector = posHelp.newLocationVectorLLA(SWEConstants.DEF_SENSOR_LOC);
        locVector.setLabel("Location");
        locVector.setDescription("Location LLA input by user");
        
        return locVector;
	}
	
	public Text getLocDescSWE()
	{
		SWEHelper sweHelpLocDesc = new SWEHelper();
		
        Text locDesc = sweHelpLocDesc.newText("http://sensorml.com/ont/swe/property/LocationDescription",
        		"Location Description", "Sensor Location Description");
        
        return locDesc;
	}
	
	public Text getSensorTypeSWE()
	{
		SWEHelper sweHelpSensorType = new SWEHelper();
		
        Text type = sweHelpSensorType.newText("http://sensorml.com/ont/csm/property/SENSOR_TYPE",
        		"Sensor Type", "Type of Sensor given by Domoticz");
        
        return type;
	}
	
	public Quantity getSensorSubTypeSWE()
	{
		SWEHelper sweHelpSensorSubType = new SWEHelper();
		
        Quantity subtype = sweHelpSensorSubType.newQuantity("http://sensorml.com/ont/csm/property/SENSOR_TYPE",
        		"Sensor Subtype", 
        		"Subtype of Sensor input by User", 
        		null, DataType.ASCII_STRING);
        
        return subtype;
	}
	
	public Text getAlertMsgSWE()
	{
		SWEHelper sweHelpAlert = new SWEHelper();
		
        Text alert = sweHelpAlert.newText("http://sensorml.com/ont/csm/property/AlertMessage",
        		"Alert Message", "Alert Message for Domoticz device");
        
        return alert;
	}
	
	// Continue
	// .
	// .
	// .
}
