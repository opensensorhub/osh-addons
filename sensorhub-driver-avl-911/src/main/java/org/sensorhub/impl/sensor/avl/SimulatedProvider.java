/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.avl;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.module.AbstractModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.util.DateTimeFormat;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;


/**
 * <p>
 * AVL data provider generating pseudo random vehicle trajectory data
 * using Google Directions API.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Nov 29, 2015
 */
public class SimulatedProvider extends AbstractModule<SimulatedProviderConfig> implements ICommProvider<SimulatedProviderConfig>
{
    private static final Logger log = LoggerFactory.getLogger(SimulatedProvider.class);
    private static Object lock = new Object();
    
    List<TrajectoryGenerator> vehicleTrajs;
    SimpleDateFormat timeFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    PipedInputStream pipeOut;
    PipedOutputStream pipeIn;
    ScheduledExecutorService timer;
    long updatePeriod = 1000L;
    long simTime;
    boolean started;
    
    
    class TrajectoryGenerator
    {
        SimulatedVehicleConfig config;
        List<double[]> trajPoints = new ArrayList<>();
        double currentTrackPos;
                
        public TrajectoryGenerator(SimulatedVehicleConfig config)
        {
            this.config = config;
        }
        
        private boolean generateTrajectory()
        {
            SimulatedProviderConfig mainConfig = SimulatedProvider.this.getConfiguration();
            
            // used fixed start/end coordinates or generate random ones 
            double startLat;
            double startLong;
            double endLat;
            double endLong;
            
            if (trajPoints.isEmpty())
            {
                // if fixed start and end locations not given, pick random values within area
                // else use start/end locations provided in configuration
                
                if (config.startLat == null || config.startLon == null)
                {
                    startLat = mainConfig.centerLat + (Math.random()-0.5) * mainConfig.areaSize;
                    startLong = mainConfig.centerLon + (Math.random()-0.5) * mainConfig.areaSize;
                }
                else
                {
                    startLat = config.startLat;
                    startLong = config.startLon;
                }
                
                if (config.stopLat == null || config.stopLon == null)
                {
                    endLat = mainConfig.centerLat + (Math.random()-0.5) * mainConfig.areaSize;
                    endLong = mainConfig.centerLon + (Math.random()-0.5) * mainConfig.areaSize;
                }
                else
                {
                    endLat = config.stopLat;
                    endLong = config.stopLon;
                }
            }
            else
            {
                // restart from end of previous track
                double[] lastPoint = trajPoints.get(trajPoints.size()-1);
                startLat = lastPoint[0];
                startLong = lastPoint[1];
                endLat = mainConfig.centerLat + (Math.random()-0.5) * mainConfig.areaSize;
                endLong = mainConfig.centerLon + (Math.random()-0.5) * mainConfig.areaSize;
            } 
            
            try
            {
                synchronized(lock)
                {
                    // request directions using Google API
                    URL dirRequest = new URL(mainConfig.googleApiUrl + "?key=" + mainConfig.googleApiKey +
                            "?origin=" + startLat + "," + startLong +
                            "&destination=" + endLat + "," + endLong);
                    log.debug("Google API request: " + dirRequest);
                    InputStream is = new BufferedInputStream(dirRequest.openStream());
                    
                    // parse JSON track
                    JsonParser reader = new JsonParser();
                    JsonElement root = reader.parse(new InputStreamReader(is));
                    //System.out.println(root);
                    JsonArray routes = root.getAsJsonObject().get("routes").getAsJsonArray();
                    if (routes.size() == 0) {
                        System.err.println(root);
                        throw new Exception("No route available for vehicle " + config.vehicleID);                    
                    }
                    JsonElement polyline = routes.get(0).getAsJsonObject().get("overview_polyline");
                    String encodedData = polyline.getAsJsonObject().get("points").getAsString();
                    
                    // decode polyline data
                    decodePoly(encodedData);
                    currentTrackPos = 0.0;
                    
                    // wait some to avoid going over Google servers rate limit
                    try { Thread.sleep(500L); }
                    catch (InterruptedException e1) {}
                    
                    return true;
                }
            }
            catch (Exception e)
            {
                log.error("Error while retrieving Google directions", e);
                trajPoints.clear();
                try { Thread.sleep(1000L); }
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
        
        public void sendCurrentPos(long time)
        {
            if (trajPoints.isEmpty() || currentTrackPos >= trajPoints.size()-2)
            {
                // stop after first run if requested
                if (!trajPoints.isEmpty() && config.stopAtEnd)
                    return;                
                
                if (!generateTrajectory())
                    return;
                
                // skip if generated traj is too small
                if (trajPoints.size() < 2)
                {
                    trajPoints.clear();
                    return;
                }
            }
            
            // convert speed from km/h to lat/lon deg/s
            double speed = config.speed / 20000 * 180 / 3600;
            int trackIndex = (int)currentTrackPos;
            double ratio = currentTrackPos - trackIndex;
            double[] p0 = trajPoints.get(trackIndex);
            double[] p1 = trajPoints.get(trackIndex+1);
            double dLat = p1[0] - p0[0];
            double dLon = p1[1] - p0[1];
            double dist = Math.sqrt(dLat*dLat + dLon*dLon);        
            
            // compute new position
            double lat = p0[0] + dLat*ratio;
            double lon = p0[1] + dLon*ratio;
            
            currentTrackPos += speed / dist;
            timeFormat.setTimeZone(TimeZone.getTimeZone("US/Central"));
            
            try
            {
                // generate AVL record
                // 20140329000003CD 18002 6401026A FE4 FE4 +34.73744 -86.53156 446388998 1542408221 0 246 0 0 2 0 00000 AQ 
                StringBuilder buf = new StringBuilder();
                buf.append(timeFormat.format(time)).append("CD ")
                   .append("18002 ")
                   .append("6401026A ")
                   .append(config.vehicleID).append(' ')
                   .append(config.vehicleID).append(' ')
                   .append(lat).append(' ')
                   .append(lon).append(' ')
                   .append("446388998 1542408221 0 246 0 0 2 0 00000 ")
                   .append("AQ")
                   .append('\n');
                
                pipeIn.write(buf.toString().getBytes());
                pipeIn.flush();
            }
            catch (IOException e)
            {
                throw new IllegalStateException("Error while writing to piped stream", e);
            }
        }
    }
    

    @Override
    public synchronized void start() throws SensorHubException
    {
        if (config.googleApiKey == null || config.googleApiKey.isEmpty())
            throw new SensorHubException("A Google API key with access to the Directions API must be provided in the configuration");
        
        if (timer != null)
            return;
        timer = Executors.newScheduledThreadPool(1);
        
        // create piped stream
        try
        {
            pipeOut = new PipedInputStream(1024);
            pipeIn = new PipedOutputStream(pipeOut);            
        }
        catch (IOException e)
        {
            throw new SensorHubException("Error while creating piped stream", e);
        }
        
        // create 1 traj generator per vehicle
        vehicleTrajs = new ArrayList<>();
        for (SimulatedVehicleConfig config: getConfiguration().vehicles)
        {
            TrajectoryGenerator trajGen = new TrajectoryGenerator(config);
            vehicleTrajs.add(trajGen);
        }
        
        // init time
        try
        {
            if (config.startDate != null)
                simTime = (long)(new DateTimeFormat().parseIso(config.startDate) * 1000);
            else
                simTime = System.currentTimeMillis();
        }
        catch (ParseException e)
        {
            throw new SensorHubException("Invalid start date: " + config.startDate);
        }
                
        // start main measurement generation thread
        Runnable task = new Runnable() {
            public void run()
            {
                for (TrajectoryGenerator gen: vehicleTrajs)
                {
                    if (!started)
                    {
                        try { pipeIn.close(); }
                        catch (IOException e) {  }
                        break;
                    }
                    gen.sendCurrentPos(simTime);
                }
                
                simTime += updatePeriod;
            }
        };
        
        started = true;
        timer.scheduleAtFixedRate(task, 0, updatePeriod/10, TimeUnit.MILLISECONDS);  
    }
    
    
    @Override
    public synchronized void stop() throws SensorHubException
    {
        try
        {
            started = false;
            timer.shutdown();
            timer.awaitTermination(100L, TimeUnit.MILLISECONDS);
            timer = null;
        }
        catch (Exception e)
        {
        }
    }
    
    
    @Override
    public InputStream getInputStream() throws IOException
    {
        return pipeOut;
    }


    @Override
    public OutputStream getOutputStream() throws IOException
    {
        return null;
    }


    @Override
    public void cleanup() throws SensorHubException
    {

    }
}
