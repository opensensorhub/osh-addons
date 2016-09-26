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

package org.sensorhub.impl.sensor.foscam;

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
import org.sensorhub.impl.comm.RobustIPConnection;
import org.sensorhub.impl.module.RobustConnection;
import org.sensorhub.impl.security.ClientAuth;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.vast.sensorML.SMLFactory;
import org.vast.swe.SWEHelper;


/**
 * <p>
 * Implementation of sensor interface for Foscam Cameras using IP
 * protocol Based on Foscam v1.0.4 API.
 * </p>
 *
 * @author Lee Butler <labutler10@gmail.com>
 * @since September 2016
 */
public class FoscamDriver extends AbstractSensorModule<FoscamConfig>
{
    RobustConnection connection;
    FoscamVideoOutput<FoscamDriver> videoDataInterface;
    FoscamVideoControl videoControlInterface;
    FoscamPtzControl ptzControlInterface;
    
    boolean ptzSupported = false;
    String serialNumber;
    String modelNumber;


    public FoscamDriver()
    {
    }
    
    
    @Override
    public void init() throws SensorHubException
    {
    	System.out.println("In Driver init()...");
        // reset internal state in case init() was already called
        super.init();
        videoDataInterface = null;
        ptzControlInterface = null;
        ptzSupported = false;

        // create connection handler
        this.connection = new RobustIPConnection(this, config.connection, "Foscam Camera")
        {
            public boolean tryConnect() throws Exception
            {
            	// just check host is reachable on specified RTSP port
                return tryConnectTCP(config.rtsp.remoteHost, config.rtsp.remotePort);
            }
        };
        
        // create video output
        this.videoDataInterface = new FoscamVideoOutput<FoscamDriver>(this);
        this.videoDataInterface.init(config.video.frameWidth, config.video.frameHeight);
        addOutput(videoDataInterface, false);

        // PTZ control and status
        try
        {
			createPtzInterfaces();
		}
        catch (IOException e)
        {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    @Override
    public synchronized void start() throws SensorHubException
    {
        // wait for valid connection to camera
        connection.waitForConnection();
        
        // start video stream
        videoDataInterface.start(config.video, config.rtsp, config.connection.connectTimeout);
    }
    
    
    @Override
    public synchronized void stop()
    {
    	if (connection != null)
            connection.cancel();
        
    	if (ptzControlInterface != null)
        	ptzControlInterface.stop();
        
    	if (videoDataInterface != null)
        	videoDataInterface.stop();
        
    	if (videoControlInterface != null)
       		videoControlInterface.stop();
    }
    
    
    @Override
    protected void updateSensorDescription()
    {
        synchronized (sensorDescLock)
        {
            super.updateSensorDescription();
            if (!sensorDescription.isSetDescription())
                sensorDescription.setDescription("Foscam Network Camera");
        }
    }
    
    
    @Override
    public boolean isConnected()
    {
        if (videoDataInterface != null)
            return videoDataInterface.firstFrameReceived;
        
        return false;
    }
    
    
    protected void createPtzInterfaces() throws SensorException, IOException
    {
        // connect to PTZ URL
        HttpURLConnection conn = null;
        try
        {
            URL optionsURL = new URL("http://" + config.http.remoteHost + 
            		":" + Integer.toString(config.http.remotePort) + 
            		"/cgi-bin/CGIProxy.fcgi?cmd=getDevInfo&usr=" + config.http.user + 
            		"&pwd=" + config.http.password);
            conn = (HttpURLConnection)optionsURL.openConnection();
            conn.setConnectTimeout(config.connection.connectTimeout);
            conn.setReadTimeout(config.connection.connectTimeout);
            conn.connect();
        }
        catch (IOException e)
        {
            throw new SensorException("Cannot connect to camera PTZ service", e);
        }

      //add PTZ controller
      this.ptzControlInterface = new FoscamPtzControl(this);
      addControlInput(ptzControlInterface);
      ptzControlInterface.init();
    }
    

    @Override
    public void cleanup()
    {
    }
}
