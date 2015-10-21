package org.sensorhub.impl.sensor.plume;
/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

import org.sensorhub.api.sensor.SensorConfig;

public class PlumeConfig extends SensorConfig
{    
	public static String plumePath = "C:/Data/sensorhub/plume/ppdm_ind_ms_v2/run/PARTICLE.OUT_p0"; // not sure why this isn't getting loaded from config file
}
