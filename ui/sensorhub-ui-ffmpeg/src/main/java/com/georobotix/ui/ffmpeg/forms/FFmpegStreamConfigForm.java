package com.georobotix.ui.ffmpeg.forms;

import org.sensorhub.ui.GenericConfigForm;
import org.sensorhub.ui.data.BaseProperty;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class FFmpegStreamConfigForm extends GenericConfigForm {

    private static final String STREAM_CONFIG_PACKAGE = "org.sensorhub.impl.sensor.ffmpeg.config.streamfilter.";
    private static final String PROP_STREAMFILTER = "connection.streamFilter";

    @Override
    public Map<String, Class<?>> getPossibleTypes(String propId, BaseProperty<?> prop)
    {
        if (propId.equals(PROP_STREAMFILTER))
        {
            Map<String, Class<?>> classList = new LinkedHashMap<>();
            try
            {
                classList.put("Max Count", Class.forName(STREAM_CONFIG_PACKAGE + "MaxCountFilter"));
                classList.put("Stream Index", Class.forName(STREAM_CONFIG_PACKAGE + "IndexFilter"));
            }
            catch (ClassNotFoundException e)
            {
                getOshLogger().error("Cannot find RPM class", e);
            }
            return classList;
        }
        return super.getPossibleTypes(propId, prop);
    }
}
