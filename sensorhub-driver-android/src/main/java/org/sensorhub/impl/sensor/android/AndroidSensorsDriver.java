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

import java.util.ArrayList;
import java.util.List;
import net.opengis.sensorml.v20.PhysicalComponent;
import net.opengis.sensorml.v20.PhysicalSystem;
import net.opengis.sensorml.v20.SpatialFrame;
import net.opengis.sensorml.v20.impl.SpatialFrameImpl;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;


public class AndroidSensorsDriver extends AbstractSensorModule<AndroidSensorsConfig>
{
    // keep logger name short because in LogCat it's max 23 chars
    private static final Logger log = LoggerFactory.getLogger(AndroidSensorsDriver.class.getSimpleName());
    public static final String LOCAL_REF_FRAME = "LOCAL_FRAME";
    
    public static Context androidContext;
    SensorManager sensorManager;
    LocationManager locationManager;
    CameraManager cameraManager;
    SensorMLBuilder smlBuilder;
    List<PhysicalComponent> smlComponents;
    
    
    public AndroidSensorsDriver()
    {
        smlComponents = new ArrayList<PhysicalComponent>();
        smlBuilder = new SensorMLBuilder();
    }
    
    
    @Override
    public void start() throws SensorException
    {
        // we call stop() to cleanup just in case we weren't properly stopped
        stop();        
        
        // create data interfaces for sensors
        this.sensorManager = (SensorManager)androidContext.getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> deviceSensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor sensor: deviceSensors)
        {
            log.debug("Detected sensor " + sensor.getName());
            
            switch (sensor.getType())
            {
                case Sensor.TYPE_ACCELEROMETER:
                    if (config.activateAccelerometer)
                        useSensor(new AndroidAcceleroOutput(this, sensorManager, sensor), sensor);                        
                    break;
                    
                case Sensor.TYPE_GYROSCOPE:
                    if (config.activateGyrometer)
                        useSensor(new AndroidGyroOutput(this, sensorManager, sensor), sensor);
                    break;
                
                case Sensor.TYPE_MAGNETIC_FIELD:
                    if (config.activateMagnetometer)
                        useSensor(new AndroidMagnetoOutput(this, sensorManager, sensor), sensor);
                    break;
                    
                case Sensor.TYPE_ROTATION_VECTOR:
                    if (config.activateOrientationQuat)
                        useSensor(new AndroidOrientationQuatOutput(this, sensorManager, sensor), sensor);
                    if (config.activateOrientationEuler)
                        useSensor(new AndroidOrientationEulerOutput(this, sensorManager, sensor), sensor);
                    break;
            }
        }
        
        // create data interfaces for location providers
        if (androidContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION))
        {
            this.locationManager = (LocationManager)androidContext.getSystemService(Context.LOCATION_SERVICE);
            
            List<String> locProviders = locationManager.getAllProviders();
            for (String provName: locProviders)
            {
                log.debug("Detected location provider " + provName);
                LocationProvider locProvider = locationManager.getProvider(provName);
                
                // keep only GPS for now
                if ( (locProvider.requiresSatellite() && config.activateGpsLocation) ||
                     (locProvider.requiresNetwork() && config.activateNetworkLocation))
                    useLocationProvider(new AndroidLocationOutput(this, locationManager, locProvider), locProvider);
            }
        }
        
        // create data interfaces for cameras
        if (androidContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA))
        {
            this.cameraManager = (CameraManager)androidContext.getSystemService(Context.CAMERA_SERVICE);
            
            try
            {
                String[] camIds = cameraManager.getCameraIdList();
                for (String cameraId: camIds)
                {
                    log.debug("Detected camera " + cameraId);
                    int camDir = cameraManager.getCameraCharacteristics(cameraId).get(CameraCharacteristics.LENS_FACING);
                    if ( (camDir == CameraCharacteristics.LENS_FACING_BACK && config.activateBackCamera) ||
                         (camDir == CameraCharacteristics.LENS_FACING_FRONT && config.activateFrontCamera))
                         useCamera(new AndroidCameraOutput(this, cameraManager, cameraId), cameraId);
                }
            }
            catch (CameraAccessException e)
            {
                throw new SensorException("Error while accessing cameras", e);
            }        
        }
        
        // init all outputs
        for (ISensorDataInterface o: this.getAllOutputs().values())
            ((IAndroidOutput)o).init();
        
        // update sensorml description
        updateSensorDescription();
    }
    
    
    protected void useSensor(ISensorDataInterface output, Sensor sensor)
    {
        addOutput(output, false);
        smlComponents.add(smlBuilder.getComponentDescription(sensorManager, sensor));
        log.info("Getting data from " + sensor.getName() + " sensor");
    }
    
    
    protected void useLocationProvider(ISensorDataInterface output, LocationProvider locProvider)
    {
        addOutput(output, false);
        smlComponents.add(smlBuilder.getComponentDescription(locationManager, locProvider));
        log.info("Getting data from " + locProvider.getName() + " location provider");
    }
    
    
    protected void useCamera(ISensorDataInterface output, String cameraId)
    {
        addOutput(output, false);
        smlComponents.add(smlBuilder.getComponentDescription(cameraManager, cameraId));
        log.info("Getting data from camera #" + cameraId);
    }
    
    
    @Override
    public void stop() throws SensorException
    {
        // stop all outputs
        for (ISensorDataInterface o: this.getAllOutputs().values())
            ((IAndroidOutput)o).stop();
        
        this.removeAllOutputs();
        this.removeAllControlInputs();
    }


    @Override
    protected void updateSensorDescription() throws SensorException
    {
        synchronized (sensorDescription)
        {
            super.updateSensorDescription();
            sensorDescription.setId("ANDROID_SENSORS");
            sensorDescription.setUniqueIdentifier("urn:android:device:" + Build.SERIAL);
            
            SpatialFrame localRefFrame = new SpatialFrameImpl();
            localRefFrame.setId("LOCAL_FRAME");
            localRefFrame.setOrigin("Center of the device screen");
            localRefFrame.addAxis("x", "The X axis is in the plane of the screen and points to the right");
            localRefFrame.addAxis("y", "The Y axis is in the plane of the screen and points up");
            localRefFrame.addAxis("z", "The Z axis points towards the outside of the front face of the screen");
            ((PhysicalSystem)sensorDescription).addLocalReferenceFrame(localRefFrame);
            
            // add components
            int index = 0;
            for (PhysicalComponent comp: smlComponents)
            {
                String name = "sensor" + index++;
                ((PhysicalSystem)sensorDescription).addComponent(name, comp);
            }
        }
    }


    @Override
    public boolean isConnected()
    {
        return true;
    }
    
    
    @Override
    public void cleanup() throws SensorHubException
    {     
    }
}
