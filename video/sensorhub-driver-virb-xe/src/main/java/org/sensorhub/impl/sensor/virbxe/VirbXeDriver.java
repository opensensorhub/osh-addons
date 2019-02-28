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
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import net.opengis.sensorml.v20.IdentifierList;
import net.opengis.sensorml.v20.Term;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.comm.RobustHTTPConnection;
import org.sensorhub.impl.security.ClientAuth;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.rtpcam.RTSPClient;
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
        
    RobustHTTPConnection connection;
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
    public void setConfiguration(final VirbXeConfig config)
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
        // reset internal state in case init() was already called
        super.init();
        videoDataInterface = null;
        navDataInterface = null;
        healthDataInterface = null;
        
        // create connection handler
        this.connection = new RobustHTTPConnection(this, config.connection, "VIRB Camera")
        {
            public boolean tryConnect() throws IOException
            {
                // check connection to HTTP server
                String json = sendCommand("{\"command\":\"deviceInfo\"}");
                if (json == null)
                    return false;
                getLogger().trace(json);
                
                // parse JSON response to get device info
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
                
                // check that we actually read a serial number
                if (serialNumber == null || serialNumber.trim().isEmpty())
                    throw new IOException("Cannot read camera serial number");
                
                // also check connection to RTSP server
                try
                {
                    RTSPClient rtspClient = new RTSPClient(
                            config.rtsp.remoteHost,
                            config.rtsp.remotePort,
                            config.rtsp.videoPath,
                            config.rtsp.user,
                            config.rtsp.password,
                            config.rtsp.localUdpPort,
                            config.connection.connectTimeout);
                    rtspClient.sendOptions();
                    rtspClient.sendDescribe();
                }
                catch (IOException e)
                {
                    reportError("Cannot connect to RTSP server", e, true);
                    return false;
                }
                
                return true;
            }
        };
        
        // TODO we could check if camera info is in cache, in which case
        // it's not necessary to connect to camera at this point
        
        // wait for valid connection to camera
        connection.waitForConnection();
        
        // generate identifiers
        generateUniqueID("urn:garmin:cam:", serialNumber);
        generateXmlID("GARMIN_VIRB_XE_", serialNumber);
        
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
        synchronized (sensorDescLock)
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
            term.setValue("Garmin " + modelNumber + " Video Camera #" + serialNumber);
            ident.addIdentifier2(term);
        
            // Short Name
            term = smlFac.newTerm();
            term.setDefinition(SWEHelper.getPropertyUri("ShortName"));
            term.setLabel("Short Name");
            term.setValue("Garmin " + modelNumber);
            ident.addIdentifier2(term);
        }
    }


    @Override
    public boolean isConnected()
    {
        return connection.isConnected();
    }
    
    
    // send POST command
    public String sendCommand(String command) throws IOException
    {
    	// send POST data to URL
        HttpURLConnection conn = connection.tryConnectPOST(getHostUrl(), command);
        if (conn == null)
            return null; // case where error reported but no exception
        
        // read response
        BufferedReader reader = null;
        try
        {    
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuffer response = new StringBuffer();
            String inputLine;            
            while ((inputLine = reader.readLine()) != null)
                response.append(inputLine);
            return response.toString();
        }
        catch (IOException e)
        {
            throw new IOException("Cannot read server response", e);
        }
        finally
        {
            if (reader != null)
                reader.close();
        }
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
        if (connection != null)
            connection.cancel();
        
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