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

import java.awt.geom.Point2D;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import net.opengis.gml.v32.AbstractGeometry;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.data.DataEvent;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.ogc.gml.IFeature;
import org.vast.swe.SWEConstants;
import org.vast.swe.helper.GeoPosHelper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


public class FakeGpsOutput extends AbstractSensorOutput<FakeGpsSensor>
{
    DataComponent dataStruct;
    DataEncoding dataEncoding;
    ScheduledExecutorService timer;
    FakeGpsConfig config;
    List<Route> routes;
    Long lastApiCallTime = 0L;
    long samplingPeriodMillis;
    boolean multipleAssets;
    boolean sendData;
    SecureRandom random = new SecureRandom();
    
    
    static class MobileAsset implements IFeature
    {
        String id;
        String uid;
        double speed;
        double currentTrackPos;
        
        public String getId() { return id; }
        public String getUniqueIdentifier() { return uid; }
        public String getName() { return "Mobile Asset " + id; }
        public String getDescription() { return "Mobile asset with simulated location " + id; }
        public AbstractGeometry getGeometry() { return null; }
    }
    
    
    class Route
    {
        List<Point2D> points = new ArrayList<>();
        List<MobileAsset> assets = new ArrayList<>();
        ReadWriteLock lock = new ReentrantReadWriteLock();
    }
    

    public FakeGpsOutput(FakeGpsSensor parentSensor)
    {
        super("gpsLocation", parentSensor);
        
        this.config = parentSensor.getConfiguration();
        this.routes = new ArrayList<>();
        
        if (config instanceof FakeGpsNetworkConfig)
        {
            var conf = (FakeGpsNetworkConfig)config;
            for (int i = 0; i < conf.numRoutes;i++)
            {
                var route = new Route();
                for (int n = 0; n < conf.numAssetsPerRoute; n++)
                {
                    var asset = new MobileAsset();
                    asset.id = String.format("G%03dA%03d", i+1, n+1);
                    asset.uid = parentSensor.getUniqueIdentifier() + ":" + asset.id;
                    route.assets.add(asset);
                }
                this.routes.add(route);
            }
        }
        else
        {
            var route = new Route();
            route.assets.add(new MobileAsset());
            this.routes.add(route);
        }
        
        this.samplingPeriodMillis = (long)(config.samplingPeriodSeconds * 1000);
        this.multipleAssets = routes.size() > 1 || routes.get(0).assets.size() > 1;
    }


    protected void init()
    {
        // create output data structure
        GeoPosHelper fac = new GeoPosHelper();
        var recBuilder = fac.createRecord()
            .name(getName())
            .definition("urn:osh:sensor:simgps:gpsdata")
            .addField("time", fac.createTime()
                .asSamplingTimeIsoGPS());
        
        if (multipleAssets)
        {
            recBuilder.addField("assetID", fac.createText()
                .label("Asset ID")
                .description("Identifier of mobile asset"));
        }  
            
        recBuilder.addField("location", fac.createVector()
            .from(fac.newLocationVectorLLA(SWEConstants.DEF_SENSOR_LOC))
            .description("Location measured by the GPS device"));
            
        dataStruct = recBuilder.build();
        dataEncoding = fac.newTextEncoding(",", "\n");
    }


    private boolean generateRandomRoute(Route route)
    {
        // used fixed start/end coordinates or generate random ones 
        double startLat;
        double startLong;
        double endLat;
        double endLong;



        if (route.points.isEmpty())
        {
            startLat = config.centerLatitude + (random.nextDouble() - 0.5) * config.areaSize;
            startLong = config.centerLongitude + (random.nextDouble() - 0.5) * config.areaSize;

            // if fixed start and end locations not given, pick random values within area 
            if (config.startLatitude == null || config.startLongitude == null ||
                config.stopLatitude == null || config.stopLongitude == null)
            {
                startLat = config.centerLatitude + (random.nextDouble() - 0.5) * config.areaSize;
                startLong = config.centerLongitude + (random.nextDouble() - 0.5) * config.areaSize;
                endLat = config.centerLatitude + (random.nextDouble() - 0.5) * config.areaSize;
                endLong = config.centerLongitude + (random.nextDouble() - 0.5) * config.areaSize;
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
            var lastPoint = route.points.get(route.points.size() - 1);
            startLat = lastPoint.getY();
            startLong = lastPoint.getX();
            endLat = config.centerLatitude + (random.nextDouble() - 0.5) * config.areaSize;
            endLong = config.centerLongitude + (random.nextDouble() - 0.5) * config.areaSize;
        }

        try
        {
            // generate 10 more random waypoints spread across the entire area
            List<Point2D> waypoints = new ArrayList<>();
            for (int i = 0; i < 10; i++)
            {
                var wlat = config.centerLatitude + (random.nextDouble() - 0.5) * config.areaSize;
                var wlon = config.centerLongitude + (random.nextDouble() - 0.5) * config.areaSize;
                waypoints.add(new Point2D.Double(wlon, wlat));
            }

            // request directions using Google API
            String dirRequest = config.googleApiUrl + "?key=" + config.googleApiKey +
                ((config.walkingMode) ? "&mode=walking" : "") +
                "&origin=" + startLat + "," + startLong +
                "&destination=" + endLat + "," + endLong + "&waypoints=";
            for (var wp: waypoints)
                dirRequest += "via:" + wp.getY() + "," + wp.getX() + "|";
            log.debug("Google API request: " + dirRequest);
            try(InputStream is = new BufferedInputStream(new URL(dirRequest).openStream())) {


                // parse JSON track
                JsonElement root = JsonParser.parseReader(new InputStreamReader(is));
                JsonObject rootObj = root.getAsJsonObject();

                //System.out.println(root);
                JsonElement routes = rootObj.get("routes");
                if (routes == null || !routes.isJsonArray() || routes.getAsJsonArray().size() == 0) {
                    String errorMsg = "No route available";
                    JsonElement errorField = rootObj.get("error_message");
                    if (errorField != null)
                        errorMsg = errorField.getAsString();
                    throw new Exception(errorMsg);
                }

                JsonElement polyline = routes.getAsJsonArray().get(0).getAsJsonObject().get("overview_polyline");
                String encodedData = polyline.getAsJsonObject().get("points").getAsString();

                try {
                    route.lock.writeLock().lock();

                    // decode polyline data
                    decodePoly(encodedData, route.points);

                    // assign mobile assets to random speed and positions along the route
                    boolean first = true;
                    for (var asset : route.assets) {
                        if (first)
                            asset.currentTrackPos = 0.0;
                        else
                            asset.currentTrackPos = random.nextDouble() * (route.points.size() - 1);
                        first = false;

                        asset.speed = config.minSpeed + random.nextDouble() * Math.abs(config.maxSpeed - config.minSpeed);
                        if (random.nextDouble() < 0.5)
                            asset.speed = -asset.speed;
                    }
                } finally {
                    route.lock.writeLock().unlock();
                }

                lastApiCallTime = System.currentTimeMillis();
                parentSensor.clearError();
                return true;
            }
        }
        catch (Exception e)
        {
            parentSensor.reportError("Error while retrieving Google directions", e);
            return false;
        }
    }


    /*
     * Parse data coming out of Google Directions API
     * and build a list of Point2D
     */
    private void decodePoly(String encoded, List<Point2D> points)
    {
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;
        points.clear();

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

            var p = new Point2D.Double((double) lng / 1E5, (double) lat / 1E5);
            points.add(p);
        }
    }


    protected void sendMeasurement(Route route, MobileAsset asset)
    {
        try
        {
            if (route.points.size() < 2)
                return;
            
            route.lock.readLock().lock();
            int lastPointIdx = route.points.size()-1;
            //System.out.println("Updating pos for " + asset.id);
            
            //System.out.printf("track pos = %.2f\n", asset.currentTrackPos);
            var floorPos = (int)Math.floor(asset.currentTrackPos);
            var ceilPos = (int)Math.ceil(asset.currentTrackPos);
                        
            // get points around current position on route
            double ratio = asset.currentTrackPos - floorPos;
            var p0 = route.points.get(floorPos);
            var p1 =  route.points.get(ceilPos);
            double dLat = p1.getY() - p0.getY();
            double dLon = p1.getX() - p0.getX();
    
            // compute new position
            double time = System.currentTimeMillis() / 1000.;
            double lat = p0.getY() + dLat * ratio;
            double lon = p0.getX() + dLon * ratio;
            double alt = 193;
    
            // build and publish datablock
            DataBlock dataBlock = dataStruct.createDataBlock();
            int i = 0;
            dataBlock.setDoubleValue(i++, time);
            if (multipleAssets)
                dataBlock.setStringValue(i++, asset.id);
            dataBlock.setDoubleValue(i++, lat);
            dataBlock.setDoubleValue(i++, lon);
            dataBlock.setDoubleValue(i++, alt);
    
            // update latest record and send event
            latestRecord = dataBlock;
            latestRecordTime = System.currentTimeMillis();
            var foiUid = multipleAssets ? asset.getUniqueIdentifier() : null;
            eventHandler.publish(new DataEvent(latestRecordTime, FakeGpsOutput.this, foiUid, dataBlock));
            //System.out.printf("%.4f,%.4f\n", dataBlock.getDoubleValue(1), dataBlock.getDoubleValue(2)); 
            
            // compute next pos on route
            boolean reverse = asset.speed < 0;
            var trackIdxInc = reverse ? -1 : 1;
            var distanceLeft = Math.abs(asset.speed) / 3600. * samplingPeriodMillis / 1000.;
            //System.out.printf("distance needed = %.2fkm\n", distanceLeft);
            var pointIdx = reverse ? ceilPos : floorPos;
            p0 = route.points.get(pointIdx);
            do
            {
                pointIdx += trackIdxInc;
                if (ratio == 0.0 && reverse)
                    ratio = 1.0;
                
                // reverse route if we reached an end
                if (pointIdx < 0 || pointIdx > lastPointIdx)
                {
                    //System.out.println("reached " + (pointIdx <= 0 ? "start" : "end") + " of route. reversing course.");
                    pointIdx = pointIdx <= 0 ? 0 : lastPointIdx+1;
                    ratio = 0.0;
                    asset.speed = -asset.speed;
                    break;
                }
                
                p1 = route.points.get(pointIdx);
                double dist = computeArcDistance(p0, p1);
                //System.out.printf("segment length = %.2fkm\n", dist);
                
                // if in the middle of a segment
                var remainingDist = dist * (reverse ? ratio : (1.0-ratio));
                //System.out.printf("remaining length = %.2fkm\n", remainingDist);
                
                // if current segment is long enough
                if (remainingDist > distanceLeft)
                {
                    ratio += distanceLeft / dist * trackIdxInc;
                    //System.out.printf("distance on segment = %.2fkm\n", dist*ratio);
                    //System.out.printf("added distance = %.2fkm\n", distanceLeft);
                    break;
                }
                else
                {
                    //System.out.printf("added distance = %.2fkm\n", remainingDist);
                    distanceLeft -= remainingDist;
                    ratio = 0.0;
                }
                
                p0 = p1;
            }
            while (true);
            
            asset.currentTrackPos = (reverse ? pointIdx : pointIdx-1) + ratio;
        }
        catch (Exception e)
        {
            getLogger().error("Error computing measurement for asset {}", asset.id, e);
        }
        finally
        {
            route.lock.readLock().unlock();
        }
    }
    
    
    protected double computeArcDistance(Point2D p1, Point2D p2)
    {
        double lat1r = Math.toRadians(p1.getY());
        double lon1r = Math.toRadians(p1.getX());
        double lat2r = Math.toRadians(p2.getY());
        double lon2r = Math.toRadians(p2.getX());
        double thetaR = lon1r - lon2r;
        double dist = Math.sin(lat1r) * Math.sin(lat2r) + Math.cos(lat1r) * Math.cos(lat2r) * Math.cos(thetaR);
        return Math.acos(dist) * 6371; //;
    }


    protected void start()
    {
        if (timer != null)
            return;
        timer = Executors.newSingleThreadScheduledExecutor();
        
        // create all initial routes
        for (var route: routes)
        {
            generateRandomRoute(route);
            
            // schedule measurements with random delays
            for (var asset: route.assets)
            {
                var randomDelay = (long)(random.nextDouble() * samplingPeriodMillis);
                timer.scheduleAtFixedRate(() -> {
                    sendMeasurement(route, asset);
                }, randomDelay, samplingPeriodMillis, TimeUnit.MILLISECONDS);
            }
        }
        
        // schedule route updates
        var updatePeriod = TimeUnit.MINUTES.toMillis(config.apiRequestPeriodMinutes);
        timer.scheduleWithFixedDelay(() -> {
            // update one of the routes (randomly picked)
            if (System.currentTimeMillis() - lastApiCallTime > updatePeriod)
            {
                int idx = random.nextInt(routes.size());
                getLogger().info("Updating route #{}", idx);
                var route = routes.get(idx);
                generateRandomRoute(route);
            }
        }, 0, 60, TimeUnit.SECONDS); // we schedule more often in case request fails
    }


    protected void stop()
    {
        if (timer != null)
        {
            try
            {
                timer.shutdown();
                timer.awaitTermination(1L, TimeUnit.SECONDS);
            }
            catch (Exception e)
            {
                log.warn("Interrupted", e);
                Thread.currentThread().interrupt();
            }
            
            timer = null;
        }
        
        for (var route: routes)
            route.points.clear();
    }


    @Override
    public double getAverageSamplingPeriod()
    {
        return samplingPeriodMillis / 1000.;
    }


    @Override
    public DataComponent getRecordDescription()
    {
        return dataStruct;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return dataEncoding;
    }
}
