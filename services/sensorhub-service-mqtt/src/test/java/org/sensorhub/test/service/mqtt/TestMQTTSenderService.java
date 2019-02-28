/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.service.mqtt;

import static org.junit.Assert.assertEquals;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.SensorHubConfig;
import org.sensorhub.impl.service.mqtt.MQTTSenderConfig;
import org.sensorhub.impl.service.mqtt.MQTTSenderConfig.MQTTDataSourceConfig;
import org.sensorhub.impl.service.mqtt.MQTTSenderService;
import org.sensorhub.test.sensor.FakeSensor;
import org.sensorhub.test.sensor.FakeSensorData;


public class TestMQTTSenderService
{
    static String NAME_OUTPUT1 = "weatherOut";
    static String NAME_OUTPUT2 = "imageOut";
    static String UID_SENSOR1 = "urn:sensors:mysensor:001";
    static String UID_SENSOR2 = "urn:sensors:mysensor:002";
    static String MQTT_BROKER_URL = "tcp://iot.eclipse.org:1883";
    static final double SAMPLING_PERIOD = 1.0;
    static final int NUM_GEN_SAMPLES = 10;
    static final String DB_PATH = "db.dat";
    
    
    Map<Integer, Integer> obsFoiMap = new HashMap<Integer, Integer>();
    File configFile;
    
    
    @Before
    public void setupFramework() throws Exception
    {
        // init sensorhub
        configFile = new File("junit-test.json");
        //configFile = File.createTempFile("junit-config-", ".json");
        configFile.deleteOnExit();
        new File(DB_PATH).deleteOnExit();
        SensorHub.createInstance(new SensorHubConfig(configFile.getAbsolutePath(), configFile.getParent()));
    }
    
    
    protected MQTTSenderService deployService(MQTTDataSourceConfig... dataSrcConfigs) throws Exception
    {   
        // create service config
        MQTTSenderConfig serviceCfg = new MQTTSenderConfig();
        serviceCfg.brokerUrl = MQTT_BROKER_URL;
        serviceCfg.enabled = true;
        serviceCfg.name = "MQTT Output";        
        for (MQTTDataSourceConfig srcConfig: dataSrcConfigs)
            serviceCfg.dataSources.add(srcConfig);
        
        // load module into registry
        MQTTSenderService mqtt = (MQTTSenderService)SensorHub.getInstance().getModuleRegistry().loadModule(serviceCfg);
        return mqtt;
    }
    
    
    protected MQTTDataSourceConfig buildSensorProvider1() throws Exception
    {
        // create test sensor
        SensorConfig sensorCfg = new SensorConfig();
        sensorCfg.enabled = true;
        sensorCfg.moduleClass = FakeSensor.class.getCanonicalName();
        sensorCfg.name = "Sensor1";
        FakeSensor sensor = (FakeSensor)SensorHub.getInstance().getModuleRegistry().loadModule(sensorCfg);
        sensor.setSensorUID(UID_SENSOR1);
        sensor.setDataInterfaces(new FakeSensorData(sensor, NAME_OUTPUT1, 10, SAMPLING_PERIOD, NUM_GEN_SAMPLES));
        //sensor.setDataInterfaces(new FakeSensorData(sensor, NAME_OUTPUT2, 10, SAMPLING_PERIOD, NUM_GEN_SAMPLES));
        
        // create SOS data provider config
        MQTTDataSourceConfig provCfg = new MQTTDataSourceConfig();
        provCfg.enabled = true;
        provCfg.topicPrefix = UID_SENSOR1;
        provCfg.streamSourceID = sensor.getLocalID(); 
        
        return provCfg;
    }
    
    
    protected void subscribe(String topic)
    {
        try
        {
            MqttClient client = new MqttClient(MQTT_BROKER_URL, "sensorhub-test", new MemoryPersistence());
                        
            // message receive callback
            client.setCallback(new MqttCallback()
            {
                @Override
                public void connectionLost(Throwable e)
                {
                    System.out.println("Connection lost");
                    e.printStackTrace();
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken arg0)
                {                                        
                }

                @Override
                public void messageArrived(String topic, MqttMessage msg) throws Exception
                {
                    System.out.println("Message received for topic " + topic + ": " +
                            new String(msg.getPayload()));
                }                
            });
            
            // connect and subscribe to receive messages back from broker
            client.connect();
            client.subscribe(topic, 1);
        }
        catch (MqttException e)
        {
            e.printStackTrace();
        }
    }
    
    
    @Test
    public void testConnect() throws Exception
    {
        deployService(buildSensorProvider1());
        Thread.sleep(2000); 
        subscribe(UID_SENSOR1 + "/" + NAME_OUTPUT1);
        Thread.sleep(5000);      
    }
    
   
    @After
    public void cleanup()
    {
        try
        {
            SensorHub.getInstance().getModuleRegistry().shutdown(false, false);
            if (configFile != null)
                configFile.delete();
            File dbFile = new File(DB_PATH);
            if (dbFile.exists())
                dbFile.delete();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
