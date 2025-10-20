package org.sensorhub.impl.process.video.transcoder.formatters;

import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.sensorhub.impl.process.video.transcoder.CodecEnum;

public abstract class AVByteFormatter<T extends Pointer> {

    public abstract T convertInput(byte[] inputData);

    public abstract byte[] convertOutput(T outputData);
}