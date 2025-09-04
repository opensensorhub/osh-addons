package org.sensorhub.impl.process.video;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.process.video.transcoder.FFMpegTranscoder;
import org.sensorhub.impl.process.video.transcoder.FFmpegTranscoderConfig;
import org.sensorhub.impl.process.video.transcoder.FFmpegTranscoderProcess;
import org.sensorhub.impl.processing.AbstractProcessProvider;

public class FFmpegDecoderDescriptor  extends AbstractProcessProvider
{

    public FFmpegDecoderDescriptor()
    {
        addImpl(FFMpegDecoder.INFO);
    }


    @Override
    public String getModuleName()
    {
        return "FFmpeg Decoder";
    }


    @Override
    public String getModuleDescription()
    {
        return "Video decoder using FFmpeg library.";
    }

    // TODO Set these two after creating module and module config
    @Override
    public Class<? extends IModule<?>> getModuleClass()
    {
        return FFmpegDecoderProcess.class;
    }


    @Override
    public Class<? extends ModuleConfig> getModuleConfigClass()
    {
        return FFmpegDecoderConfig.class;
    }

}
