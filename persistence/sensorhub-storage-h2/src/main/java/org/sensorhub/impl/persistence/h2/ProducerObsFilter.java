/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2018 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence.h2;

import java.util.Set;
import org.sensorhub.api.persistence.IDataFilter;
import org.sensorhub.api.persistence.IObsFilter;
import com.google.common.collect.Sets;
import com.vividsolutions.jts.geom.Polygon;


/**
 * Wrapper to add producerID to filter when omitted
 */
public class ProducerObsFilter implements IObsFilter
{
    final Set<String> producerIDs;
    final IDataFilter filter;
    
    
    public ProducerObsFilter(String producerID, IDataFilter filter)
    {
        this.producerIDs = (producerID != null) ? Sets.newHashSet(producerID) : null;
        this.filter = filter;
    }


    @Override
    public String getRecordType()
    {
        return filter.getRecordType();
    }


    @Override
    public Set<String> getProducerIDs()
    {
        return producerIDs;
    }


    @Override
    public double[] getTimeStampRange()
    {
        return filter.getTimeStampRange();
    }


    @Override
    public double[] getResultTimeRange()
    {
        if (filter instanceof IObsFilter)
            return ((IObsFilter)filter).getResultTimeRange();
        else
            return null;
    }


    @Override
    public Set<String> getFoiIDs()
    {
        if (filter instanceof IObsFilter)
            return ((IObsFilter)filter).getFoiIDs();
        else
            return null;
    }


    @Override
    public Polygon getRoi()
    {
        if (filter instanceof IObsFilter)
            return ((IObsFilter)filter).getRoi();
        else
            return null;
    }

}
