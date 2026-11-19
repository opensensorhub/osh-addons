package org.sensorhub.impl.process.video;

import net.opengis.swe.v20.DataBlock;
import org.sensorhub.impl.process.video.transcoder.FFmpegTranscoderConfig;
import org.vast.data.DataBlockString;
import org.vast.process.IProcessExec;

public class FFmpegDecoderProcess extends FFmpegProcess {
    @Override
    public void initExcProcess(IProcessExec executable) {
        DataBlock inCodec = new DataBlockString(1);
        inCodec.setStringValue(((FFmpegDecoderConfig)config).inCodec.toString());

        //DataBlock outFormat = new DataBlockString(1);
        //outFormat.setStringValue(((FFmpegTranscoderConfig)config).outCodec.toString());

        executable.getParameterList().getComponent("codec").setData(inCodec);
        //executable.getParameterList().getComponent("outCodec").setData(outFormat);
    }
}
