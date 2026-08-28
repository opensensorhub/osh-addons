/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence.es;

import org.sensorhub.api.persistence.IRecordStoreInfo;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;


public class DataStreamInfo implements IRecordStoreInfo
{
    String name;
    DataComponent recordDescription;
    DataEncoding recommendedEncoding;
    
    
    public DataStreamInfo(String name, DataComponent recordDescription, DataEncoding recommendedEncoding)
    {
        this.name = name;
        this.recordDescription = recordDescription;
        this.recommendedEncoding = recommendedEncoding;
    }


    @Override
    public String getName()
    {
        return name;
    }
    
    
    @Override
    public DataComponent getRecordDescription()
    {
        return recordDescription;
    }
    
    
    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return recommendedEncoding;
    }
}
