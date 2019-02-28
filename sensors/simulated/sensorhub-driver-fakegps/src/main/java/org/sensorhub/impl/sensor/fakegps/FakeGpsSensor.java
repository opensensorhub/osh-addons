/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.fakegps;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;


/**
 * <p>
 * Driver implementation outputing simulated GPS data after
 * requesting trajectories from Google Directions.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Nov 2, 2014
 */
public class FakeGpsSensor extends AbstractSensorModule<FakeGpsConfig>
{
    FakeGpsOutput dataInterface;
    
    
    public FakeGpsSensor()
    {
    }
    
    
    @Override
    public void init() throws SensorHubException
    {
        super.init();
        
        if (config.googleApiKey == null || config.googleApiKey.isEmpty())
            throw new SensorHubException("A Google API key with access to the Directions API must be provided in the configuration");
        
        // generate IDs
        generateUniqueID("urn:osh:sensor:simgps:", null);
        generateXmlID("GPS_SENSOR_", null);
        
        // init main data interface
        dataInterface = new FakeGpsOutput(this);
        addOutput(dataInterface, false);
        dataInterface.init();
    }
    
    
    @Override
    protected void updateSensorDescription()
    {
        synchronized (sensorDescLock)
        {
            super.updateSensorDescription();
            sensorDescription.setDescription("Simulated GPS sensor generating data along random itineraries obtained using Google Direction API");
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
        if (dataInterface != null)
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
