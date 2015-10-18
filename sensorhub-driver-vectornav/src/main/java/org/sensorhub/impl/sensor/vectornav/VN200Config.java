/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.vectornav;

import org.sensorhub.api.comm.CommConfig;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.impl.comm.RS232Config;


public class VN200Config extends SensorConfig
{
    
    @DisplayInfo(label="Communication Settings", desc="Settings for selected communication port")
    public CommConfig commSettings;
    
    
    @DisplayInfo(label="Attitude Sampling Rate", desc="Desired attitude sampling rate divider from the base 800Hz frequency (e.g. 8 to get output at 100Hz)")
    public int attSamplingFactor = 16;
    
    
    @DisplayInfo(label="GPS Sampling Rate", desc="Desired GPS sampling rate divider from the base 800Hz frequency (e.g. 80 to get output at 10Hz)")
    public int gpsSamplingFactor = 80;
    
    
    public VN200Config()
    {
        this.moduleClass = VN200Sensor.class.getCanonicalName();
        
        RS232Config serialConf = new RS232Config();
        serialConf.moduleClass = "org.sensorhub.impl.comm.rxtx.RxtxSerialCommProvider";
        serialConf.portName = "/dev/ttyUSB0";
        serialConf.baudRate = 115200;
        this.commSettings = serialConf;
    }
}
