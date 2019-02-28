/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2017 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.usgs.water;

import org.sensorhub.api.persistence.DataKey;
import org.sensorhub.api.persistence.IDataRecord;
import net.opengis.swe.v20.DataBlock;


public class WaterDataRecord implements IDataRecord, Comparable<WaterDataRecord>
{
    DataKey key;
    DataBlock data;
    
    
    public WaterDataRecord(DataKey key, DataBlock data)
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
    public int compareTo(WaterDataRecord other)
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
