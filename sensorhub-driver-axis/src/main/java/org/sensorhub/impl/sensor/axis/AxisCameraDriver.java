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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import net.opengis.sensorml.v20.IdentifierList;
import net.opengis.sensorml.v20.Term;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.comm.RobustHTTPConnection;
import org.sensorhub.impl.module.RobustConnection;
import org.sensorhub.impl.security.ClientAuth;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.rtpcam.RTPVideoOutput;
import org.sensorhub.impl.sensor.rtpcam.RTSPClient;
import org.vast.sensorML.SMLFactory;
import org.vast.swe.SWEHelper;


/**
 * <p>
 * Implementation of sensor interface for generic Axis Cameras using IP
 * protocol
 * </p>
 * 
 * @author Mike Botts <mike.botts@botts-inc.com>
 * @since October 30, 2014
 */
public class AxisCameraDriver extends AbstractSensorModule<AxisCameraConfig>
{
	RobustConnection connection;
    AxisVideoOutput mjpegVideoOutput;
	RTPVideoOutput<AxisCameraDriver> h264VideoOutput;
    AxisPtzOutput ptzPosOutput;
    AxisVideoControl videoControlInterface;
    AxisPtzControl ptzControlInterface;
    
    String hostUrl;
    String serialNumber;
    String modelNumber;
    String longName;
    String shortName;

    boolean ptzSupported = false;


    public AxisCameraDriver()
    {	
    }
    
    
    @Override
    public void setConfiguration(final AxisCameraConfig config)
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
        hostUrl = "http://" + config.http.remoteHost + ":" + config.http.remotePort + "/axis-cgi";
    };
    
    
    @Override
    public void init() throws SensorHubException
    {
        // reset internal state in case init() was already called
        super.init();
        mjpegVideoOutput = null;
        h264VideoOutput = null;
        ptzPosOutput = null;
        ptzControlInterface = null;
        ptzSupported = false;
        
        // create connection handler
        connection = new RobustHTTPConnection(this, config.connection, "Axis Camera")
        {
            public boolean tryConnect() throws Exception
            {
                // check we can reach the HTTP server
                // and access the param URL
                HttpURLConnection conn = tryConnectGET(getHostUrl() + "/view/param.cgi?action=list");
                if (conn == null)
                    return false;
                
                // parse response to extract identifiers
                BufferedReader reader = null;
                try
                {
                    InputStream is = conn.getInputStream();
                    reader = new BufferedReader(new InputStreamReader(is));
                    String line;
                    while ((line = reader.readLine()) != null)
                    {
                        String[] tokens = line.split("=");
                        
                        if ((tokens[0].trim().equalsIgnoreCase("root.Brand.Brand")) && (tokens[1].trim().equalsIgnoreCase("AXIS")))
                            connected = true;                        
                        else if (tokens[0].trim().equalsIgnoreCase("root.Properties.PTZ.PTZ"))
                        {
                            if (tokens[1].trim().equalsIgnoreCase("yes"))
                                ptzSupported = true;
                            else
                                ptzSupported = false;
                        }
                        else if (tokens[0].trim().equalsIgnoreCase("root.Brand.ProdFullName"))
                            longName = tokens[1];
                        else if (tokens[0].trim().equalsIgnoreCase("root.Brand.ProdShortName"))
                            shortName = tokens[1];
                        else if (tokens[0].trim().equalsIgnoreCase("root.Brand.ProdNbr"))
                            modelNumber = tokens[1];
                        else if (tokens[0].trim().equalsIgnoreCase("root.Properties.System.SerialNumber"))
                            serialNumber = tokens[1];
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
                if (config.enableH264)
                {
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
                }
                
                return true;                
            }
        };
        
        // TODO we could check if basic metadata is in cache, in which case
        // it's not necessary to connect to camera at this point
        
        // wait for valid connection to camera
        connection.waitForConnection();
        
        // generate identifiers
        generateUniqueID("urn:axis:cam:", serialNumber);
        generateXmlID("AXIS_CAM_", serialNumber);
        
        // create I/O objects
        String videoOutName = "video";
        int videoOutNum = 1;
        
        // add MJPEG video output
        if (config.enableMJPEG)
        {
            String outputName = videoOutName + videoOutNum++;
            mjpegVideoOutput = new AxisVideoOutput(this, outputName);
            mjpegVideoOutput.init();
            addOutput(mjpegVideoOutput, false);
        }
        
        // add H264 video output
        if (config.enableH264)
        {
            String outputName = videoOutName + videoOutNum++;
            h264VideoOutput = new RTPVideoOutput<AxisCameraDriver>(this, outputName);
            h264VideoOutput.init(config.video.resolution.getWidth(), config.video.resolution.getHeight());
            addOutput(h264VideoOutput, false);
        }
        
        // add video settings controller
        //this.videoControlInterface = new AxisVideoControl(this);
        //addControlInput(videoControlInterface);           
        //videoControlInterface.init();         
        
        if (ptzSupported)
        {
            // add PTZ output
            ptzPosOutput = new AxisPtzOutput(this);
            addOutput(ptzPosOutput, false);
            ptzPosOutput.init();
            
            // add PTZ controller
            ptzControlInterface = new AxisPtzControl(this);
            addControlInput(ptzControlInterface);
            ptzControlInterface.init();                 
        }
    }
    
    
    @Override
    public void start() throws SensorHubException
    {
        // wait for valid connection to camera
        connection.waitForConnection();
            
        // start video output
        if (mjpegVideoOutput != null)
            mjpegVideoOutput.start();
        
        if (h264VideoOutput != null)
            h264VideoOutput.start(config.video, config.rtsp, config.connection.connectTimeout);
        
        // if PTZ supported
        if (ptzSupported)
        {
            ptzPosOutput.start();
            ptzControlInterface.start();
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
                        
            SMLFactory smlFac = new SMLFactory();            

            if (!sensorDescription.isSetDescription())
                sensorDescription.setDescription("Axis Video Camera");
          
            IdentifierList identifierList = smlFac.newIdentifierList();
            sensorDescription.addIdentification(identifierList);
            
            Term term;            
            term = smlFac.newTerm();
            term.setDefinition(SWEHelper.getPropertyUri("Manufacturer"));
            term.setLabel("Manufacturer Name");
            term.setValue("Axis");
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
            
            if (longName != null)
            {
                term = smlFac.newTerm();
                term.setDefinition(SWEHelper.getPropertyUri("LongName"));
                term.setLabel("Long Name");
                term.setValue(longName);
                identifierList.addIdentifier2(term);
            }
            
            if (shortName != null)
            {
                term = smlFac.newTerm();
                term.setDefinition(SWEHelper.getPropertyUri("ShortName"));
                term.setLabel("Short Name");
                term.setValue(shortName);
                identifierList.addIdentifier2(term);
            }
        }
    }


    @Override
    public boolean isConnected()
    {
        if (connection == null)
            return false;
        
        return connection.isConnected();
    }
    

    protected void setAuth()
    {
        ClientAuth.getInstance().setUser(config.http.user);
        if (config.http.password != null)
            ClientAuth.getInstance().setPassword(config.http.password.toCharArray());
    }


    @Override
    public void stop()
    {
        if (connection != null)
            connection.cancel();
        
        if (ptzPosOutput != null)
        	ptzPosOutput.stop();
        
        if (ptzControlInterface != null)
        	ptzControlInterface.stop();
        
       if (mjpegVideoOutput != null)
        	mjpegVideoOutput.stop();
       
       if (h264VideoOutput != null)
           h264VideoOutput.stop();
        
       if (videoControlInterface != null)
       		videoControlInterface.stop();
    }


    @Override
    public void cleanup()
    {
    }
    
    
    protected String getHostUrl()
    {
        setAuth();
        return hostUrl;
    }
}
