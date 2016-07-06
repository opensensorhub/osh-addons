/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.rtpcam;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.comm.RobustIPConnection;
import org.sensorhub.impl.module.RobustConnection;
import org.sensorhub.impl.sensor.AbstractSensorModule;


/**
 * <p>
 * Generic driver implementation for RTP/RTSP cameras.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Dec 12, 2015
 */
public class RTPCameraDriver extends AbstractSensorModule<RTPCameraConfig>
{
    RobustConnection connection;
    RTPVideoOutput<RTPCameraDriver> dataInterface;
    
    
    public RTPCameraDriver()
    {
    }
    
    
    @Override
    public void init() throws SensorHubException
    {
        // reset internal state in case init() was already called
        super.init();
        dataInterface = null;
        
        // create connection handler
        this.connection = new RobustIPConnection(this, config.connection, "RTP Camera")
        {
            public boolean tryConnect() throws Exception
            {
                // just check host is reachable on specified RTSP port
                return tryConnectTCP(config.rtsp.remoteHost, config.rtsp.remotePort);
            }
        };
        
        // generate identifiers
        if (config.cameraID != null)
        {
            this.uniqueID = "urn:osh:sensor:rtpcam:" + config.cameraID;
            this.xmlID = "RTP_CAM_" + config.cameraID;
        }
        
        // create video output
        this.dataInterface = new RTPVideoOutput<RTPCameraDriver>(this);
        this.dataInterface.init(config.video.frameWidth, config.video.frameHeight);
        addOutput(dataInterface, false);
    }
    
    
    @Override
    public synchronized void start() throws SensorHubException
    {
        // wait for valid connection to camera
        connection.waitForConnection();
        
        // start video stream
        dataInterface.start(config.video, config.rtsp);
    }
    
    
    @Override
    public synchronized void stop()
    {
        if (connection != null)
            connection.cancel();
        
        if (dataInterface != null)
            dataInterface.stop();
    }
    
    
    @Override
    protected void updateSensorDescription()
    {
        synchronized (sensorDescription)
        {
            super.updateSensorDescription();
            
            if (AbstractSensorModule.DEFAULT_ID.equals(sensorDescription.getId()))
                sensorDescription.setId("RTP_CAM_" + config.cameraID);
            
            if (config.cameraID != null)
                sensorDescription.setUniqueIdentifier("urn:osh:sensor:rtpcam:" + config.cameraID);
        }
    }


    @Override
    public boolean isConnected()
    {
        if (dataInterface != null)
            return dataInterface.firstFrameReceived;
        
        return false;
    }
    

    @Override
    public void cleanup()
    {        
    }
}
