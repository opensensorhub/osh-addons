/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
Developer are Copyright (C) 2016 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.dahua;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;

import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.rtpcam.RTPCameraConfig;
import org.sensorhub.impl.sensor.rtpcam.RTPCameraDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * <p>
 * Implementation of sensor interface for generic Dahua Cameras using IP
 * protocol Based on Dahua v1.0 Drivers.
 * </p>
 *
  * @author Mike Botts <mike.botts@botts-inc.com>
 * @since March 2016
 */

public class DahuaCameraDriver extends RTPCameraDriver
{
	private static final Logger log = LoggerFactory.getLogger(DahuaCameraDriver.class);
	
	DahuaVideoOutput videoDataInterface;
    DahuaPtzOutput ptzDataInterface;
    DahuaVideoControl videoControlInterface;
    DahuaPtzControl ptzControlInterface;
    
    DahuaCameraConfig config;
    
    String ipAddress;
    String serialNumber;
    String modelNumber;


    public DahuaCameraDriver()
    {	
    	Authenticator auth = new Authenticator()
    	{
            @Override
			protected PasswordAuthentication getPasswordAuthentication()
			{
				if (this.getRequestingHost().equals(config.remoteHost))
					return new PasswordAuthentication("admin", "op3nsaysam3".toCharArray());
				
				return super.getPasswordAuthentication();
			}
    	};
    	
    	Authenticator.setDefault(auth);
    }
    
    @Override
    public void init(RTPCameraConfig config){
    	this.config = (DahuaCameraConfig) config;
    }
    
    
    @Override
    public void start() throws SensorException
    {
    	ipAddress = getConfiguration().remoteHost;
    	
    	// check first if connected
    	if (isConnected()){
    	
	    	// establish the outputs and controllers (video and PTZ)   	
	    	// add video output and controller
	        this.videoDataInterface = new DahuaVideoOutput(this);
	        addOutput(videoDataInterface, false);
	
	        //this.videoControlInterface = new DahuaVideoControl(this);
	        //addControlInput(videoControlInterface);
	        
	        videoDataInterface.init();
	        //videoControlInterface.init();	  
	        
	        videoDataInterface.start();
	        
	        /** check if PTZ supported  **/
	        boolean ptzSupported = false;	        
	        try
	        {
   
		        URL optionsURL = new URL("http://" + ipAddress + "/cgi-bin/ptz.cgi?action=getCurrentProtocolCaps&channel=0");
		        InputStream is = optionsURL.openStream();
		        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		        
		        String line;
		        while ((line = reader.readLine()) != null)
		        {
		            // parse response
		            String[] tokens = line.split("=");
		
		            if (tokens[0].trim().equalsIgnoreCase("caps.SupportPTZCoordinates"))
		                ptzSupported = tokens[1].equalsIgnoreCase("true");    	
		        }
		        
		        if (ptzSupported){
		        	
		        	// add PTZ output
			        this.ptzDataInterface = new DahuaPtzOutput(this);
			        addOutput(ptzDataInterface, false);
			        ptzDataInterface.init();
			        
			        // add PTZ controller
			        this.ptzControlInterface = new DahuaPtzControl(this);
			        addControlInput(ptzControlInterface);
			        ptzControlInterface.init();
		            	
		        }


	        }
	        catch (Exception e)
	        {
	            e.printStackTrace();
	        }
    	}
    	else
    		log.error("connection not established at " + ipAddress);
    }
    

    @Override
	public RTPCameraConfig getConfiguration()
	{
		return this.config;
	}

	@Override
    protected void updateSensorDescription()
    {
        synchronized (sensorDescription)
        {
            // parent class reads SensorML from config if provided
            // and then sets unique ID, outputs and control inputs
            super.updateSensorDescription();
            
            // add more stuff in SensorML here
            sensorDescription.setId("DAHUA_" + modelNumber + "_" + serialNumber);
        }
    }


    @Override
    public boolean isConnected()
    {
    	boolean connected = false;
        try
        {
        	// try to open stream and check for Dahua Info
	        URL optionsURL = new URL("http://" + ipAddress + "/cgi-bin/magicBox.cgi?action=getSystemInfo");
		    URLConnection conn = optionsURL.openConnection();
		    conn.setConnectTimeout(500);
		    conn.connect();
		    InputStream is = conn.getInputStream();
	        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
	        
	        // note: should return three lines with serialNumber, deviceType (model number), and hardwareVersion
            String line;
            while ((line = reader.readLine()) != null)
            {
		        String[] tokens = line.split("=");	
                if (tokens[0].trim().equalsIgnoreCase("serialNumber")){
                    serialNumber = tokens[1];
                    connected = true; 
                }
                else if (tokens[0].trim().equalsIgnoreCase("deviceType"))
                    modelNumber = tokens[1];
	          }
        }
        catch (Exception e)
        {
            return false;
        }
        return connected;
    }



    @Override
    public void stop()
    {
        if (ptzDataInterface != null)
        	ptzDataInterface.stop();
        
        if (ptzControlInterface != null)
        	ptzControlInterface.stop();
        
       if (videoDataInterface != null)
        	videoDataInterface.stop();
        
       if (videoControlInterface != null)
       		videoControlInterface.stop();
    }


    @Override
    public void cleanup()
    {

    }
    
    @Override
    public void finalize()
    {
        stop();
    }


}
