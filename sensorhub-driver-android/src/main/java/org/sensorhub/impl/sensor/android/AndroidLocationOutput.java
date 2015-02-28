/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.android;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.Time;
import net.opengis.swe.v20.Vector;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.data.SWEFactory;
import org.vast.data.TextEncodingImpl;
import org.vast.swe.SWEConstants;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;


/**
 * <p>
 * Implementation of data interface for Android location providers
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Jan 18, 2015
 */
public class AndroidLocationOutput extends AbstractSensorOutput<AndroidSensorsDriver> implements IAndroidOutput, LocationListener
{
    // keep logger name short because in LogCat it's max 23 chars
    //private static final Logger log = LoggerFactory.getLogger(AndroidLocationOutput.class.getSimpleName());
            
    private static final String LOC_DEF = "http://sensorml.com/ont/swe/property/Location";
    private static final String LOC_CRS = "http://www.opengis.net/def/crs/EPSG/0/4979";
    //private static final String TIME_REF = "http://www.opengis.net/def/trs/USNO/0/GPS";
    private static final String TIME_REF = "http://www.opengis.net/def/trs/BIPM/0/UTC";
    private static final long GPS_TO_UTC_OFFSET = -16000L;
    
    LocationManager locManager;
    LocationProvider locProvider;
    String name;
    boolean enabled;
    DataBlock latestRecord;
    DataComponent posDataStruct;
    
    
    protected AndroidLocationOutput(AndroidSensorsDriver parentModule, LocationManager locManager, LocationProvider locProvider)
    {
        super(parentModule);
        this.locManager = locManager;
        this.locProvider = locProvider;        
        this.name = locProvider.getName().replaceAll(" ", "_") + "_data";
    }
    
    
    @Override
    public String getName()
    {
        return name;
    }
    
    
    @Override
    public void init()
    {
        // SWE Common data structure
        SWEFactory fac = new SWEFactory();
        posDataStruct = fac.newDataRecord(2);
        posDataStruct.setName(getName());
        
        Time c1 = fac.newTime();
        c1.getUom().setHref(Time.ISO_TIME_UNIT);
        c1.setDefinition(SWEConstants.DEF_SAMPLING_TIME);
        c1.setReferenceFrame(TIME_REF);
        posDataStruct.addComponent("time", c1);

        Vector vec = fac.newVector();        
        vec.setDefinition(LOC_DEF);
        ((Vector)vec).setReferenceFrame(LOC_CRS);
        ((Vector)vec).setLocalFrame("#" + AndroidSensorsDriver.LOCAL_REF_FRAME);
        posDataStruct.addComponent("location", vec);
        
        Quantity c;
        c = fac.newQuantity();
        c.getUom().setCode("deg");
        c.setDefinition("http://sensorml.com/ont/swe/property/Latitude");
        c.setAxisID("Lat");
        vec.addComponent("lat",c);

        c = fac.newQuantity();
        c.getUom().setCode("deg");
        c.setDefinition("http://sensorml.com/ont/swe/property/Longitude");
        c.setAxisID("Long");
        vec.addComponent("lon", c);

        c = fac.newQuantity();
        c.getUom().setCode("m");
        c.setDefinition("http://sensorml.com/ont/swe/property/Altitude");
        c.setAxisID("h");
        vec.addComponent("alt", c);
        
        // request location data
        locManager.requestLocationUpdates(locProvider.getName(), 100, 0.0f, this);
    }
    
    
    @Override
    public void stop()
    {
        locManager.removeUpdates(this);
    }


    @Override
    public boolean isEnabled()
    {
        return enabled;
    }


    @Override
    public double getAverageSamplingPeriod()
    {
        return 1.0;
    }


    @Override
    public DataComponent getRecordDescription()
    {
        return posDataStruct;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return new TextEncodingImpl(",", "\n");
    }

    
    @Override
    public DataBlock getLatestRecord()
    {
        return latestRecord;
    }
    
    
    @Override
    public double getLatestRecordTime()
    {
        if (latestRecord != null)
            return latestRecord.getDoubleValue(0);
        
        return Double.NaN;
    }


    @Override
    public void onLocationChanged(Location location)
    {
        /*log.debug("Location received from " + getName() + ": "
                  + location.getLatitude() + ", " +
                  + location.getLongitude() + ", " +
                  + location.getAltitude()); */
        
        double sampleTime = location.getTime() / 1000.0;
        double eventTime = System.currentTimeMillis() / 1000.0;
                
        // build and populate datablock
        DataBlock dataBlock = posDataStruct.createDataBlock();
        dataBlock.setDoubleValue(0, sampleTime);
        dataBlock.setDoubleValue(1, location.getLatitude());
        dataBlock.setDoubleValue(2, location.getLongitude());
        dataBlock.setDoubleValue(3, location.getAltitude());        
                
        // update latest record and send event
        latestRecord = dataBlock;
        eventHandler.publishEvent(new SensorDataEvent(eventTime, this, dataBlock));
    }


    @Override
    public void onStatusChanged(String provider, int status, Bundle extras)
    {
        
    }


    @Override
    public void onProviderEnabled(String provider)
    {
        enabled = true;        
    }


    @Override
    public void onProviderDisabled(String provider)
    {
        enabled = false;        
    }
    
}
