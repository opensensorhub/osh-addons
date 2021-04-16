/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sta;

import java.nio.ByteBuffer;
import java.util.regex.Pattern;
import org.sensorhub.api.comm.mqtt.IMqttService.IMqttHandler;
import com.google.common.base.Charsets;
import de.fraunhofer.iosb.ilt.frostserver.service.RequestType;
import de.fraunhofer.iosb.ilt.frostserver.service.Service;
import de.fraunhofer.iosb.ilt.frostserver.service.ServiceRequestBuilder;
import de.fraunhofer.iosb.ilt.frostserver.settings.CoreSettings;


/**
 * <p>
 * This class handles communication with the embedded MQTT server and transfers
 * messages to/from the STA service for processing. 
 * </p>
 *
 * @author Alex Robin
 * @since Apr 14, 2021
 */
public class STAMqttConnector implements IMqttHandler
{
    STAService service;
    String endpoint;
    CoreSettings coreSettings;
    Pattern topicRegex;
    Service frostService;
    
    
    public STAMqttConnector(STAService service, String endpoint, CoreSettings coreSettings)
    {
        this.service = service;
        this.endpoint = endpoint;
        this.coreSettings = coreSettings;
        this.topicRegex = Pattern.compile(endpoint + 
            ".*(Things|Sensors|Datastreams|MultiDatastreams|Observations|FeaturesOfInterest)");
        this.frostService = new Service(coreSettings);
    }


    @Override
    public boolean isValidTopic(String topic)
    {
        return topicRegex.matcher(topic).matches();
    }


    @Override
    public boolean subscribe(String topic)
    {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean publish(String topic, ByteBuffer payload)
    {
        var collectionUrl = "/" + topic.replaceFirst(endpoint, "");
        
        var req = new ServiceRequestBuilder(coreSettings.getFormatter())
            .withRequestType(RequestType.CREATE)
            .withUrlPath(collectionUrl)
            .withContent(Charsets.UTF_8.decode(payload).toString())
            .build();                
        
        var resp = frostService.execute(req);
        if (!resp.isSuccessful())
            throw new IllegalArgumentException("Invalid SensorThings topic or payload: " + resp.getMessage(), null);
        
        return true;
    }
}
