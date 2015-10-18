/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.mqtt;

import java.util.ArrayList;
import java.util.List;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.service.ServiceConfig;


/**
 * <p>
 * Configuration class for the MQTT service module
 * </p>
 *
 * <p>Copyright (c) 2015 Sensia Software LLC</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Oct 15, 2015
 */
public class MQTTSenderConfig extends ServiceConfig
{
    
    public static class MQTTDataSourceConfig
    {
        @DisplayInfo(desc="Local ID of streaming data provider")
        public String streamSourceID;
        
        @DisplayInfo(desc="Set to false to temporarily disable this MQTT stream")
        public boolean enabled = true;
        
        @DisplayInfo(label="Sub topic", desc="Full topic name will be {topicPrefix}/{outputName}")
        public String topicPrefix;
        
        public List<String> hiddenOutputs = new ArrayList<String>();
    }
    
    
    @DisplayInfo(label="Broker URL", desc="URL of MQTT broker where to send messages and subscribe to data streams")
    public String brokerUrl;
    
    
    public List<MQTTDataSourceConfig> dataSources = new ArrayList<MQTTDataSourceConfig>();
    
    
    public MQTTSenderConfig()
    {
        this.moduleClass = MQTTSenderService.class.getCanonicalName();
    }
}
