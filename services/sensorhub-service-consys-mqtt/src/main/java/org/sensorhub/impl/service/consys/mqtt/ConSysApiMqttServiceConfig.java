/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.consys.mqtt;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.service.ServiceConfig;


/**
 * <p>
 * Configuration class for the Connected Systems API MQTT service module
 * </p>
 *
 * @author Alex Robin
 * @since May 9, 2023
 */
public class ConSysApiMqttServiceConfig extends ServiceConfig
{
    @DisplayInfo(label="Node ID",
        desc="MQTT topic namespace prefix for all topics. Per OGC CS API Part 3, topics become " +
             "'{nodeId}/systems/{id}' and '{nodeId}/systems/{id}/...:data'. " +
             "Defaults to 'api' so topics are 'api/systems/...'. " +
             "Set to null or blank to use the HTTP endpoint path as prefix instead.")
    public String nodeId = "api";

    @DisplayInfo(label="CS API Base URL",
        desc="Public CS API root URL used as the CloudEvents 'source' field " +
             "(e.g. 'https://example.org/sensorhub/api'). " +
             "If null, derived automatically from the ConSysApiService public endpoint.")
    public String csApiBaseUrl = null;
}