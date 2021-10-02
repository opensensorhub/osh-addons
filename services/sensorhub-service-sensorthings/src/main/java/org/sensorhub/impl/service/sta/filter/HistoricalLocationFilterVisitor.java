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

import org.sensorhub.impl.service.sta.STALocationFilter;


/**
 * <p>
 * Visitor used to build a STALocationFilter from STA $filter expression
 * </p>
 *
 * @author Alex Robin
 * @date Apr 16, 2021
 */
public class HistoricalLocationFilterVisitor extends ResourceFilterVisitor<HistoricalLocationFilterVisitor, STALocationFilter.Builder>
{        
    
    public HistoricalLocationFilterVisitor()
    {
        this(new STALocationFilter.Builder());
    }
    
    
    public HistoricalLocationFilterVisitor(STALocationFilter.Builder filter)
    {
        super(filter);
        
        this.propTypes.put("time", TimePropVisitor.class);
        this.propTypes.put("Things", ThingAssocVisitor.class);
        this.propTypes.put("Locations", LocationAssocVisitor.class);
    }
    
    
    class TimePropVisitor extends TemporalPropVisitor
    {
        @Override
        protected void assignFilter()
        {
            var timeFilter = builder.build();
            
            if (notEqual)
            {
                HistoricalLocationFilterVisitor.this.builder.withValuePredicate(
                    loc -> !timeFilter.test(loc.getValidTime()));
            }
            else
                HistoricalLocationFilterVisitor.this.builder.withValidTime(timeFilter);
        }       
    }
    
    
    class ThingAssocVisitor extends ThingFilterVisitor
    {
        @Override
        protected void assignFilter()
        {
            var thingsFilter = builder.build();
            HistoricalLocationFilterVisitor.this.builder.withThings(thingsFilter);
        }
    }
    
    
    class LocationAssocVisitor extends LocationFilterVisitor
    {
        @Override
        protected void assignFilter()
        {
            var locFilter = builder.build();
            HistoricalLocationFilterVisitor.this.builder.copyFrom(locFilter);
        }
    }


    @Override
    protected HistoricalLocationFilterVisitor getNewInstance()
    {
        return new HistoricalLocationFilterVisitor();
    }

}
