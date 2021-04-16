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

import org.sensorhub.impl.service.sta.STADataStreamFilter;


/**
 * <p>
 * Visitor used to build a ProcedureFilter from STA $filter expression
 * </p>
 *
 * @author Alex Robin
 * @date Sep 7, 2019
 */
public class DatastreamFilterVisitor extends ResourceFilterVisitor<DatastreamFilterVisitor, STADataStreamFilter.Builder>
{    
    
    public DatastreamFilterVisitor()
    {
        this(new STADataStreamFilter.Builder());
    }
    
    
    public DatastreamFilterVisitor(STADataStreamFilter.Builder builder)
    {
        super(builder);
        
        this.propTypes.put("unitOfMeasurement", UomVisitor.class);
        this.propTypes.put("phenomenonTime", PhenomenonTimeVisitor.class);
        this.propTypes.put("resultTime", ResultTimeVisitor.class);
        this.propTypes.put("Thing", ThingAssocVisitor.class);
        this.propTypes.put("Sensor", SensorAssocVisitor.class);
    }
    
    
    class UomVisitor extends UomFilterVisitor
    {
        
    }
    
    
    class PhenomenonTimeVisitor extends TemporalPropVisitor
    {
        @Override
        protected void assignFilter()
        {
            var timeFilter = builder.build();
            
            if (notEqual)
            {
                DatastreamFilterVisitor.this.builder.withValuePredicate(
                    dsInfo -> !timeFilter.test(dsInfo.getPhenomenonTimeRange()));
            }
            else
            {
                DatastreamFilterVisitor.this.builder.withValuePredicate(
                    dsInfo -> timeFilter.test(dsInfo.getPhenomenonTimeRange()));
            }
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
                DatastreamFilterVisitor.this.builder.withValuePredicate(
                    dsInfo -> !timeFilter.test(dsInfo.getPhenomenonTimeRange()));
            }
            else
                DatastreamFilterVisitor.this.builder.withValidTime(timeFilter);
        }    
    }
    
    
    class ThingAssocVisitor extends ThingFilterVisitor
    {
        @Override
        protected void assignFilter()
        {
            var thingFilter = builder.build();
            DatastreamFilterVisitor.this.builder.withThings(thingFilter);
        }
    }
    
    
    class SensorAssocVisitor extends SensorFilterVisitor
    {
        @Override
        protected void assignFilter()
        {
            var procFilter = builder.build();
            DatastreamFilterVisitor.this.builder.withProcedures(procFilter);
        }
    }


    @Override
    protected DatastreamFilterVisitor getNewInstance()
    {
        return new DatastreamFilterVisitor();
    }
}
