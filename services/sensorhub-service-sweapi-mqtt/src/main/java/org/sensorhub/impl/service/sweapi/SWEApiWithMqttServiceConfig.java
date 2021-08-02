/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sweapi;

import org.sensorhub.api.config.DisplayInfo;


/**
 * <p>
 * Configuration class for the SWE API service module with MQTT support
 * </p>
 *
 * @author Alex Robin
 * @since Jul 28, 2021
 */
public class SWEApiWithMqttServiceConfig extends SWEApiServiceConfig
{
    
    @DisplayInfo(desc="Set to true to enable MQTT support")
    public boolean enableMqtt = true;
}
