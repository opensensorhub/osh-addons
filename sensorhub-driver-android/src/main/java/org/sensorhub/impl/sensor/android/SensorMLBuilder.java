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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.SWEFactory;
import org.vast.sensorML.SMLFactory;
import net.opengis.sensorml.v20.PhysicalComponent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraManager;
import android.location.LocationManager;
import android.location.LocationProvider;


public class SensorMLBuilder
{
    // keep logger name short because in LogCat it's max 23 chars
    protected static final Logger log = LoggerFactory.getLogger(SensorMLBuilder.class.getSimpleName());
    
    SMLFactory smlFac = new SMLFactory();
    SWEFactory sweFac = new SWEFactory();
    
    
    public PhysicalComponent getComponentDescription(SensorManager aSensorManager, Sensor aSensor)
    {
        PhysicalComponent comp = smlFac.newPhysicalComponent();
        comp.setId("SENSOR_" + formatId(aSensor.getName()));
        comp.setName(aSensor.getName());        
        return comp;
    }
    
    
    public PhysicalComponent getComponentDescription(LocationManager locManager, LocationProvider locProvider)
    {
        PhysicalComponent comp = smlFac.newPhysicalComponent();
        comp.setId("LOC_" + formatId(locProvider.getName()));
        comp.setName(locProvider.getName());
        return comp;
    }
    
    
    public PhysicalComponent getComponentDescription(CameraManager camManager, String cameraId)
    {
        PhysicalComponent comp = smlFac.newPhysicalComponent();
        comp.setId("CAM_" + formatId(cameraId));
        comp.setName("Android Camera #" + cameraId);
        
        /*try
        {
            CameraCharacteristics camOpts = camManager.getCameraCharacteristics(cameraId);
            CharacteristicList charList = smlFac.newCharacteristicList();
                        
            for (Key<?> key: camOpts.getKeys())
            {
                Text ch = sweFac.newText();
                ch.setName(key.getName());
                
                Object val = camOpts.get(key);
                String textVal;
                if (val instanceof int[])
                    textVal = Arrays.toString((int[])val);
                else if (val instanceof float[])
                    textVal = Arrays.toString((float[])val);
                else if (val instanceof Object[])
                    textVal = Arrays.toString((Object[])val);
                else
                    textVal = val.toString();
                
                //log.debug(key.getName() + "=" + textVal);
                ch.setValue(textVal);
                charList.addCharacteristic(key.getName(), ch);
            }
            
            comp.addCharacteristics("camera_options", charList);
            
        }
        catch (CameraAccessException e)
        {
            log.error("Cannot access camera " + cameraId);
        }*/
        
        return comp;
    }
    
    
    protected String formatId(String name)
    {
        return name.toUpperCase().replace(" ", "_");
    }
}
