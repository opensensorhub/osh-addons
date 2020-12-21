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

import java.util.Map;
import java.util.TreeMap;
import org.sensorhub.api.common.SensorHubException;
import org.vast.ogc.gml.IGeoFeature;


/**
 * <p>
 * Driver implementation generating simulated GPS trajectories for multiple
 * vehicles in a given area.
 * </p>
 *
 * @author Alex Robin
 * @since Dec 19, 2020
 */
public class FakeGpsNetwork extends FakeGpsSensor
{
    Map<String, IGeoFeature> fois = new TreeMap<>();
    
    
    public FakeGpsNetwork()
    {
    }
    
    
    @Override
    public void init() throws SensorHubException
    {
        super.init();
        
        // create FOIs
        for (var route: dataInterface.routes)
            for (var asset: route.assets)
                fois.put(asset.getUniqueIdentifier(), asset);
    }


    @Override
    public Map<String, ? extends IGeoFeature> getCurrentFeaturesOfInterest()
    {
        return fois;
    }
    
    
    @Override
    protected void updateSensorDescription()
    {
        synchronized (sensorDescLock)
        {
            super.updateSensorDescription();
            sensorDescription.setDescription(FakeGpsNetworkDescriptor.DRIVER_DESC);
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
