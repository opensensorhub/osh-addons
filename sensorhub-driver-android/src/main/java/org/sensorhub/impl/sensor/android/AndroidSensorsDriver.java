/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Sensia Software LLC. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.android;

import java.util.List;
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
    
    
    public AndroidSensorsDriver()
    {
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
                    addOutput(new AndroidAcceleroOutput(this, sensorManager, sensor), false);
                    break;
                    
                case Sensor.TYPE_GYROSCOPE:
                    addOutput(new AndroidGyroOutput(this, sensorManager, sensor), false);
                    break;
                
                case Sensor.TYPE_MAGNETIC_FIELD:
                    addOutput(new AndroidMagnetoOutput(this, sensorManager, sensor), false);
                    break;
                    
                case Sensor.TYPE_ROTATION_VECTOR:
                    addOutput(new AndroidOrientationOutput(this, sensorManager, sensor), false);
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
                addOutput(new AndroidLocationOutput(this, locationManager, locProvider), false);
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
                    if (cameraManager.getCameraCharacteristics(cameraId).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK)
                        addOutput(new AndroidCameraOutput(this, cameraManager, cameraId), false);
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
            localRefFrame.addAxis("x", "");
            localRefFrame.addAxis("y", "");
            localRefFrame.addAxis("z", "");
            ((PhysicalSystem)sensorDescription).addLocalReferenceFrame(localRefFrame);
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
