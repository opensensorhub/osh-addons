package org.sensorhub.impl.process.video.transcoder;

import net.opengis.swe.v20.*;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.process.video.FFmpegProcess;
import org.vast.data.CountImpl;
import org.vast.data.DataBlockInt;
import org.vast.data.DataBlockString;
import org.vast.process.IProcessExec;

public class FFmpegTranscoderProcess extends FFmpegProcess {

    Count outWidth, outHeight;

    public FFmpegTranscoderProcess() {
        super();
    }

    @Override
    public void initExcProcess(IProcessExec executable) {
        DataBlock inCodec = new DataBlockString(1);
        inCodec.setStringValue(((FFmpegTranscoderConfig)config).inCodec.toString());

        DataBlock outCodec = new DataBlockString(1);
        outCodec.setStringValue(((FFmpegTranscoderConfig)config).outCodec.toString());

        DataBlockInt outWidthBlock = new DataBlockInt(1);
        if (((FFmpegTranscoderConfig) config).outputWidth != null) {
            outWidthBlock.setIntValue(((FFmpegTranscoderConfig) config).outputWidth);
        } else {
            outWidthBlock.setIntValue(-1);
        }
        //outWidth = new CountImpl();
        //outWidth.setValue(outWidthBlock.getIntValue());
        outWidth = smlHelper.createCount().value(outWidthBlock.getIntValue()).build();

        DataBlockInt outHeightBlock = new DataBlockInt(1);
        if (((FFmpegTranscoderConfig) config).outputHeight != null) {
            outHeightBlock.setIntValue(((FFmpegTranscoderConfig) config).outputHeight);
        } else {
            outHeightBlock.setIntValue(-1);
        }
        outHeight = smlHelper.createCount().value(outHeightBlock.getIntValue()).build();


        executable.getParameterList().getComponent("inCodec").setData(inCodec);
        executable.getParameterList().getComponent("outCodec").setData(outCodec);
        executable.getOutputList().getComponent("outFrame").getComponent("outWidth").setData(outWidthBlock);
        executable.getOutputList().getComponent("outFrame").getComponent("outHeight").setData(outHeightBlock);
    }

    @Override
    public void afterInit() throws SensorHubException {
        var castConf = ((FFmpegTranscoderConfig)config);

        // Detect input codec
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

        if (outWidth.getValue() > 0 && outHeight.getValue() > 0) {
            setArraySize(outputVideoOutport, isVariable, outWidth, outHeight);
            videoOutput.setSize(outWidth.getValue(), outHeight.getValue());
        }

        super.afterInit();
    }
}
