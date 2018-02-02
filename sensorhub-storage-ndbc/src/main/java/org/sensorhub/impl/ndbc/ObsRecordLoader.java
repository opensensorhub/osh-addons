package org.sensorhub.impl.ndbc;

import org.sensorhub.impl.module.AbstractModule;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;

public class ObsRecordLoader {
	AbstractModule<?> module;
	DataBlock data;
	
    public ObsRecordLoader(AbstractModule<?> module, DataComponent recordDesc)
    {
        this.module = module;
        this.data = recordDesc.createDataBlock();
        
    }
    
    public void postData() {
    	System.out.println("Posting Data...");
    	data.setDoubleValue(0, 1517545558);
    	data.setStringValue(1, "0001A");
    	data.setDoubleValue(2, 15);
    }
}
