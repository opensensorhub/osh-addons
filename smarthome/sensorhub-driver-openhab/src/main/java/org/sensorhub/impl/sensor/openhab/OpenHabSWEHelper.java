package org.sensorhub.impl.sensor.openhab;

import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.Text;
import net.opengis.swe.v20.Vector;

public class OpenHabSWEHelper
{
	public Text getNameSWE()
	{
    	SWEHelper sweHelpName = new SWEHelper();

    	Text name = sweHelpName.newText("http://sensorml.com/ont/swe/property/SensorName", 
        		"Sensor Name", "Name of Sensor from OpenHAB");
        
        return name;
	}
	
	public Quantity getBatteryLevelSWE()
	{
		SWEHelper sweHelpBatt = new SWEHelper();
		
		Quantity battery = sweHelpBatt.newQuantity("http://sensorml.com/ont/swe/property/BatteryLevel", 
        		"Battery Level", 
        		"Battery Level of 'Thing'", 
        		"%", DataType.INT);
		
		return battery;
	}
	
	public Quantity getItemStateSWE()
	{
		SWEHelper sweHelpItemState = new SWEHelper();
		
        Quantity itemState = sweHelpItemState.newQuantity("http://sensorml.com/ont/swe/property/ItemState",
        		"Current State", 
        		"Current state of item", 
        		null, DataType.ASCII_STRING);
        
        return itemState;
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
        		"%", DataType.DOUBLE);
		
		return relhum;
	}
	
	public Quantity getLumSWE()
	{
		SWEHelper sweHelpLum = new SWEHelper();
		
		Quantity lux = sweHelpLum.newQuantity("http://sensorml.com/ont/swe/property/Illuminance", 
        		"Illuminance", 
        		"Luminous Flux per Area", 
        		"lx", DataType.FLOAT);
		
		return lux;
	}
	
	public Quantity getUVISWE()
	{
		SWEHelper sweHelpUV = new SWEHelper();
		
		Quantity uvi = sweHelpUV.newQuantity("http://sensorml.com/ont/swe/property/UVI", 
        		"UV Index", 
        		"Index of Ultraviolet Radiation", 
        		"UVI", DataType.INT);
		
		return uvi;
	}
	
	public Quantity getThingStatusSWE()
	{
		SWEHelper sweHelpStatus = new SWEHelper();
		
		Quantity status = sweHelpStatus.newQuantity("http://sensorml.com/ont/swe/property/ThingStatus", 
        		"Thing Status", 
        		"Status of Thing", 
        		null, DataType.ASCII_STRING);
		
		return status;
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
	
	public Quantity getSensorBinarySWE()
	{
		SWEHelper sweHelpBinary = new SWEHelper();
		
		Quantity status = sweHelpBinary.newQuantity("http://sensorml.com/ont/swe/property/SensorState", 
        		"Sensor Status", 
        		"Status of Sensor", 
        		null, DataType.ASCII_STRING);
		
		return status;
	}
	
	public Quantity getAlarmEntrySWE()
	{
		SWEHelper sweHelpEntry = new SWEHelper();
		
		Quantity entry = sweHelpEntry.newQuantity("http://sensorml.com/ont/swe/property/Entry", 
        		"Alarm Status", 
        		"Status of Entry Alarm", 
        		null, DataType.ASCII_STRING);
		
		return entry;
	}
	
	public Quantity getAlarmBurglarSWE()
	{
		SWEHelper sweHelpBurglar = new SWEHelper();
		
		Quantity burg = sweHelpBurglar.newQuantity("http://sensorml.com/ont/swe/property/Burglar", 
        		"Alarm Status", 
        		"Status of Burglar Alarm", 
        		null, DataType.ASCII_STRING);
		
		return burg;
	}
	
	public Quantity getAlarmGeneralSWE()
	{
		SWEHelper sweHelpGen = new SWEHelper();
		
		Quantity gen = sweHelpGen.newQuantity("http://sensorml.com/ont/swe/property/General", 
        		"Alarm Status", 
        		"Status of General Alarm", 
        		null, DataType.ASCII_STRING);
		
		return gen;
	}
	
	public Quantity getAlarmAllSWE()
	{
		SWEHelper sweHelpAlarm = new SWEHelper();
		
		Quantity alarm = sweHelpAlarm.newQuantity("http://sensorml.com/ont/swe/property/Alarm", 
        		"Alarm Status", 
        		"Status of All Alarms", 
        		null, DataType.ASCII_STRING);
		
		return alarm;
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
	
	public Text getBindingTypeSWE()
	{
		SWEHelper sweHelpBindingType = new SWEHelper();
		
        Text type = sweHelpBindingType.newText("http://sensorml.com/ont/csm/property/BindingType",
        		"Binding Type", "Type of Channel given by OpenHAB");
        
        return type;
	}
	
	public Quantity getOwningThingSWE()
	{
		SWEHelper sweHelpOwningThing = new SWEHelper();
		
        Quantity thing = sweHelpOwningThing.newQuantity("http://sensorml.com/ont/csm/property/OwningThing",
        		"Owning Thing", 
        		"'Thing' that owns item", 
        		null, DataType.ASCII_STRING);
        
        return thing;
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
