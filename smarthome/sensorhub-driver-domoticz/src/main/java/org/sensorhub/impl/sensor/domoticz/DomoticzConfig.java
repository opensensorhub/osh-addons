package org.sensorhub.impl.sensor.domoticz;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.config.DisplayInfo.Required;
import org.sensorhub.api.sensor.PositionConfig;
import org.sensorhub.api.sensor.PositionConfig.EulerOrientation;
import org.sensorhub.api.sensor.PositionConfig.LLALocation;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.impl.comm.HTTPConfig;
import org.sensorhub.impl.comm.RobustIPConnectionConfig;


public class DomoticzConfig extends SensorConfig
{
	@Required
    @DisplayInfo(desc="Network serial number (used as suffix to generate unique identifier URI)")
    public String serialNumber = "01";
	
	@Required
    @DisplayInfo(desc="Network model number (used as suffix to generate unique identifier URI)")
    public String modelNumber = "AA";
	
    @DisplayInfo(label="HTTP", desc="HTTP configuration")
    public HTTPConfig http = new HTTPConfig();
    
    @DisplayInfo(desc="Geographic position of z-wave controller")
    public PositionConfig position = new PositionConfig();
    
    @DisplayInfo(label="Connection Options")
    public RobustIPConnectionConfig connection = new RobustIPConnectionConfig();
    
    public List<DomoticzDevice> domoticzDevice = new ArrayList<DomoticzDevice>();
    
    public List<String> motionIdx = new ArrayList<String>();
    
    public enum DeviceTypeEnum
    {
    	Temperature,
    	Humidity,
    	Air_Pressure,
    	Temperature_Humidity,
    	Temperature_Humidity_Pressure,
    	Rain,
    	Wind,
    	UV,
    	Count,
    	Electricity,
    	Electricity_P1_Smart_Meter,
    	Air_Quality,
    	Pressure,
    	Percentage,
    	Gas,
    	Lux,
    	Voltage,
    	Text_Sensor,
    	Selector_Switch,
    	Battery_Level,
    	Switch
    }
    
    public List<DeviceTypeEnum> enviroTypes = new ArrayList<DeviceTypeEnum>();
    
    
    public DomoticzConfig() throws IOException
    {
        http.user = "botts";
        http.password = "t3a42or24t3a";
        http.remoteHost = "192.168.0.22";
        
//        http.user = "MOTOROLA-7FFBC";
//        http.password = "db0e10daa990751ddf0c";
//        http.remoteHost = "192.168.0.13";
        
//        http.user = "osh";
//        http.password = "osh12345";
//        http.remoteHost = "192.168.1.164";
        
        http.remotePort = 8080;
        
/****************************************************************************************************/
/* Add Devices in Z-Wave Network Using Above Map                                                    */
/*                                                                                                  */
/* Format: domoticzDevice.add(new DomoticzDevice(deviceIdx,                                         */
/* 												 deviceType,                                        */
/* 												 deviceLoc,                                         */
/* 												 deviceLocDesc,                                     */
/* 												 provideAlert));                                    */
/*                                                                                                  */
/* Note: deviceIdx is type String                                                                   */
/*       deviceType is type DeviceTypeEnum                                                          */
/*       deviceLoc is type double[lat, lon, alt]                                                    */
/*       deviceLocDesc is type String                                                               */
/*       provideAlert is type boolean                                                               */
/****************************************************************************************************/
        
        LocationLLA locIdx8 = new LocationLLA();
        locIdx8.setLat(34.66); locIdx8.setLon(-86.78); locIdx8.setAlt(192.0);
        domoticzDevice.add(new DomoticzDevice("8", DeviceTypeEnum.Selector_Switch, locIdx8, "Sensor Lab", true)); // Dimmable LED Bulb
        
        
        LocationLLA locIdx9 = new LocationLLA();
        locIdx9.setLat(34.55); locIdx9.setLon(-86.78); locIdx9.setAlt(190.0);
        domoticzDevice.add(new DomoticzDevice("9", DeviceTypeEnum.Switch, locIdx9, "Unknown Office", false)); //
        
        
        LocationLLA locIdx10 = new LocationLLA();
        locIdx10.setLat(34.66); locIdx10.setLon(-86.76); locIdx10.setAlt(92.0);
        domoticzDevice.add(new DomoticzDevice("10", DeviceTypeEnum.Switch, locIdx10, "Sensor Lab", true)); // Siren Switch - active
        
        
        LocationLLA locIdx11 = new LocationLLA();
        locIdx11.setLat(34.59); locIdx11.setLon(-87.70); locIdx11.setAlt(98.4);
        domoticzDevice.add(new DomoticzDevice("11", DeviceTypeEnum.Switch, locIdx11, "Unknown Office", false)); // Unknown Alarm Type
        
        
        LocationLLA locIdx12 = new LocationLLA();
        locIdx12.setLat(34.648); locIdx12.setLon(-86.768); locIdx12.setAlt(160.40);
        domoticzDevice.add(new DomoticzDevice("12", DeviceTypeEnum.Switch, locIdx12, "", false)); // Unknown Alarm Level
 

        LocationLLA locIdx14 = new LocationLLA();
        locIdx14.setLat(34.665); locIdx14.setLon(-86.776); locIdx14.setAlt(105.60);
        domoticzDevice.add(new DomoticzDevice("14", DeviceTypeEnum.Temperature, locIdx14, "Sensor Lab", true)); // Lab Motion Sensor Temp
//        

//        domoticzDevice.add(new DomoticzDevice("16",24, devLoc)); // MultiSensor6 Alarm Type
//        

//        domoticzDevice.add(new DomoticzDevice("17",24, devLoc)); // MultiSensor6 Alarm Level
//        

        LocationLLA locIdx19 = new LocationLLA();
        locIdx19.setLat(34.775); locIdx19.setLon(-86.716); locIdx19.setAlt(95.60);
        domoticzDevice.add(new DomoticzDevice("19", DeviceTypeEnum.Switch, locIdx19, "Main Office", true)); // MultiSensor6 Motion
        

        LocationLLA locIdx20 = new LocationLLA();
        locIdx20.setLat(34.6985); locIdx20.setLon(-86.7685); locIdx20.setAlt(101.350);
        domoticzDevice.add(new DomoticzDevice("20", DeviceTypeEnum.Lux, locIdx20, "Main Office", false)); // MultiSensor6 Luminance
        

        LocationLLA locIdx21 = new LocationLLA();
        locIdx21.setLat(34.7222); locIdx21.setLon(-86.7721); locIdx21.setAlt(100.050);
        domoticzDevice.add(new DomoticzDevice("21", DeviceTypeEnum.UV, locIdx21, "Main Office", false)); // MultiSensor6 UV
        

//        domoticzDevice.add(new DomoticzDevice("22",24, devLoc)); // Unknown Alarm Type
//        

//        domoticzDevice.add(new DomoticzDevice("24",24, devLoc)); // Unknown Burglar
//        

//        domoticzDevice.add(new DomoticzDevice("25",24, devLoc)); // Unknown Alarm Type
//        

//        domoticzDevice.add(new DomoticzDevice("27",24, devLoc)); // Unknown Burglar
//        

        LocationLLA locIdx28 = new LocationLLA();
        locIdx28.setLat(34.665); locIdx28.setLon(-86.776); locIdx28.setAlt(101.20);
        domoticzDevice.add(new DomoticzDevice("28", DeviceTypeEnum.Temperature_Humidity, locIdx28, "Main Office", true)); // MultiSensor6 Temp/Hum
        

//        domoticzDevice.add(new DomoticzDevice("29",24, devLoc)); // Unknown Alarm Type
//        

//        domoticzDevice.add(new DomoticzDevice("30",24, devLoc)); // Unknown Switch
//        

//        domoticzDevice.add(new DomoticzDevice("31",24, devLoc)); // Unknown Alarm Type
//        

//        domoticzDevice.add(new DomoticzDevice("32",24, devLoc)); // Unknown Switch
//        

//        domoticzDevice.add(new DomoticzDevice("33",24, devLoc)); // Unknown Switch
//        

//        domoticzDevice.add(new DomoticzDevice("34",24, devLoc)); // Unknown Switch
//        

//        domoticzDevice.add(new DomoticzDevice("35",24, devLoc)); // Unknown Switch
//        

//        domoticzDevice.add(new DomoticzDevice("36",24, devLoc)); // Unknown Switch
//        

//        domoticzDevice.add(new DomoticzDevice("21",18, devLoc)); // Duplicate, Nonsense
//        

//        domoticzDevice.add(new DomoticzDevice("21",3, devLoc)); // Duplicate, Nonsense
//        

//        domoticzDevice.add(new DomoticzDevice("19",24, devLoc)); // Duplicate, Nonsense
        
        
        // Build list of environmental sensor type indexes
        DeviceTypeEnum envTemp = DeviceTypeEnum.Temperature;
        enviroTypes.add(envTemp);
        
        DeviceTypeEnum envHum = DeviceTypeEnum.Humidity;
        enviroTypes.add(envHum);
        
        DeviceTypeEnum envPres = DeviceTypeEnum.Pressure;
        enviroTypes.add(envPres);
        
        // Build list of motion sensor idx's
        // to compare with list of valid sensor idx's
        motionIdx.add("9"); motionIdx.add("19");
    }

    public List<DeviceTypeEnum> getEnviroTypes()
    {
        return enviroTypes;
    }
    
    public List<String> getMotionIdx()
    {
        return motionIdx;
    }
    
    @Override
    public LLALocation getLocation()
    {
        return position.location;
    }
    
    
    @Override
    public EulerOrientation getOrientation()
    {
        return position.orientation;
    }
}
