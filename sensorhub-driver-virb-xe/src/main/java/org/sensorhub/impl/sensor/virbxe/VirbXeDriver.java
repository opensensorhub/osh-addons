/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.virbxe;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import net.opengis.sensorml.v20.IdentifierList;
import net.opengis.sensorml.v20.PhysicalSystem;
import net.opengis.sensorml.v20.SpatialFrame;
import net.opengis.sensorml.v20.Term;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.security.ClientAuth;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.vast.sensorML.SMLFactory;
import org.vast.swe.SWEHelper;

import com.google.gson.Gson;


/**
 * <p>
 * Driver for Garmin VIRB XE camera
 * </p>
 *
 * @author Mike Botts <mike.botts@botts-inc.com>
 * @since April 12, 2016
 */
public class VirbXeDriver extends AbstractSensorModule<VirbXeConfig>
{
    protected final static String CRS_ID = "SENSOR_FRAME";
        
    // Navigation Data output
    VirbXeNavOutput navDataInterface;
    
    // Health Data Output
    VirbXeAntOutput healthDataInterface;
    
    // Video Data Output
	//VirbXeOutput videoDataInterface;
	
    String hostName;
    
    String serialNumber = " ";
    String firmware = " ";
    String modelNumber = " ";
   
    boolean doInit = true;
    long connectionRetryPeriod = 2000L;
    
    
    public VirbXeDriver()
    {       
    }


    @Override
    public void init(VirbXeConfig config) throws SensorHubException
    {
        super.init(config);
       
    }
    
    @Override
    public void start() throws SensorHubException
    {
        hostName = "http://" + config.net.remoteHost + "/virb";  
        boolean done = false;
        
        
        // check first if connected
        while (!done && waitForConnection(connectionRetryPeriod, config.connectTimeout))
        {
            // create output only the first time it is started
            if (doInit)
            {
            	// TODO uncomment when videoDataInterface ready
                // video output
//                videoDataInterface = new DahuaVideoOutput(this);
//                videoDataInterface.init();
//                addOutput(videoDataInterface, false);
        
                // create navigation data interface
                navDataInterface = new VirbXeNavOutput(this);
                navDataInterface.init();
                addOutput(navDataInterface, false);
                
                // create health data interface
                healthDataInterface = new VirbXeAntOutput(this);
                healthDataInterface.init();
                if (healthDataInterface.hasSensors())
                	addOutput(healthDataInterface, false);
                
               doInit = false;
            }
            
            // TODO uncomment when videoDataInterface ready
//            videoDataInterface.start();
            navDataInterface.start();
            
            if (healthDataInterface.hasSensors())
            	healthDataInterface.start();
                       
            done = true;
           
        }
    }
    


    @Override
    protected void updateSensorDescription()
    {
        synchronized (sensorDescription)
        {
            super.updateSensorDescription();
            
            SMLFactory smlFac = new SMLFactory();
            sensorDescription.setId("Garmin_VIRB_XE_" + serialNumber);
            sensorDescription.setUniqueIdentifier("urn:garmin:cam:" + serialNumber);
            sensorDescription.setDescription("Garmin VIRB-XE camera with GPS and Orientation" );
            
            
            IdentifierList ident = smlFac.newIdentifierList();
            sensorDescription.getIdentificationList().add(ident);
            Term term;
            
            term = smlFac.newTerm();
            term.setDefinition(SWEHelper.getPropertyUri("Manufacturer"));
            term.setLabel("Manufacturer Name");
            term.setValue("Garmin");
            ident.addIdentifier2(term);
            
            term = smlFac.newTerm();
            term.setDefinition(SWEHelper.getPropertyUri("ModelNumber"));
            term.setLabel("Model Number");
            term.setValue(modelNumber);
            ident.addIdentifier2(term);
            
            term = smlFac.newTerm();
            term.setDefinition(SWEHelper.getPropertyUri("SerialNumber"));
            term.setLabel("Serial Number");
            term.setValue(serialNumber);
            ident.addIdentifier2(term);
            
            term = smlFac.newTerm();
            term.setDefinition(SWEHelper.getPropertyUri("Firmware"));
            term.setLabel("Firmware");
            term.setValue(firmware);
            ident.addIdentifier2(term);
            
            // Long Name
            term = smlFac.newTerm();
            term.setDefinition(SWEHelper.getPropertyUri("LongName"));
            term.setLabel("Long Name");
            term.setValue("Garmin VIRB Video Camera " + modelNumber + ": " + serialNumber);
            ident.addIdentifier2(term);
        
            // Short Name
            term = smlFac.newTerm();
            term.setDefinition(SWEHelper.getPropertyUri("ShortName"));
            term.setLabel("Short Name");
            term.setValue("Garmin VIRB: " + serialNumber);
            ident.addIdentifier2(term);

            
            // TODO check this frame
            SpatialFrame localRefFrame = smlFac.newSpatialFrame();
            localRefFrame.setId(CRS_ID);
            localRefFrame.setOrigin("Position of Accelerometers (as marked on the plastic box of the device)");
            localRefFrame.addAxis("X", "The X axis is in the plane of the aluminum mounting plate, parallel to the serial connector (as marked on the plastic box of the device)");
            localRefFrame.addAxis("Y", "The Y axis is in the plane of the aluminum mounting plate, orthogonal to the serial connector (as marked on the plastic box of the device)");
            localRefFrame.addAxis("Z", "The Z axis is orthogonal to the aluminum mounting plate, so that the frame is direct (as marked on the plastic box of the device)");
            ((PhysicalSystem)sensorDescription).addLocalReferenceFrame(localRefFrame);
        }
    }


    @Override
    public boolean isConnected()
    {
    	// Check connection to VIRB and get DeviceInfo
    	// request JSON response, parse, and assign values
     	String json = sendCommand("{\"command\":\"deviceInfo\"}");
     	
     	System.out.println (json + "\n");
     	
    	if (json.equalsIgnoreCase("0"))
    		return false;
  		
    	// Returns an array with one component
    	// serialize the DeviceInfo JSON Object
    	Gson gson = new Gson(); 	
      	DeviceInfoArray info = gson.fromJson(json, DeviceInfoArray.class);
    	  		
    	modelNumber = info.deviceInfo[0].model;
    	serialNumber = info.deviceInfo[0].deviceId;
    	firmware = info.deviceInfo[0].firmware;
    	
    	// set GPS on and set Units to metric
    	//String response = 
    	sendCommand("{\"command\":\"updateFeature\",\"feature\": \"gps\" ,\"value\": \"on\" }");
    	//String response = 
    	sendCommand("{\"command\":\"updateFeature\",\"feature\": \"units\" ,\"value\": \"Metric\" }");

    	
        return true;
    }
    
    // send Post command
    public String sendCommand(String command){
    	
    	StringBuffer response = null;
    	
    	try
    	{
    		URL obj = new URL(hostName);
    		HttpURLConnection con = (HttpURLConnection) obj.openConnection();    		
    		con.setRequestMethod("POST");
 
    		con.setDoOutput(true);
    		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
    		wr.writeBytes(command);
    		wr.flush();
    		wr.close();

    		// check response code for an error
    		String responseCode = Integer.toString(con.getResponseCode());
    		if ((responseCode.equalsIgnoreCase("-1")) || (responseCode.equalsIgnoreCase("401")))
    			return "0";
     		
    		BufferedReader in = new BufferedReader(
    		        new InputStreamReader(con.getInputStream()));
    		String inputLine;
    		response = new StringBuffer();

    		while ((inputLine = in.readLine()) != null) {
    			response.append(inputLine);
    		}
    		in.close();
    		     		
    	}
    	catch (IOException e)
    	{  		
    		 e.printStackTrace();
    	}
    	
    	return response.toString();
    }
   
    
   // Class to serialize JSON response
   private class DeviceInfo{
    	
    	String model;
    	String firmware;
    	//String type;
    	//String partNumber;
    	String deviceId;
    	  	
    }
   
   private class DeviceInfoArray{
	   DeviceInfo[] deviceInfo;
   }

    
    @Override
    protected void restartOnDisconnect()
    {
        super.restartOnDisconnect();
    }


    @Override
    public void stop() throws SensorHubException
    {
        if (navDataInterface != null)
            navDataInterface.stop();
        
//        if (videoDataInterface != null)
//        	videoDataInterface.stop();
                          
    }
    

    @Override
    public void cleanup() throws SensorHubException
    {
       
    }
    
    
    private void setAuth()
    {
        ClientAuth.getInstance().setUser(config.net.user);
        if (config.net.password != null)
            ClientAuth.getInstance().setPassword(config.net.password.toCharArray());
    }


    protected String getHostName()
    {
        setAuth();
        return hostName;
    } 
    
 }