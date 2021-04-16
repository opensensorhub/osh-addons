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

import org.sensorhub.api.datastore.obs.ObsFilter;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.logical.And;


/**
 * <p>
 * Visitor used to build an ObsFilter from STA $filter expression
 * </p>
 *
 * @author Alex Robin
 * @date April 26, 2021
 */
public class ObsFilterVisitor extends EntityFilterVisitor<ObsFilterVisitor>
{
    ObsFilter.Builder builder;
        
    
    public ObsFilterVisitor()
    {
        this(new ObsFilter.Builder());
    }
    
    
    public ObsFilterVisitor(ObsFilter.Builder filter)
    {
        this.builder = filter;
        
        this.propTypes.put("phenomenonTime", PhenomenonTimeVisitor.class);
        this.propTypes.put("resultTime", ResultTimeVisitor.class);
        this.propTypes.put("Datastream", DatastreamAssocVisitor.class);
        this.propTypes.put("MultiDatastream", DatastreamAssocVisitor.class);
    }
    
    
    @Override
    public ObsFilterVisitor visit(And node)
    {
        var v1 = node.getParameters().get(0).accept(getNewInstance());
        var v2 = node.getParameters().get(1).accept(getNewInstance());
        
        try {
            var f1 = v1.builder.build();
            var f2 = v2.builder.build();
            this.builder = builder.copyFrom(f1.intersect(f2));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid AND filter", e);
        }        
        
        return this;
    }
    
    
    class PhenomenonTimeVisitor extends TemporalPropVisitor
    {
        @Override
        protected void assignFilter()
        {
            var timeFilter = builder.build();
            
            if (notEqual)
            {
                ObsFilterVisitor.this.builder.withValuePredicate(
                    obs -> !timeFilter.test(obs.getPhenomenonTime()));
            }
            else
                ObsFilterVisitor.this.builder.withPhenomenonTime(timeFilter);
        }       
    }
    
    
    class ResultTimeVisitor extends TemporalPropVisitor
    {
        @Override
        protected void assignFilter()
        {
            var timeFilter = builder.build();
            
            if (notEqual)
            {              
                ObsFilterVisitor.this.builder.withValuePredicate(
                    obs -> !timeFilter.test(obs.getResultTime()));
            }
            else
                ObsFilterVisitor.this.builder.withResultTime(timeFilter);
        }    
    }
    
    
    class DatastreamAssocVisitor extends DatastreamFilterVisitor
    {
        @Override
        protected void assignFilter()
        {
            var dsFilter = builder.build();
            ObsFilterVisitor.this.builder.withDataStreams(dsFilter);
        }
    }


    @Override
    protected ObsFilterVisitor getNewInstance()
    {
        return new ObsFilterVisitor();
    }

}
