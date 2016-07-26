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

package org.sensorhub.impl.sensor.fakeweather;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.fakeweather.FakeWeatherOutput;


/**
 * <p>
 * Driver implementation outputting simulated weather data by randomly
 * increasing or decreasing temperature, pressure, wind speed, and
 * wind direction.  Serves as a simple sensor to deploy as well as
 * a simple example of a sensor driver.
 * </p>
 *
 * @author Mike Botts <mike.botts@botts-inc.com>
 * @since Dec 24, 2014
 */
public class FakeWeatherSensor extends AbstractSensorModule<FakeWeatherConfig>
{
    private static final String UID_PREFIX = "urn:osh:sensor:simweather:";
    
    FakeWeatherOutput dataInterface;
    
    
    public FakeWeatherSensor()
    {        
    }
    
    
    @Override
    public void init() throws SensorHubException
    {
        super.init();
        
        // generate identifiers
        if (config.serialNumber != null)
        {
            this.uniqueID = UID_PREFIX + config.serialNumber;
            this.xmlID = "WEATHER_STATION_" + config.serialNumber.toUpperCase();
        }
        
        // init main data interface
        dataInterface = new FakeWeatherOutput(this);
        addOutput(dataInterface, false);
        dataInterface.init();
    }


    @Override
    protected void updateSensorDescription()
    {
        synchronized (sensorDescription)
        {
            super.updateSensorDescription();
            
            if (!sensorDescription.isSetDescription())
                sensorDescription.setDescription("Simulated weather station generating realistic pseudo-random measurements");
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

