/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.fakeweather;

import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.vast.ogc.gml.IGeoFeature;


public class FakeWeatherNetwork extends AbstractSensorModule<FakeWeatherNetworkConfig>
{
    FakeWeatherNetworkOutput dataInterface;
    Map<String, FakeWeatherStation> stations = new TreeMap<>();
    
    
    public FakeWeatherNetwork()
    {
    }
    
    
    @Override
    protected void doInit() throws SensorHubException
    {
        super.doInit();
        
        // generate identifiers
        generateUniqueID("urn:osh:sensor:simweathernetwork:", config.networkID);
        generateXmlID("WEATHER_NETWORK_", config.networkID);
        
        // init main data interface
        dataInterface = new FakeWeatherNetworkOutput(this);
        addOutput(dataInterface, false);
        dataInterface.init();
        
        // generate station fois
        var rand = new Random(config.id.hashCode());
        for (int i = 1; i <= config.numStations; i++)
        {
            var lat = (rand.nextDouble() - 0.5) * config.areaSize + config.centerLocation.lat;
            var lon = (rand.nextDouble() - 0.5) * config.areaSize + config.centerLocation.lon;
            var station = new FakeWeatherStation(i, lat, lon);
            stations.put(station.uid, station);
        }
    }


    @Override
    public Map<String, ? extends IGeoFeature> getCurrentFeaturesOfInterest()
    {
        return stations;
    }
    
    
    @Override
    protected void updateSensorDescription()
    {
        synchronized (sensorDescLock)
        {
            super.updateSensorDescription();
            sensorDescription.setDescription(FakeWeatherNetworkDescriptor.DRIVER_DESC);
        }
    }


    @Override
    protected void doStart() throws SensorHubException
    {
        if (dataInterface != null)
            dataInterface.start();        
    }
    

    @Override
    protected void doStop() throws SensorHubException
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
