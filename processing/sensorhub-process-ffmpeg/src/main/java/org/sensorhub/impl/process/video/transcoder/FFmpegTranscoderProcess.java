package org.sensorhub.impl.process.video.transcoder;

import net.opengis.swe.v20.BinaryBlock;
import net.opengis.swe.v20.BinaryEncoding;
import net.opengis.swe.v20.BinaryMember;
import net.opengis.swe.v20.DataBlock;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.process.video.FFmpegProcess;
import org.vast.data.DataBlockString;
import org.vast.process.IProcessExec;

public class FFmpegTranscoderProcess extends FFmpegProcess {

    public FFmpegTranscoderProcess() {
        super();
    }

    @Override
    public void initExcProcess(IProcessExec executable) {
        DataBlock inCodec = new DataBlockString(1);
        inCodec.setStringValue(((FFmpegTranscoderConfig)config).inCodec.toString());

        DataBlock outCodec = new DataBlockString(1);
        outCodec.setStringValue(((FFmpegTranscoderConfig)config).outCodec.toString());

        executable.getParameterList().getComponent("inCodec").setData(inCodec);
        executable.getParameterList().getComponent("outCodec").setData(outCodec);
    }

    @Override
    public void afterInit() throws SensorHubException {
        var castConf = ((FFmpegTranscoderConfig)config);

        if (castConf.detectInput) {
            if (inputVideoOutport.getRecommendedEncoding() instanceof BinaryEncoding enc) {
                for (BinaryMember m: ((BinaryEncoding)enc).getMemberList())
                {
                    if (m instanceof BinaryBlock)
                    {
                        DataBlock inCodec = new DataBlockString(1);
                        inCodec.setStringValue(((BinaryBlock) m).getCompression());
                        executable.getParameterList().getComponent("inCodec").setData(inCodec);
                        //videoOutput.setStructCompression(inCodec.getStringValue());
                        break;
                    }
                }
            }
        }
        videoOutput.setStructCompression(((FFmpegTranscoderConfig) config).outCodec.toString());
        super.afterInit();
    }
}
