/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.fakecam;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;


/**
 * <p>
 * Driver implementation outputing live video data obtained by reading a
 * static video file in loop.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Jan 10, 2015
 */
public class FakeCamSensor extends AbstractSensorModule<FakeCamConfig>
{
    FakeCamOutput dataInterface;
    
    
    public FakeCamSensor()
    {
        
    }
    
    
    @Override
    public void init() throws SensorHubException
    {
        super.init();
        
        // generate IDs
        generateUniqueID("urn:osh:sensor:simcam:", config.cameraID);
        generateXmlID("VIDEO_CAM_", config.cameraID);       
        
        // create output
        dataInterface = new FakeCamOutput(this);
        addOutput(dataInterface, false);
        dataInterface.init();
    }
    
    
    @Override
    protected void updateSensorDescription()
    {
        synchronized (sensorDescLock)
        {
            super.updateSensorDescription();
            sensorDescription.setDescription("Fake camera sensor generating data read from a static video file");
        }
    }


    @Override
    public void start() throws SensorHubException
    {
        dataInterface.start();        
    }
    

    @Override
    public void stop() throws SensorHubException
    {
        dataInterface.stop();
    }
    

    @Override
    public void cleanup() throws SensorHubException
    {
       
    }
    
    
    @Override
    public boolean isConnected()
    {
        return true;
    }
}
