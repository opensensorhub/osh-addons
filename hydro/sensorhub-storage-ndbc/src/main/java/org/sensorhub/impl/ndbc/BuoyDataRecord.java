package org.sensorhub.impl.ndbc;

import org.sensorhub.api.persistence.DataKey;
import org.sensorhub.api.persistence.IDataRecord;
import net.opengis.swe.v20.DataBlock;

public class BuoyDataRecord implements IDataRecord, Comparable<BuoyDataRecord>
{
    DataKey key;
    DataBlock data;
    
    
    public BuoyDataRecord(DataKey key, DataBlock data)
    {
        this.key = key;
        this.data = data;
    }
    

    @Override
    public final DataKey getKey()
    {
        return key;
    }
    
    
    @Override
    public final DataBlock getData()
    {
        return data;
    }
    

    @Override
    public int compareTo(BuoyDataRecord other)
    {
        double ts = key.timeStamp;
        double ots = other.key.timeStamp;
        
        if (ts < ots)
            return -1;
        
        if (ts > ots)
            return 1;
        
        return 0;
    }
}
