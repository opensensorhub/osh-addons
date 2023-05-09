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

import java.util.Random;
import org.vast.ogc.gml.IFeature;
import org.vast.swe.SWEConstants;
import net.opengis.gml.v32.AbstractGeometry;
import net.opengis.gml.v32.impl.GMLFactory;
import net.opengis.swe.v20.DataBlock;


/**
 * <p>
 * TODO FakeStation type description
 * </p>
 *
 * @author Alex Robin
 * @since Jan 29, 2021
 */
public class FakeWeatherStation implements IFeature
{
    String id;
    String uid;
    double lat, lon;
    
    // reference values around which actual values vary
    transient double tempRef = 20.0;
    transient double pressRef = 1013.0;
    transient double windSpeedRef = 5.0;
    transient double directionRef = 0.0;

    // initialize then keep new values for each measurement
    transient double temp = tempRef;
    transient double press = pressRef;
    transient double windSpeed = windSpeedRef;
    transient double windDir = directionRef;
    
    transient Random rand = new Random();
    transient AbstractGeometry geom;
    
    
    FakeWeatherStation(int num, double lat, double lon)
    {
        this.id = String.format("WS%03d", num);
        this.uid = "urn:osh:sensor:simweather:station:" + id;
        this.lat = lat;
        this.lon = lon;        
    }
    
    
    DataBlock createMeasurement(DataBlock prevData)
    {
        // generate new weather values
        double time = System.currentTimeMillis() / 1000.;
        
        // temperature; value will increase or decrease by less than 0.1 deg
        temp += variation(temp, tempRef, 0.001, 0.1);
        
        // pressure; value will increase or decrease by less than 20 hPa
        press += variation(press, pressRef, 0.001, 0.1);
        
        // wind speed; keep positive
        // vary value between +/- 10 m/s
        windSpeed += variation(windSpeed, windSpeedRef, 0.001, 0.1);
        windSpeed = windSpeed < 0.0 ? 0.0 : windSpeed;
        
        // wind direction; keep between 0 and 360 degrees
        windDir += 1.0 * (2.0 * Math.random() - 1.0);
        windDir = windDir < 0.0 ? windDir+360.0 : windDir;
        windDir = windDir > 360.0 ? windDir-360.0 : windDir;
        
        // build datablock
        int idx = 0;
        DataBlock dataBlock = prevData.renew();
        dataBlock.setDoubleValue(idx++, time);
        dataBlock.setStringValue(idx++, id);
        dataBlock.setDoubleValue(idx++, temp);
        dataBlock.setDoubleValue(idx++, press);
        dataBlock.setDoubleValue(idx++, windSpeed);
        dataBlock.setDoubleValue(idx++, windDir);
        
        return dataBlock;
    }
    
    
    private double variation(double val, double ref, double dampingCoef, double noiseSigma)
    {
        return -dampingCoef*(val - ref) + noiseSigma*rand.nextGaussian();
    }

    
    @Override
    public String getId()
    {
        return id;
    }


    @Override
    public String getUniqueIdentifier()
    {
        return uid;
    }


    @Override
    public String getName()
    {
        return "Station " + id;
    }


    @Override
    public String getDescription()
    {
        return "Weather station " + id + " part of demo weather network";
    }


    @Override
    public AbstractGeometry getGeometry()
    {
        if (geom == null)
        {
            geom = new GMLFactory(true).newPoint(lat, lon);
            geom.setSrsName(SWEConstants.REF_FRAME_4979);
        }
        return geom;
    }

}
