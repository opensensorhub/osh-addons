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

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.Vector;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.swe.SWEConstants;
import org.vast.swe.helper.GeoPosHelper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


public class FakeGpsOutput extends AbstractSensorOutput<FakeGpsSensor>
{
    DataComponent posDataStruct;
    DataEncoding posDataEncoding;
    List<double[]> trajPoints;
    boolean sendData;
    Timer timer;
    double currentTrackPos;
    

    public FakeGpsOutput(FakeGpsSensor parentSensor)
    {
        super(parentSensor);
        trajPoints = new ArrayList<>();
    }


    @Override
    public String getName()
    {
        return "gpsLocation";
    }


    protected void init()
    {
        GeoPosHelper fac = new GeoPosHelper();
        
        // SWE Common data structure
        posDataStruct = fac.newDataRecord(3);
        posDataStruct.setName(getName());
                
        posDataStruct.addComponent("time", fac.newTimeStampIsoGPS());
        
        Vector locVector = fac.newLocationVectorLLA(SWEConstants.DEF_SENSOR_LOC);
        locVector.setLabel("Location");
        locVector.setDescription("Location measured by GPS device");
        posDataStruct.addComponent("location", locVector);
        
        posDataEncoding = fac.newTextEncoding(",", "\n");
    }


    private boolean generateRandomTrajectory()
    {
        FakeGpsConfig config = getParentModule().getConfiguration();
        
        // used fixed start/end coordinates or generate random ones 
        double startLat;
        double startLong;
        double endLat;
        double endLong;
        
        if (trajPoints.isEmpty())
        {
            startLat = config.centerLatitude + (Math.random()-0.5) * config.areaSize;
            startLong = config.centerLongitude + (Math.random()-0.5) * config.areaSize;
            
            // if fixed start and end locations not given, pick random values within area 
            if (config.startLatitude == null || config.startLongitude == null ||
                config.stopLatitude == null || config.stopLongitude == null)
            {
                startLat = config.centerLatitude + (Math.random()-0.5) * config.areaSize;
                startLong = config.centerLongitude + (Math.random()-0.5) * config.areaSize;
                endLat = config.centerLatitude + (Math.random()-0.5) * config.areaSize;
                endLong = config.centerLongitude + (Math.random()-0.5) * config.areaSize;
            }
            
            // else use start/end locations provided in configuration
            else
            {
                startLat = config.startLatitude;
                startLong = config.startLongitude;
                endLat = config.stopLatitude;
                endLong = config.stopLongitude;
            }
        }
        else
        {
            // restart from end of previous track
            double[] lastPoint = trajPoints.get(trajPoints.size()-1);
            startLat = lastPoint[0];
            startLong = lastPoint[1];
            endLat = config.centerLatitude + (Math.random()-0.5) * config.areaSize;
            endLong = config.centerLongitude + (Math.random()-0.5) * config.areaSize;
        }        
        
        
        try
        {
            // request directions using Google API
            URL dirRequest = new URL(config.googleApiUrl + "?key=" + config.googleApiKey +
                    "&origin=" + startLat + "," + startLong +
                    "&destination=" + endLat + "," + endLong + ((config.walkingMode) ? "&mode=walking" : ""));
            log.debug("Google API request: " + dirRequest);
            InputStream is = new BufferedInputStream(dirRequest.openStream());
            
            // parse JSON track
            JsonParser reader = new JsonParser();
            JsonElement root = reader.parse(new InputStreamReader(is));
            JsonObject rootObj = root.getAsJsonObject();
            
            //System.out.println(root);
            JsonElement routes = rootObj.get("routes");
            if (routes == null || !routes.isJsonArray() || routes.getAsJsonArray().size() == 0)
            {
                String errorMsg = "No route available";
                JsonElement errorField = rootObj.get("error_message");
                if (errorField != null)
                    errorMsg = errorField.getAsString();
                throw new Exception(errorMsg);
            }
            
            JsonElement polyline = routes.getAsJsonArray().get(0).getAsJsonObject().get("overview_polyline");
            String encodedData = polyline.getAsJsonObject().get("points").getAsString();
            
            // decode polyline data
            decodePoly(encodedData);
            currentTrackPos = 0.0;
            parentSensor.clearError();
            return true;
        }
        catch (Exception e)
        {
            parentSensor.reportError("Error while retrieving Google directions", e);
            trajPoints.clear();
            try { Thread.sleep(60000L); }
            catch (InterruptedException e1) {}
            return false;
        }
    }


    private void decodePoly(String encoded)
    {
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;        
        trajPoints.clear();
        
        while (index < len)
        {
            int b, shift = 0, result = 0;
            do
            {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            }
            while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do
            {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            }
            while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            double[] p = new double[] {(double) lat / 1E5, (double) lng / 1E5};
            trajPoints.add(p);
        }
    }
    
    
    private void sendMeasurement()
    {
        if (trajPoints.isEmpty() || currentTrackPos >= trajPoints.size()-2)
        {
            if (!generateRandomTrajectory())
                return;
            
            // skip if generated traj is too small
            if (trajPoints.size() < 2)
            {
                trajPoints.clear();
                return;
            }
            //for (double[] p: trajPoints)
            //     System.out.println(Arrays.toString(p));
        }
        
        // convert speed from km/h to lat/lon deg/s
        double speed = getParentModule().getConfiguration().vehicleSpeed / 20000 * 180 / 3600;
        int trackIndex = (int)currentTrackPos;
        double ratio = currentTrackPos - trackIndex;
        double[] p0 = trajPoints.get(trackIndex);
        double[] p1 = trajPoints.get(trackIndex+1);
        double dLat = p1[0] - p0[0];
        double dLon = p1[1] - p0[1];
        double dist = Math.sqrt(dLat*dLat + dLon*dLon);        
        
        // compute new position
        double time = System.currentTimeMillis() / 1000.;
        double lat = p0[0] + dLat*ratio;
        double lon = p0[1] + dLon*ratio;
        double alt = 193;
        
        // build and publish datablock
        DataBlock dataBlock = posDataStruct.createDataBlock();
        dataBlock.setDoubleValue(0, time);
        dataBlock.setDoubleValue(1, lat);
        dataBlock.setDoubleValue(2, lon);
        dataBlock.setDoubleValue(3, alt);
        
        // update latest record and send event
        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, FakeGpsOutput.this, dataBlock));
        
        currentTrackPos += speed / dist;
    }


    protected void start()
    {
        if (timer != null)
            return;
        timer = new Timer();
        
        // start main measurement generation thread
        TimerTask task = new TimerTask() {
            public void run()
            {
                sendMeasurement();
            }            
        };
        
        timer.schedule(task, 0, (long)(getAverageSamplingPeriod()*1000));        
    }


    protected void stop()
    {
        if (timer != null)
        {
            timer.cancel();
            timer = null;
        }
        
        trajPoints.clear();
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
        return posDataEncoding;
    }

}
