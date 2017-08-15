/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.process.cam;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.algo.geoloc.GeoTransforms;
import org.sensorhub.algo.geoloc.NadirPointing;
import org.sensorhub.algo.vecmath.Mat3d;
import org.sensorhub.algo.vecmath.Vect3d;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.processing.DataSourceConfig;
import org.sensorhub.api.processing.ProcessException;
import org.sensorhub.impl.processing.AbstractStreamProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.process.DataQueue;
import org.vast.swe.SWEConstants;
import org.vast.swe.helper.GeoPosHelper;


/**
 * <p>
 * Orocess for controlling a PTZ camera to point at a particular geographic
 * location.
 * </p>
 *
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Aug 9, 2015
 */
public class CamPtzGeoPointingProcess extends AbstractStreamProcess<CamPtzGeoPointingConfig>
{
    protected static final Logger log = LoggerFactory.getLogger(CamPtzGeoPointingProcess.class);
        
    protected CamPtzGeoPointingOutput camPtzOutput;
    protected GeoTransforms geoConv = new GeoTransforms();
    protected NadirPointing nadirPointing = new NadirPointing();
    
    protected boolean lastCamPosSet = false;
    protected boolean lastCamRotSet = false;
    protected Vect3d lastCamPosEcef = new Vect3d();
    protected Vect3d lastCamRotEnu = new Vect3d();
    protected Vect3d targetPosEcef = new Vect3d();
    protected Vect3d llaCam = new Vect3d();
    protected Vect3d llaTarget = new Vect3d();
    protected Mat3d ecefRot = new Mat3d();
    
    protected DataRecord cameraLocInput;
    protected DataRecord cameraRotInput;
    protected DataRecord targetLocInput;    
    protected DataQueue cameraLocQueue;
    protected DataQueue cameraRotQueue;
    protected DataQueue targetLocQueue;
    protected ExecutorService threadPool;
    
    
    @Override
    public void init(CamPtzGeoPointingConfig config) throws SensorHubException
    {
        this.config = config;
        
        // initialize with fixed location if set
        if (config.fixedCameraPosLLA != null)
        {
            double[] pos = config.fixedCameraPosLLA; // lat,lon,alt in degrees
            
            try
            {
                llaCam.set(Math.toRadians(pos[1]), Math.toRadians(pos[0]), pos[2]);
                geoConv.LLAtoECEF(llaCam, lastCamPosEcef);
                lastCamPosSet = true;
            }
            catch (Exception e)
            {
                throw new SensorHubException("Invalid camera position: " + Arrays.toString(pos));
            }
        }
        
        // initializa with fixed orientation if set
        if (config.fixedCameraRotENU != null)
        {
            double[] rot = config.fixedCameraRotENU; // pitch,roll,yaw in degrees
            
            try
            {
                lastCamRotEnu.x = Math.toRadians(rot[0]);
                lastCamRotEnu.y = Math.toRadians(rot[1]);
                lastCamRotEnu.z = Math.toRadians(rot[2]);
                lastCamRotSet = true;
            }
            catch (Exception e)
            {
                throw new SensorHubException("Invalid camera orientation: " + Arrays.toString(rot));
            }
        }
        
        // create inputs
        GeoPosHelper fac = new GeoPosHelper();   
        
        cameraLocInput = fac.newDataRecord();
        cameraLocInput.setName("camLocation");
        cameraLocInput.addField("time", fac.newTimeStampIsoUTC());
        cameraLocInput.addField("loc", fac.newLocationVectorLLA(SWEConstants.DEF_SENSOR_LOC));
        inputs.put(cameraLocInput.getName(), cameraLocInput);
        
        cameraRotInput = fac.newDataRecord();
        cameraRotInput.setName("camRotation");
        cameraRotInput.addField("time", fac.newTimeStampIsoUTC());
        cameraRotInput.addField("rot", fac.newEulerOrientationENU(null));
        inputs.put(cameraRotInput.getName(), cameraRotInput);
        
        targetLocInput = fac.newDataRecord();
        targetLocInput.setName("targetLocation");
        targetLocInput.addField("time", fac.newTimeStampIsoUTC());
        targetLocInput.addField("loc", fac.newLocationVectorLLA(null));
        targetLocInput.addField("keepZoom", fac.newBoolean("http://sensorml.com/ont/swe/property/KeepZoom", "Keep Zoom", null));
        inputs.put(targetLocInput.getName(), targetLocInput);
        
        // create outputs
        camPtzOutput = new CamPtzGeoPointingOutput(this);
        addOutput(camPtzOutput);
        
        super.init(config);
    }
    
    
    @Override
    protected void connectInput(String inputName, String dataPath, DataQueue inputQueue) throws ProcessException
    {        
        super.connectInput(inputName, dataPath, inputQueue);
        
        if (inputName.equals(cameraLocInput.getName()))
            cameraLocQueue = inputQueue;
        
        if (inputName.equals(cameraRotInput.getName()))
            cameraRotQueue = inputQueue;
        
        else if (inputName.equals(targetLocInput.getName()))
            targetLocQueue = inputQueue;
    }
    
    
    @Override
    public void start() throws SensorHubException
    {
        super.start();
        camPtzOutput.start();
        threadPool = new ThreadPoolExecutor(1, 1, 10000L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
    }
    
    
    @Override
    protected synchronized void process(final DataEvent lastEvent) throws ProcessException
    {        
        try
        {
            if (cameraLocQueue != null && cameraLocQueue.isDataAvailable())
            {
                // data received is LLA in degrees
                DataBlock dataBlk = cameraLocQueue.get();
                double lat = dataBlk.getDoubleValue(1);
                double lon = dataBlk.getDoubleValue(2);
                double alt = dataBlk.getDoubleValue(3);
                log.trace("Last camera pos = [{},{},{}]" , lat, lon, alt);
                
                // convert to radians and then ECEF
                llaCam.y = Math.toRadians(lat);
                llaCam.x = Math.toRadians(lon);
                llaCam.z = alt;
                geoConv.LLAtoECEF(llaCam, lastCamPosEcef);
                lastCamPosSet = true;
            }
            
            else if (cameraRotQueue != null && cameraRotQueue.isDataAvailable())
            {
                // data received is euler angles in degrees
                DataBlock dataBlk = cameraRotQueue.get();
                double pitch = dataBlk.getDoubleValue(1);
                double roll = dataBlk.getDoubleValue(2);
                double yaw = dataBlk.getDoubleValue(3);
                log.trace("Last camera rot = [{},{},{}]" , pitch, roll, yaw);
                
                // convert to radians
                lastCamRotEnu.x = Math.toRadians(pitch);
                lastCamRotEnu.y = Math.toRadians(roll);
                lastCamRotEnu.z = Math.toRadians(yaw);
                lastCamRotSet = true;
            }
            
            else if (lastCamPosSet && lastCamRotSet && targetLocQueue.isDataAvailable())
            {            
                // data received is LLA in degrees
                DataBlock dataBlk = targetLocQueue.get();
                final double time = dataBlk.getDoubleValue(0);
                final double lat = dataBlk.getDoubleValue(1);
                final double lon = dataBlk.getDoubleValue(2);
                final double alt = dataBlk.getDoubleValue(3);  
                final boolean keepZoom = dataBlk.getBooleanValue(4);
                log.debug("Target pos = [{},{},{}]" , lat, lon, alt);
                
                threadPool.execute(new Runnable() {
                    @Override
                    public void run()
                    {
                        processInThread(time, lat, lon, alt, keepZoom);
                        log.info("PTZ pointing done");
                    }            
                });
            }
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
        catch (RejectedExecutionException e)
        {
            log.error("PTZ geo-pointing process already running");
        }
    }
    
    
    protected void processInThread(double time, double lat, double lon, double alt, boolean keepZoom)
    {
        try
        {
            // convert to radians and then ECEF
            llaTarget.y = Math.toRadians(lat);
            llaTarget.x = Math.toRadians(lon);
            llaTarget.z = alt;
            geoConv.LLAtoECEF(llaTarget, targetPosEcef);
            
            // compute LOS from camera to target
            Vect3d los = targetPosEcef.sub(lastCamPosEcef);
            double dist = los.norm();
            los.scale(1./dist); // normalize
            
            // transform LOS to ENU frame
            nadirPointing.getRotationMatrixENUToECEF(lastCamPosEcef, ecefRot);
            ecefRot.transpose();
            los.rotate(ecefRot);
            
            // transform LOS to camera frame
            los.rotateZ(-lastCamRotEnu.z);
            //los.rotateY(lastCameraRotEnu.y);
            //los.rotateX(lastCameraRotEnu.x);
            
            // compute PTZ values
            double pan = Math.toDegrees(Math.atan2(los.y, los.x));
            if (pan < 0)
            	pan += 360.0;
            double xyProj = Math.sqrt(los.x*los.x + los.y*los.y);
            double tilt = Math.toDegrees(Math.atan2(los.z, xyProj));
            
            // compute zoom value
            log.debug("Distance to target = {}", dist);
            double sensorSize = config.cameraSensorSize/1000.;
            double minFocal = config.cameraMinFocalLength;
            double maxFocal = config.cameraMaxFocalLength;
            double desiredFocal = dist*sensorSize/config.desiredViewSize*1000.;   
            double zoom = (desiredFocal - minFocal) / (maxFocal - minFocal);
            zoom = Math.min(Math.max(zoom, 0.), 1.);
            log.debug("Computed PTZ = [{},{},{}]", pan, tilt, zoom);
            
            if (keepZoom)
            {
            	// set zoom to previous zoom factor
            	zoom = Double.NaN;
            }
            
            // send to PTZ output
            camPtzOutput.sendPtz(time, pan, tilt, zoom);
        }
        catch (Exception e)
        {
            log.error("Error computing PTZ position", e);
        }
    }
    
    
    @Override
    public boolean isPauseSupported()
    {
        return false;
    }

    
    @Override
    public boolean isCompatibleDataSource(DataSourceConfig dataSource)
    {
        return true;
    }
}
