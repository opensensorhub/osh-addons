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
    @DisplayInfo.Required
    @DisplayInfo(label="Node ID", desc="Prefix of all MQTT topics, as in '{nodeId}/systems/{id}'. "
        + "It must be one topic level: no '/', '+', '#' or NUL, and it must not start with '$'. "
        + "Use a different value for each CS API server that shares the MQTT broker.")
    public String nodeId = "api";

    @DisplayInfo(label="Batch Event Window (ms)", desc="Length of the time window for the Batch Resource Events "
        + "of observations. Each datastream gets at most one event in each window. After a quiet window, the first "
        + "event is published at once. Set 0 to publish one event each time the datastream gets observations.")
    public int batchEventWindowMs = 1000;

    @DisplayInfo(label="Broker Address", desc="Broker host and port (MQTT over TCP) to advertise in the AsyncAPI description, "
        + "as in 'broker.example.org:1883'. Leave empty to use the HTTP host and port 1883, the port of the bundled "
        + "HiveMQ config. Set it if the node's HiveMQ config uses another port.")
    public String brokerAddress;

    @DisplayInfo(label="WebSocket Address", desc="Broker address for MQTT over WebSocket to advertise in the AsyncAPI description, "
        + "as in 'broker.example.org:8083/mqtt' or 'wss://broker.example.org/mqtt'. Leave empty to advertise TCP only.")
    public String webSocketAddress;

    @DisplayInfo(label="AsyncAPI Viewer URL", desc="Base URL of the AsyncAPI viewer (@asyncapi/react-component) "
        + "used by the HTML page of the AsyncAPI description. Leave empty to show only the YAML text.")
    public String asyncApiViewerUrl = "https://unpkg.com/@asyncapi/react-component@3.2.2";
}
