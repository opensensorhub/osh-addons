package org.sensorhub.impl.process.utils;

import org.sensorhub.api.processing.OSHProcessInfo;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;
import org.vast.swe.SWEHelper;
import net.opengis.swe.v20.DataArray;


public class ArrayGenerator extends ExecutableProcessImpl
{
    public static final OSHProcessInfo INFO = new OSHProcessInfo("test:ArrayGenerator", "Array Generator", "", ArrayGenerator.class);
    
    DataArray arrayOut;
    
    
    public ArrayGenerator()
    {
        super(INFO);
        var swe = new SWEHelper();
        
        outputData.add("array", arrayOut = swe.createArray()
            .withFixedSize(100)
            .withElement("sample", swe.createQuantity())
            .build());
    }
    
   
    @Override
    public void init() throws ProcessException
    {
        super.init();
    }
    
    
    @Override
    public void execute() throws ProcessException
    {
        for (int i = 0; i < 100; i++)
            arrayOut.getData().setDoubleValue(i, i * 0.5);
        started = false; // make sure it won't run again
    }
}