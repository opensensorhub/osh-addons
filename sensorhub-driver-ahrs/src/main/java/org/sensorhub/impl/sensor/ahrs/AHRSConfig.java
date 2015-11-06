package org.sensorhub.impl.sensor.ahrs;

import org.sensorhub.api.comm.CommConfig;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.impl.comm.RS232Config;

//import gnu.io.*;

public class AHRSConfig extends SensorConfig 
{

	public String serialNumber;
	
	public String contactPerson;
	
	public String ipAddress;
	
    @DisplayInfo(label="Communication Settings", desc="Settings for selected communication port")
    public CommConfig commSettings;
	
    public AHRSConfig()
    {
        this.moduleClass = AHRSSensor.class.getCanonicalName();
        
        RS232Config serialConf = new RS232Config();
        serialConf.moduleClass = "org.sensorhub.impl.comm.rxtx.RxtxSerialCommProvider";
        serialConf.portName = "/dev/ttyS2";
        serialConf.baudRate = 115200;
        this.commSettings = serialConf;
        
//        System.out.println("RXTX Configured ...");
        
    }

	
}
