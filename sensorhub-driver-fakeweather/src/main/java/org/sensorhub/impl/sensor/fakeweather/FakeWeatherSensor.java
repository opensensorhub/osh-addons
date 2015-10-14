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

import net.opengis.gml.v32.AbstractFeature;
import net.opengis.gml.v32.Point;
import net.opengis.gml.v32.impl.GMLFactory;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.fakeweather.FakeWeatherOutput;
import org.vast.ogc.om.SamplingPoint;
import org.vast.swe.SWEHelper;


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
    private static final String UID_PREFIX = "urn:test:sensors:simweather:";
    
    FakeWeatherOutput dataInterface;
    SamplingPoint foi;
    
    
    public FakeWeatherSensor()
    {        
    }
    
    
    @Override
    public void init(FakeWeatherConfig config) throws SensorHubException
    {
        super.init(config);
        
        // init main data interface
        dataInterface = new FakeWeatherOutput(this);
        addOutput(dataInterface, false);
        dataInterface.init();
        
        generateFoi();
    }
    
    
    @Override
    public void updateConfig(FakeWeatherConfig config) throws SensorHubException
    {
        super.updateConfig(config);
        generateFoi();
    }
    
    
    protected void generateFoi()
    {
        // create FoI
        GMLFactory gml = new GMLFactory();
        foi = new SamplingPoint();
        foi.setUniqueIdentifier(UID_PREFIX + config.serialNumber + ":foi");
        foi.setName("Weather Station Location");
        Point p = gml.newPoint();
        p.setSrsName(SWEHelper.REF_FRAME_4979);
        p.setPos(new double[] {config.stationLat, config.stationLon, config.stationAlt});
        foi.setShape(p);
    }


    @Override
    public AbstractFeature getCurrentFeatureOfInterest()
    {
        return foi;
    }


    @Override
    protected void updateSensorDescription()
    {
        synchronized (sensorDescription)
        {
            super.updateSensorDescription();
            sensorDescription.setId("WEATHER_STATION");
            sensorDescription.setUniqueIdentifier(UID_PREFIX + config.serialNumber);
            sensorDescription.setDescription("Simulated weather station generating randomly increasing and decreasing measurements");
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

