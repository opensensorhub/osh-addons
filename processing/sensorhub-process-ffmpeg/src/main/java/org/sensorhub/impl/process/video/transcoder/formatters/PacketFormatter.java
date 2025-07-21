package org.sensorhub.impl.process.video.transcoder.formatters;

import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.sensorhub.impl.process.video.transcoder.CodecEnum;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avutil.*;
import static org.bytedeco.ffmpeg.global.swscale.*;

public class PacketFormatter extends AVByteFormatter<AVPacket> {

    @Override
    public AVPacket convertInput(byte[] inputData) {
        AVPacket newPacket = av_packet_alloc();
        av_new_packet(newPacket, inputData.length);
        newPacket.data().position(0);
        newPacket.data().put(inputData.clone(), 0, inputData.length);
        return newPacket;
    }

    @Override
    public byte[] convertOutput(AVPacket outputData) {
        byte[] outData = new byte[outputData.size()];
        outputData.data().get(outData);
        return outData;
    }
}