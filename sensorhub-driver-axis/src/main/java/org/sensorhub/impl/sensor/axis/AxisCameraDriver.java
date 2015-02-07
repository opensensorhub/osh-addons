/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.axis;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorModule;



/**
 * <p>
 * Implementation of sensor interface for generic Axis Cameras using IP
 * protocol
 * </p>
 *
 * <p>
 * Copyright (c) 2014
 * </p>
 * 
 * @author Mike Botts <mike.botts@botts-inc.com>
 * @since October 30, 2014
 */

public class AxisCameraDriver extends AbstractSensorModule<AxisCameraConfig>
{
	private static final Log log = LogFactory.getLog(AxisCameraDriver.class);
	
	AxisVideoOutput videoDataInterface;
    AxisSettingsOutput ptzDataInterface;
    AxisVideoControl videoControlInterface;
    AxisPtzControl ptzControlInterface;
    
    String ipAddress;



    public AxisCameraDriver()
    {
    	
    }
    
    @Override
    public void start() throws SensorException
    {
    	ipAddress = getConfiguration().ipAddress;
    	
    	// check first if connected
    	if (isConnected()){
    	
	    	// establish the outputs and controllers (video and PTZ)   	
	    	// add video output and controller
	        this.videoDataInterface = new AxisVideoOutput(this);
	        addOutput(videoDataInterface, false);
	
	        //this.videoControlInterface = new AxisVideoControl(this);
	        //addControlInput(videoControlInterface);
	        
	        videoDataInterface.init();
	        //videoControlInterface.init();	        
	        
	        /** check if PTZ supported  **/
	        try
	        {
	        
		        URL optionsURL = new URL("http://" + ipAddress + "/axis-cgi/view/param.cgi?action=list&group=root.Properties.PTZ");
		        InputStream is = optionsURL.openStream();
		        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		        
		        boolean ptzSupported = false;
		
		        String line;
		        while ((line = reader.readLine()) != null)
		        {
		            // parse response
		            String[] tokens = line.split("=");
		
		            if (tokens[0].trim().equalsIgnoreCase("root.Properties.PTZ.PTZ"))
		                ptzSupported = tokens[1].equalsIgnoreCase("yes");    	
		        }
		        
		        if (ptzSupported){
		        	
		        	// add PTZ output
			        this.ptzDataInterface = new AxisSettingsOutput(this);
			        addOutput(ptzDataInterface, false);
			        ptzDataInterface.init();
			        
			        // add PTZ controller
			        this.ptzControlInterface = new AxisPtzControl(this);
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
    protected void updateSensorDescription() throws SensorException
    {
        synchronized (sensorDescription)
        {
        	// TODO get sensor info (camera model, serial no, etc.) camera and add to SensorML description
        	// use http://192.168.1.50/axis-cgi/view/param.cgi?action=list&group= ... root.Brand.* and root.Properties.*
        	
            // parent class reads SensorML from config if provided
            // and then sets unique ID, outputs and control inputs
            super.updateSensorDescription();
            
            // add more stuff in SensorML here
            sensorDescription.setId("AXIS_CAMERA_SENSOR");
        }
    }


    @Override
    public boolean isConnected()
    {
        try
        {
        	// try to open stream and check for AXIS Brand
	        URL optionsURL = new URL("http://" + ipAddress + "/axis-cgi/view/param.cgi?action=list&group=root.Brand.Brand");
		    URLConnection conn = optionsURL.openConnection();
		    conn.setConnectTimeout(500);
		    conn.connect();
		    InputStream is = conn.getInputStream();
	        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
	        
	        // note: should return one line with root.Brand.Brand=AXIS
            String line = reader.readLine();
		    if (line != null)
		    {
		        String[] tokens = line.split("=");	
	            if ((tokens[0].trim().equalsIgnoreCase("root.Brand.Brand")) && (tokens[1].trim().equalsIgnoreCase("AXIS")))
	                return true; 
		    }
		    
		    return false;
        }
        catch (Exception e)
        {
            return false;
        }   
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
