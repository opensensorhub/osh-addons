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
import java.net.URL;
import java.net.URLConnection;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.security.ClientAuth;
import org.sensorhub.impl.sensor.AbstractSensorModule;
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
public class DahuaCameraDriver extends AbstractSensorModule<DahuaCameraConfig>
{
	private static final Logger log = LoggerFactory.getLogger(DahuaCameraDriver.class);
	
	DahuaVideoOutput videoDataInterface;
    DahuaPtzOutput ptzDataInterface;
    DahuaVideoControl videoControlInterface;
    DahuaPtzControl ptzControlInterface;
    
    String ipAddress;
    String serialNumber;
    String modelNumber;


    public DahuaCameraDriver()
    {	
    }
    
    
    @Override
    public void start() throws SensorException
    {
    	ipAddress = getConfiguration().net.remoteHost;
    	    	
    	// check first if connected
    	if (isConnected())
    	{
    		// establish the outputs and controllers (video and PTZ)   	
	    	// video output
	        this.videoDataInterface = new DahuaVideoOutput(this);
	        videoDataInterface.init();
	        addOutput(videoDataInterface, false);	        
	        videoDataInterface.start();
	        
	        // video controller
	        //this.videoControlInterface = new DahuaVideoControl(this);
	        //videoControlInterface.init();
	        //addControlInput(videoControlInterface);
	        	        
	        // check if PTZ supported
	        boolean ptzSupported = false;	        
	        try
	        {
	            setAuth();
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
		        
		        if (ptzSupported)
		        {		        	
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
    	{
    		log.error("connection not established at " + ipAddress);
    		config.enabled = false;
    	}
    }
    

	@Override
    protected void updateSensorDescription()
    {
        synchronized (sensorDescription)
        {
            // parent class reads SensorML from config if provided
            // and then sets unique ID, outputs and control inputs
            super.updateSensorDescription();
            
            // set identifiers in SensorML
            if (AbstractSensorModule.DEFAULT_ID.equals(sensorDescription.getId()))
                sensorDescription.setId("DAHUA_CAM_" + serialNumber);
            sensorDescription.setUniqueIdentifier("urn:dahua:cam:" + modelNumber + ":" + serialNumber);
        }
    }


    @Override
    public boolean isConnected()
    {
    	try
        {
    	    boolean connected = false;
    	    
    	    // try to open stream and check for Dahua Info
    	    setAuth();
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
            
            return connected;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    
    private void setAuth()
    {
        ClientAuth.getInstance().setUser(config.net.user);
        if (config.net.password != null)
            ClientAuth.getInstance().setPassword(config.net.password.toCharArray());
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
