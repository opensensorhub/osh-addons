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
        
    protected CamPtzGeoPointingOutput targetLocOutput;
    protected GeoTransforms geoConv = new GeoTransforms();
    protected NadirPointing nadirPointing = new NadirPointing();
    
    protected Vect3d lastCameraPosEcef = new Vect3d();
    protected Vect3d lastCameraRotEnu = new Vect3d();
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
            llaCam.set(Math.toRadians(pos[1]), Math.toRadians(pos[0]), pos[2]);
            geoConv.LLAtoECEF(llaCam, lastCameraPosEcef);
        }
        
        // initializa with fixed orientation if set
        if (config.fixedCameraRotENU != null)
        {
            double[] rot = config.fixedCameraRotENU; // pitch,roll,yaw in degrees
            lastCameraRotEnu.x = Math.toRadians(rot[0]);
            lastCameraRotEnu.y = Math.toRadians(rot[1]);
            lastCameraRotEnu.z = Math.toRadians(rot[2]);
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
        inputs.put(cameraLocInput.getName(), cameraLocInput);
        
        // create outputs
        targetLocOutput = new CamPtzGeoPointingOutput(this);
        addOutput(targetLocOutput);
        
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
    protected void process(DataEvent lastEvent) throws ProcessException
    {
        try
        {
            if (cameraLocQueue.isDataAvailable())
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
                geoConv.LLAtoECEF(llaCam, lastCameraPosEcef);
            }
            
            else if (cameraRotQueue.isDataAvailable())
            {
                // data received is euler angles in degrees
                DataBlock dataBlk = cameraRotQueue.get();
                double pitch = dataBlk.getDoubleValue(1);
                double roll = dataBlk.getDoubleValue(2);
                double yaw = dataBlk.getDoubleValue(3);
                log.debug("Last camera rot = [{},{},{}]" , pitch, roll, yaw);
                
                // convert to radians
                lastCameraRotEnu.x = Math.toRadians(pitch);
                lastCameraRotEnu.y = Math.toRadians(roll);
                lastCameraRotEnu.z = Math.toRadians(yaw);
            }
            
            else if (targetLocQueue.isDataAvailable())
            {
                // data received is LLA in degrees
                DataBlock dataBlk = targetLocQueue.get();
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
                Vect3d los = targetPosEcef.sub(lastCameraPosEcef);
                los.normalize();
                
                // transform LOS to ENU frame
                nadirPointing.getRotationMatrixENUToECEF(lastCameraPosEcef, ecefRot);
                ecefRot.transpose();
                los.rotate(ecefRot);
                
                // transform LOS to camera frame
                los.rotateZ(lastCameraRotEnu.z);
                los.rotateY(lastCameraRotEnu.y);
                los.rotateX(lastCameraRotEnu.x);
                
                // compute PTZ values
                
                
                // send to PTZ output
                
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
    
    
    public static void main(String[] args) throws Exception
    {
        /*TargetGeolocProcess p = new TargetGeolocProcess();
        TargetGeolocConfig processConf = new TargetGeolocConfig();
        processConf.fixedPosLLA = new double[] {0.0, 0.0, 0.0};
        p.init(processConf);
        p.sensorLocQueue = new DataQueue();
        p.rangeMeasQueue = new DataQueue();
        
        TruPulseSensor sensor = new TruPulseSensor();
        TruPulseConfig sensorConf = new TruPulseConfig();
        sensor.init(sensorConf);
        ISensorDataInterface sensorOutput = sensor.getAllOutputs().values().iterator().next();
        DataComponent outputDef = sensorOutput.getRecordDescription();
                
        IStreamingDataInterface processOutput = p.getAllOutputs().values().iterator().next();
        IEventListener l = new IEventListener() {
            public void handleEvent(Event<?> e)
            {
                DataBlock data = ((DataEvent)e).getRecords()[0];
                double lat = data.getDoubleValue(1);
                double lon = data.getDoubleValue(2);
                double alt = data.getDoubleValue(3);
                System.out.println(lat + "," + lon + "," + alt);
            }
        };
        processOutput.registerListener(l);
        
        DataBlock dataBlk = outputDef.createDataBlock();
        long now = System.currentTimeMillis();
        double range = 10.0;
        double az = 90.0;
        double inc = 0.0;
        dataBlk.setDoubleValue(0, now / 1000.);
        dataBlk.setDoubleValue(2, range);
        dataBlk.setDoubleValue(3, az);
        dataBlk.setDoubleValue(4, inc);
        p.rangeMeasQueue.add(new DataBlockFloat());
        p.process(new SensorDataEvent(now, sensorOutput, dataBlk));*/
    }
}
