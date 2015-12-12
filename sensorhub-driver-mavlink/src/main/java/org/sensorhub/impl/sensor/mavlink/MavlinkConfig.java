/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.mavlink;

import java.util.ArrayList;
import java.util.List;
import org.sensorhub.api.comm.CommConfig;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.SensorConfig;


public class MavlinkConfig extends SensorConfig
{
    public enum MavlinkMsg
    {
        GLOBAL_POSITION,
        GPS_RAW_INT,
        GPS_STATUS,
        ATTITUDE,
        ATTITUDE_QUATERNION,
        BATTERY_STATUS,
        RADIO_STATUS,
        GIMBAL_REPORT
    }    
    
    public enum MavlinkCmd
    {
        MOUNT_CONFIGURE,
        MOUNT_CONTROL,
        NAV_WAYPOINT,
        NAV_RETURN_TO_LAUNCH,
    }
        
    @DisplayInfo(desc="MAVLink messages to expose through this sensor interface")
    public List<MavlinkMsg> activeMessages = new ArrayList<MavlinkMsg>(3);
    
    @DisplayInfo(desc="MAVLink commands to expose through this sensor interface")
    public List<MavlinkCmd> activeCommands = new ArrayList<MavlinkCmd>(3);
    
    @DisplayInfo(desc="Communication settings to access MAVLink data")
    public CommConfig commSettings;
}
