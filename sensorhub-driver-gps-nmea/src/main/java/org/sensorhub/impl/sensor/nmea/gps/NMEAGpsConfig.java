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

import java.util.Arrays;
import java.util.List;
import org.sensorhub.api.comm.CommConfig;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.SensorConfig;


public class NMEAGpsConfig extends SensorConfig
{    
    
    @DisplayInfo(label="Communication Settings", desc="Settings for selected communication port")
    public CommConfig commSettings;
    
    
    @DisplayInfo(label="Active NMEA Messages", desc="List of NMEA sentences to provide as outputs")
    public List<String> activeSentences = Arrays.asList("GGA");
}
