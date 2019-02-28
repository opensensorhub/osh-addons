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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import net.opengis.sensorml.v20.IdentifierList;
import net.opengis.sensorml.v20.Term;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.comm.RobustHTTPConnection;
import org.sensorhub.impl.module.RobustConnection;
import org.sensorhub.impl.security.ClientAuth;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.rtpcam.RTSPClient;
import org.vast.sensorML.SMLFactory;
import org.vast.swe.SWEHelper;


/**
 * <p>
 * Implementation of sensor interface for generic Dahua Cameras using IP
 * protocol Based on Dahua v1.23 API.
 * </p>
 *
  * @author Mike Botts <mike.botts@botts-inc.com>
 * @since March 2016
 */
public class DahuaCameraDriver extends AbstractSensorModule<DahuaCameraConfig>
{
    RobustConnection connection;
    DahuaVideoOutput videoDataInterface;
    DahuaPtzOutput ptzDataInterface;
    DahuaVideoControl videoControlInterface;
    DahuaPtzControl ptzControlInterface;
    
    boolean ptzSupported = false;
    String hostUrl;
    String serialNumber;
    String modelNumber;


    public DahuaCameraDriver()
    {
    }
    
    
    @Override
    public void setConfiguration(DahuaCameraConfig config)
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
        hostUrl = "http://" + config.http.remoteHost + ":" + config.http.remotePort + "/cgi-bin";
    };
    
    
    @Override
    public void init() throws SensorHubException
    {
        // reset internal state in case init() was already called
        super.init();
        videoDataInterface = null;
        ptzDataInterface = null;
        ptzControlInterface = null;
        ptzSupported = false;
        
        // create connection handler
        connection = new RobustHTTPConnection(this, config.connection, "Dahua Camera")
        {
            public boolean tryConnect() throws IOException
            {
                // check we can reach the HTTP server
                // and access the system info URL
                HttpURLConnection conn = tryConnectGET(getHostUrl() + "/magicBox.cgi?action=getSystemInfo");
                if (conn == null)
                    return false;
                
                // read response
                BufferedReader reader = null;
                try
                {
                    InputStream is = conn.getInputStream();
                    reader = new BufferedReader(new InputStreamReader(is));
                    
                    // note: should return three lines with serialNumber, deviceType (model number), and hardwareVersion
                    String line;
                    while ((line = reader.readLine()) != null)
                    {
                        String[] tokens = line.split("=");  
                        if (tokens[0].trim().equalsIgnoreCase("serialNumber"))
                            serialNumber = tokens[1];
                        else if (tokens[0].trim().equalsIgnoreCase("deviceType"))
                            modelNumber = tokens[1];
                    }
                }
                catch (IOException e)
                {
                    reportError("Cannot read camera information", e, true);
                    return false;
                }
                finally
                {
                    if (reader != null)
                        reader.close();
                }         
                
                // check that we actually read a serial number
                if (serialNumber == null || serialNumber.trim().isEmpty())
                    throw new IOException("Cannot read camera serial number");
                
                // check connection to RTSP server
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
                }
                catch (IOException e)
                {
                    reportError("Cannot connect to RTSP server", e, true);
                    return false;
                }                
                
                return true;                
            }
        };
        
        // TODO we could check if basic metadata is in cache, in which case
        // it's not necessary to connect to camera at this point
        
        // wait for valid connection to camera
        connection.waitForConnection();
        
        // generate identifiers
        generateUniqueID("urn:dahua:cam:", serialNumber);
        generateXmlID("DAHUA_CAM_", serialNumber);
        
        // create I/O objects
        // video output
        videoDataInterface = new DahuaVideoOutput(this);
        videoDataInterface.init();
        addOutput(videoDataInterface, false);
                
        // video control
        //this.videoControlInterface = new DahuaVideoControl(this);
        //videoControlInterface.init();
        //addControlInput(videoControlInterface);
                    
        // PTZ control and status
        createPtzInterfaces();
    }
    
    
    protected void createPtzInterfaces() throws SensorException
    {
        // connect to PTZ URL
        HttpURLConnection conn = null;
        try
        {
            URL optionsURL = new URL(getHostUrl() + "/ptz.cgi?action=getCurrentProtocolCaps&channel=0");
            conn = (HttpURLConnection)optionsURL.openConnection();
            conn.setConnectTimeout(config.connection.connectTimeout);
            conn.setReadTimeout(config.connection.connectTimeout);
            conn.connect();
        }
        catch (IOException e)
        {
            throw new SensorException("Cannot connect to camera PTZ service", e);
        }
        
        // parse response
        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null)
            {
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
        catch (IOException e)
        {
            throw new SensorException("Cannot read PTZ metadata", e);
        }
        finally
        {
            try
            {
                if (reader != null)
                    reader.close();
            }
            catch (IOException e)
            {
            }
        }
    }
    
    
    @Override
    public void start() throws SensorHubException
    {
        // wait for valid connection to camera
        connection.waitForConnection();
            
        // start video output
        videoDataInterface.start();
        
        // if PTZ supported
        if (ptzSupported)
        {
            ptzDataInterface.start();
            ptzControlInterface.start();
        }
    }
    

	@Override
    protected void updateSensorDescription()
    {
        synchronized (sensorDescLock)
        {
            // parent class reads SensorML from config if provided
            // and then sets unique ID, outputs and control inputs
            super.updateSensorDescription();
            
            // set identifiers in SensorML
            SMLFactory smlFac = new SMLFactory();            

            if (!sensorDescription.isSetDescription())
                sensorDescription.setDescription("Dahua Video Camera " + modelNumber);
          
            IdentifierList identifierList = smlFac.newIdentifierList();
            sensorDescription.addIdentification(identifierList);
            
            Term term;            
            term = smlFac.newTerm();
            term.setDefinition(SWEHelper.getPropertyUri("Manufacturer"));
            term.setLabel("Manufacturer Name");
            term.setValue("Dahua");
            identifierList.addIdentifier2(term);
            
            if (modelNumber != null)
            {
                term = smlFac.newTerm();
                term.setDefinition(SWEHelper.getPropertyUri("ModelNumber"));
                term.setLabel("Model Number");
                term.setValue(modelNumber);
                identifierList.addIdentifier2(term);
            }
            
            if (serialNumber != null)
            {
                term = smlFac.newTerm();
                term.setDefinition(SWEHelper.getPropertyUri("SerialNumber"));
                term.setLabel("Serial Number");
                term.setValue(serialNumber);
                identifierList.addIdentifier2(term);
            }
            
            // Long Name
            term = smlFac.newTerm();
            term.setDefinition(SWEHelper.getPropertyUri("LongName"));
            term.setLabel("Long Name");
            term.setValue("Dahua " + modelNumber + " Video Camera #" + serialNumber);
            identifierList.addIdentifier2(term);

            // Short Name
            term = smlFac.newTerm();
            term.setDefinition(SWEHelper.getPropertyUri("ShortName"));
            term.setLabel("Short Name");
            term.setValue("Dahua Cam " + modelNumber);
            identifierList.addIdentifier2(term);
        }
    }


    @Override
    public boolean isConnected()
    {
    	if (connection == null)
    	    return false;
    	
        return connection.isConnected();
    }

    
    @Override
    public void stop()
    {
        if (connection != null)
            connection.cancel();
        
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
