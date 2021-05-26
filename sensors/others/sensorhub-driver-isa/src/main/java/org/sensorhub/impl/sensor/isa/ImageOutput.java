package org.sensorhub.impl.sensor.isa;

import org.sensorhub.impl.sensor.videocam.VideoCamHelper;

public class ImageOutput extends ISAOutput
{
    
    public ImageOutput(ISASensor parentSensor, String codec)
    {
        super("video", parentSensor);
        VideoCamHelper swe = new VideoCamHelper();
        
        // output structure
        var ds = swe.newVideoOutputCODEC(this.name, 1280, 720, codec);
        dataStruct = ds.getElementType();  
        dataEnc = ds.getEncoding();
    }
    
    
    protected void sendRandomMeasurement()
    {
        
    }
}
