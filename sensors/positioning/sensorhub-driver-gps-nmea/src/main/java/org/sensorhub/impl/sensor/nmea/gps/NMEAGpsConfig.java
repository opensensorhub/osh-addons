/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.nmea.gps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.sensorhub.api.comm.CommProviderConfig;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.config.DisplayInfo.Required;
import org.sensorhub.api.sensor.SensorConfig;


public class NMEAGpsConfig extends SensorConfig
{    
    
    @Required
    @DisplayInfo(desc="Sensor serial number (used as suffix to generate unique identifier URI)")
    public String serialNumber = null;
    
    
    @DisplayInfo(desc="Communication settings to connect to NMEA GPS data stream")
    public CommProviderConfig<?> commSettings;
    
    
    @DisplayInfo(label="Active NMEA Messages", desc="List of NMEA sentences to provide as outputs")
    public List<String> activeSentences = new ArrayList<String>();
    
    
    public NMEAGpsConfig()
    {
        this.moduleClass = NMEAGpsSensor.class.getCanonicalName();
        this.activeSentences.add("GGA");
    }
}
