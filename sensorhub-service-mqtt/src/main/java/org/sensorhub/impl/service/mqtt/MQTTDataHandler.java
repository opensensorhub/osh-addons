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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataStream;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.sensorhub.api.common.Event;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.vast.cdm.common.DataStreamWriter;
import org.vast.swe.SWEHelper;
import org.vast.swe.SWEUtils;
import org.vast.xml.XMLWriterException;


public class MQTTDataHandler implements IEventListener
{
    MqttAsyncClient client;
    String topic;
    WeakReference<IStreamingDataInterface> sourceRef;
    ByteArrayOutputStream tempOs = new ByteArrayOutputStream();
    DataStreamWriter dataWriter;
    
    
    public MQTTDataHandler(MqttAsyncClient client, String topic, IStreamingDataInterface dataSource)
    {
        this.client = client;
        this.topic = topic;
        this.sourceRef = new WeakReference<IStreamingDataInterface>(dataSource);
    }
    
    
    public void start() throws SensorHubException
    {
        IStreamingDataInterface dataSource = sourceRef.get();
        if (dataSource == null)
            throw new SensorHubException("Data source is not available");
        
        // publish stream description 
        try
        {
            DataComponent dataStruct = dataSource.getRecordDescription();
            DataEncoding dataEncoding = dataSource.getRecommendedEncoding();
            DataStream ds = new SWEHelper().newDataStream(dataStruct, dataEncoding);
                        
            // prepare writer for sending data messages
            dataWriter = SWEHelper.createDataWriter(dataEncoding);
            dataWriter.setDataComponents(dataStruct);
            dataWriter.setOutput(tempOs);
            
            // publish description message with retain flag
            new SWEUtils(SWEUtils.V2_0).writeDataStream(tempOs, ds, true);
            MqttMessage msg = new MqttMessage(tempOs.toByteArray());
            msg.setRetained(true);
            client.publish(topic, msg);
            
            MQTTSenderService.log.debug("Record description published on topic " + topic);
        }
        catch (MqttException e)
        {
            e.printStackTrace();
        }
        catch (XMLWriterException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        
        dataSource.registerListener(this);
    }
    
    
    public void stop()
    {
        IStreamingDataInterface dataSource = sourceRef.get();
        if (dataSource != null)
            dataSource.unregisterListener(this);
    }


    @Override
    public void handleEvent(Event<?> e)
    {
        if (e instanceof DataEvent)
        {
            try
            {
                for (DataBlock dataBlk: ((DataEvent) e).getRecords())
                {
                    tempOs.reset();
                    dataWriter.write(dataBlk);
                    dataWriter.flush();
                    
                    MqttMessage msg = new MqttMessage(tempOs.toByteArray());
                    client.publish(topic, msg);
                    
                    MQTTSenderService.log.trace("Data published on topic " + topic);
                }
            }
            catch (IOException ex)
            {
                // TODO Auto-generated catch block
                ex.printStackTrace();
            }
            catch (MqttException ex)
            {
                // TODO Auto-generated catch block
                ex.printStackTrace();
            }
        }        
    }
    
}
