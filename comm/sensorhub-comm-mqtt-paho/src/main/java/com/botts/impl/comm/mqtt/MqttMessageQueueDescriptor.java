package com.botts.impl.comm.mqtt;

import org.sensorhub.api.module.*;
import org.sensorhub.impl.module.JarModuleProvider;

public class MqttMessageQueueDescriptor extends JarModuleProvider implements IModuleProvider
{
    @Override
    public String getModuleName()
    {
        return "Mqtt Message Queue";
    }


    @Override
    public String getModuleDescription()
    {
        return "Communication protocol that can publish/subscribe to MQTT topics";
    }


    @Override
    public Class<? extends IModuleBase<?>> getModuleClass()
    {
        return MqttMessageQueue.class;
    }


    @Override
    public Class<? extends ModuleConfigBase> getModuleConfigClass()
    {
        return MqttMessageQueueConfig.class;
    }
}
