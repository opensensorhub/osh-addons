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

import org.sensorhub.api.sensor.SensorConfig;


/**
 * <p>
 * Configuration class for the generic Android sensors driver
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Sep 6, 2013
 */
public class AndroidSensorsConfig extends SensorConfig
{
    private static final long serialVersionUID = -8246835851102755538L;
    
    
    public boolean activateAccelerometer = false;
    public boolean activateGyrometer = false;
    public boolean activateMagnetometer = false;
    public boolean activateOrientation = true;
    public boolean activateGpsLocation = true;
    public boolean activateNetworkLocation = false;
    public boolean activateBackCamera = false;
    public boolean activateFrontCamera = false;
    
    
    public String sosEndpoint = "http://192.168.0.10:8080/sensorhub/sos";

}
