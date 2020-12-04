/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.persistence;

import java.util.Set;
import org.vast.util.Asserts;
import org.vast.util.DateTimeFormat;


/**
 * <p>
 * Default implementation of {@link IDataFilter} returning null on all filter
 * predicates. It is meant be used as a base to implement your own filter and
 * unlike {@link IDataFilter} doesn't require implementing all methods.
 * </p>
 *
 * @author Alex Robin
 * @since May 9, 2015
 */
public class DataFilter implements IDataFilter
{
    private String recordType;
    
    
    public DataFilter(String recordType)
    {
        Asserts.checkNotNull(recordType, "recordType");
        this.recordType = recordType;
    }
    
    
    @Override
    public String getRecordType()
    {
        return recordType;
    }


    @Override
    public double[] getTimeStampRange()
    {
        return null;
    }


    @Override
    public Set<String> getProducerIDs()
    {
        return null;
    }
    
    
    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append("Filter {");
        toString(buf);
        buf.append('}');
        return buf.toString();
    }
    
    
    protected void toString(StringBuilder buf)
    {
        buf.append("recordType=").append(recordType);
        buf.append(", producers=").append(getProducerIDs() == null ? "ALL" : getProducerIDs());
        buf.append(", timeRange=");
        double[] timeRange = getTimeStampRange();                
        if (timeRange != null)
        {
            buf.append(new DateTimeFormat().formatIso(timeRange[0], 0));
            buf.append(new DateTimeFormat().formatIso(timeRange[1], 0));
        }
        else
            buf.append("ALL");
    }
}
