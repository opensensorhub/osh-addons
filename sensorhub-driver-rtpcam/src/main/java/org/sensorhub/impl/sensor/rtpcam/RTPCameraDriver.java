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
import org.sensorhub.api.sensor.SensorException;
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
    RTPVideoOutput<RTPCameraDriver> dataInterface;
    
    
    public RTPCameraDriver()
    {
    }
    
    
    @Override
    public void init(RTPCameraConfig config) throws SensorHubException
    {
        super.init(config);        
        this.dataInterface = new RTPVideoOutput<RTPCameraDriver>(this, config.video, config.net, config.rtsp);
        this.dataInterface.init();
        addOutput(dataInterface, false);
    }
    
    
    @Override
    public synchronized void start() throws SensorException
    {
        dataInterface.updateConfig(config.video, config.net, config.rtsp);
        dataInterface.start();
    }
    
    
    @Override
    public synchronized void stop()
    {
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
