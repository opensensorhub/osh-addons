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
import org.sensorhub.api.persistence.IFoiFilter;
import com.vividsolutions.jts.geom.Polygon;


/**
 * Wrapper to add producer foi IDs to filter when omitted
 */
public class ProducerFoiFilter implements IFoiFilter
{
    final Set<String> foiIDs;
    final IFoiFilter filter;
    
    
    public ProducerFoiFilter(Set<String> foiIDs, IFoiFilter filter)
    {
        this.foiIDs = foiIDs;
        this.filter = filter;
    }


    @Override
    public Set<String> getProducerIDs()
    {
        return null;
    }


    @Override
    public Set<String> getFeatureIDs()
    {
        return foiIDs;
    }


    @Override
    public Polygon getRoi()
    {
        return filter.getRoi();
    }

}
