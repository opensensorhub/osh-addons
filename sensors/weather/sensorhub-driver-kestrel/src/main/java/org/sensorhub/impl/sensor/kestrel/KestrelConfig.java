/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.kestrel;

import org.sensorhub.api.comm.CommProviderConfig;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.impl.module.RobustConnection;
import org.sensorhub.impl.module.RobustConnectionConfig;


/**
 *
 * @author Kalyn Stricklin
 * @since Dec 1, 2025
 */
public class KestrelConfig extends SensorConfig
{
    public KestrelConfig() { this.moduleClass = Kestrel.class.getCanonicalName(); }

    @DisplayInfo(label="Serial Number", desc="Kestrel Weather Meter Serial Number")
    public String serialNumber;

    @DisplayInfo(label="Device Address", desc="Kestrel Weather Meter Device Address")
    public String deviceAddress;

    @DisplayInfo(label="Bluetooth Network", desc="Local ID of Bluetooth LE network to use")
    public String networkID;

}
