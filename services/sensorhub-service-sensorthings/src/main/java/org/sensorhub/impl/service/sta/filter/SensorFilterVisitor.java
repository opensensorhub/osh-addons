/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2019 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sta.filter;

import org.sensorhub.api.datastore.system.SystemFilter;


/**
 * <p>
 * Visitor used to build a SystemFilter from STA $filter expression
 * </p>
 *
 * @author Alex Robin
 * @date Apr 16, 2021
 */
public class SensorFilterVisitor extends ResourceFilterVisitor<SensorFilterVisitor, SystemFilter.Builder>
{        
    
    public SensorFilterVisitor()
    {
        this(new SystemFilter.Builder());
    }
    
    
    public SensorFilterVisitor(SystemFilter.Builder filter)
    {
        super(filter);
        
        //this.propTypes.put("properties", UomVisitor.class);
        this.propTypes.put("Datastreams", DatastreamAssocVisitor.class);
    }
    
    
    class DatastreamAssocVisitor extends DatastreamFilterVisitor
    {
        @Override
        protected void assignFilter()
        {
            var dsFilter = builder.build();
            SensorFilterVisitor.this.builder.withDataStreams(dsFilter);
        }
    }


    @Override
    protected SensorFilterVisitor getNewInstance()
    {
        return new SensorFilterVisitor();
    }

}
