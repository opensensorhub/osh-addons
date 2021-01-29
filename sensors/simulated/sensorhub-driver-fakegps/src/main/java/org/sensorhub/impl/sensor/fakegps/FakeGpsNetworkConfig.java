/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.fakegps;

import org.sensorhub.api.common.SensorHubException;


public class FakeGpsNetworkConfig extends FakeGpsConfig
{    
    public int numRoutes = 5;
    public int numAssetsPerRoute = 3;
    
    
    protected void validate() throws SensorHubException
    {
        super.validate();
        
        if (numRoutes <= 0 || numRoutes > 10)
            throw new SensorHubException(String.format("Invalid number of routes: %d", numRoutes));
        
        if (numAssetsPerRoute <= 0 || numAssetsPerRoute > 100)
            throw new SensorHubException(String.format("Invalid number of assets: %d", numAssetsPerRoute));
    }
}
