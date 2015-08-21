/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.process.cam;

import java.util.Arrays;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.processing.DataSourceConfig;
import org.sensorhub.api.processing.ProcessException;
import org.sensorhub.impl.process.geoloc.GeoTransforms;
import org.sensorhub.impl.process.geoloc.NadirPointing;
import org.sensorhub.impl.processing.AbstractStreamProcess;
import org.sensorhub.vecmath.Mat3d;
import org.sensorhub.vecmath.Vect3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.process.DataQueue;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;


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
        SWEHelper fac = new SWEHelper();   
        
        cameraLocInput = fac.newDataRecord();
        cameraLocInput.setName("camLocation");
        cameraLocInput.addField("time", fac.newTimeStampIsoUTC());
        cameraLocInput.addField("loc", fac.newLocationVectorLLA(SWEConstants.DEF_SENSOR_LOC));
        inputs.put(cameraLocInput.getName(), cameraLocInput);
        
        cameraRotInput = fac.newDataRecord();
        cameraRotInput.setName("camRotation");
        cameraRotInput.addField("time", fac.newTimeStampIsoUTC());
        cameraRotInput.addField("rot", fac.newEulerOrientationENU(SWEHelper.DEF_ORIENTATION));
        inputs.put(cameraRotInput.getName(), cameraRotInput);
        
        targetLocInput = fac.newDataRecord();
        targetLocInput.setName("targetLocation");
        targetLocInput.addField("time", fac.newTimeStampIsoUTC());
        targetLocInput.addField("loc", fac.newLocationVectorLLA(SWEHelper.DEF_LOCATION));
        inputs.put(targetLocInput.getName(), targetLocInput);
        
        // create outputs
        camPtzOutput = new CamPtzGeoPointingOutput(this);
        addOutput(camPtzOutput);
        
        super.init(config);
    }
    
    
    @Override
    protected void connectInput(String inputName, String dataPath, DataQueue inputQueue) throws Exception
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
    }
    
    
    @Override
    protected void process(DataEvent lastEvent) throws ProcessException
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
                log.debug("Last camera pos = [{},{},{}]" , lat, lon, alt);
                
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
                log.debug("Last camera rot = [{},{},{}]" , pitch, roll, yaw);
                
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
                double time = dataBlk.getDoubleValue(0);
                double lat = dataBlk.getDoubleValue(1);
                double lon = dataBlk.getDoubleValue(2);
                double alt = dataBlk.getDoubleValue(3);
                log.debug("Last target pos = [{},{},{}]" , lat, lon, alt);
                
                // convert to radians and then ECEF
                llaTarget.y = Math.toRadians(lat);
                llaTarget.x = Math.toRadians(lon);
                llaTarget.z = alt;
                geoConv.LLAtoECEF(llaTarget, targetPosEcef);
                
                // compute LOS from camera to target
                Vect3d los = targetPosEcef.sub(lastCamPosEcef);
                los.normalize();
                
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
                double xyProj = Math.sqrt(los.x*los.x + los.y*los.y); 
                double tilt = Math.toDegrees(Math.atan2(los.z, xyProj));
                
                // send to PTZ output
                camPtzOutput.sendPtz(time, pan, tilt, 1.0);
            }
        }
        catch (InterruptedException e)
        {
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
