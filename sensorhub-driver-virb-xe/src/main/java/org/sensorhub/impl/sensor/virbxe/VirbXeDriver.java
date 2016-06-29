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
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import net.opengis.sensorml.v20.IdentifierList;
import net.opengis.sensorml.v20.PhysicalSystem;
import net.opengis.sensorml.v20.SpatialFrame;
import net.opengis.sensorml.v20.Term;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.comm.RobustIPConnection;
import org.sensorhub.impl.module.RobustConnection;
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
        
    RobustConnection connection;
    VirbXeNavOutput navDataInterface;
    VirbXeAntOutput healthDataInterface;
    VirbXeVideoOutput videoDataInterface;
	
    String hostUrl;    
    String serialNumber;
    String firmware;
    String modelNumber;
    
    
    public VirbXeDriver()
    {       
    }
    
    
    @Override
    public void setConfiguration(VirbXeConfig config)
    {
        super.setConfiguration(config);
        
        // use same config for HTTP and RTSP by default
        if (config.rtsp.localAddress == null)
            config.rtsp.localAddress = config.http.localAddress;
        if (config.rtsp.remoteHost == null)
            config.rtsp.remoteHost = config.http.remoteHost;
        if (config.rtsp.user == null)
            config.rtsp.user = config.http.user;
        if (config.rtsp.password == null)
            config.rtsp.password = config.http.password;
                    
        // compute full host URL
        hostUrl = "http://" + config.http.remoteHost + ":" + config.http.remotePort + "/virb";
    };
    
    
    @Override
    public void init() throws SensorHubException
    {
        // create connection handler
        this.connection = new RobustIPConnection(this, config.connection, "VIRB Camera")
        {
            public boolean tryConnect() throws Exception
            {
                if (!tryConnect(config.http.remoteHost, config.http.remotePort))
                    return false;
                
                // check connection to VIRB and get device info
                String json;
                try
                {
                    json = sendCommand("{\"command\":\"deviceInfo\"}");
                    getLogger().trace(json);
                }
                catch (IOException e)
                {
                    reportError("Cannot connect to camera HTTP server", e, true);
                    return false;
                }                
                
                try
                {
                    // deserialize the DeviceInfoArray JSON Object
                    Gson gson = new Gson();     
                    DeviceInfoArray info = gson.fromJson(json, DeviceInfoArray.class);
                            
                    modelNumber = info.deviceInfo[0].model;
                    serialNumber = info.deviceInfo[0].deviceId;
                    firmware = info.deviceInfo[0].firmware;
                }
                catch (Exception e)
                {
                    throw new IOException("Cannot parse JSON response", e);
                }
                
                return true;
            }
        };
        
        // TODO we could check if camera info is in cache, in which case
        // it's not necessary to connect to camera at this point
        
        // wait for valid connection to camera
        connection.waitForConnection();
        
        // generate identifiers
        this.uniqueID = "urn:garmin:cam:" + serialNumber;
        this.xmlID = "GARMIN_VIRB_XE_" + serialNumber.toUpperCase();
        
        // create I/O objects
        // video output
        videoDataInterface = new VirbXeVideoOutput(this);
        videoDataInterface.init();
        addOutput(videoDataInterface, false);

        // navigation output
        navDataInterface = new VirbXeNavOutput(this);
        navDataInterface.init();
        addOutput(navDataInterface, false);
        
        // health data output
        healthDataInterface = new VirbXeAntOutput(this);
        healthDataInterface.init();
        if (healthDataInterface.hasSensors())
            addOutput(healthDataInterface, false);
        else
            healthDataInterface = null; // set to null if no sensors are connected
    }
    
    
    @Override
    public void start() throws SensorHubException
    {
        // wait for valid connection to camera
        connection.waitForConnection();
            
        // enable GPS on and set units to metric
        try
        {
            sendCommand("{\"command\":\"updateFeature\",\"feature\": \"gps\" ,\"value\": \"on\" }");
            sendCommand("{\"command\":\"updateFeature\",\"feature\": \"units\" ,\"value\": \"Metric\" }");
        }
        catch (IOException e)
        {
            throw new SensorException("Cannot send startup commands", e);
        }
        
        // start output threads
        videoDataInterface.start();
        navDataInterface.start();            
        if (healthDataInterface != null)
        	healthDataInterface.start();
    }


    @Override
    protected void updateSensorDescription()
    {
        synchronized (sensorDescription)
        {
            super.updateSensorDescription();
            
            SMLFactory smlFac = new SMLFactory();
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
            term.setValue("Garmin " + modelNumber + " Video Camera");
            ident.addIdentifier2(term);
        
            // Short Name
            term = smlFac.newTerm();
            term.setDefinition(SWEHelper.getPropertyUri("ShortName"));
            term.setLabel("Short Name");
            term.setValue("Garmin " + modelNumber);
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
        return connection.isConnected();
    }
    
    
    // send Post command
    public String sendCommand(String command) throws IOException
    {
    	URL obj = new URL(getHostUrl());
        HttpURLConnection conn = (HttpURLConnection) obj.openConnection();           
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(config.connection.connectTimeout);
        conn.setReadTimeout(config.connection.connectTimeout);
        conn.setDoOutput(true);
        conn.connect();
        
        // check response code for an error
        if (conn.getResponseCode() > 202)
            throw new IOException("Received HTTP error code " + conn.getResponseCode());

        // read JSON response
        StringBuffer response = null;
        try
        {
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
            out.write(command);
            out.flush();
            out.close();
            
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            response = new StringBuffer();
            String inputLine;            
            while ((inputLine = in.readLine()) != null)
                response.append(inputLine);
        }
        catch (IOException e)
        {
            throw new IOException("Cannot read server response", e);
        }
        finally
        {
            conn.disconnect();
        }
    	
    	return response.toString();
    }
   
    
    // Class to serialize JSON response
    static private class DeviceInfo
    {    	
    	String model;
    	String firmware;
    	//String type;
    	//String partNumber;
    	String deviceId;    	  	
    }
   
    static private class DeviceInfoArray
    {
	   DeviceInfo[] deviceInfo;
    }


    @Override
    public void stop() throws SensorHubException
    {
        if (videoDataInterface != null)
            videoDataInterface.stop();
        
        if (navDataInterface != null)
            navDataInterface.stop();
        
        if (healthDataInterface != null)
            healthDataInterface.stop();                    
    }
    

    @Override
    public void cleanup() throws SensorHubException
    {
       
    }
    
    
    private void setAuth()
    {
        ClientAuth.getInstance().setUser(config.http.user);
        if (config.http.password != null)
            ClientAuth.getInstance().setPassword(config.http.password.toCharArray());
    }


    protected String getHostUrl()
    {
        setAuth();
        return hostUrl;
    } 
    
 }