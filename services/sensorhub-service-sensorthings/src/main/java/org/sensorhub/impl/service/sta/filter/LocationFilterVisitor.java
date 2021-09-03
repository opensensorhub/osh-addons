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
public class LocationFilterVisitor extends ResourceFilterVisitor<LocationFilterVisitor, STALocationFilter.Builder>
{        
    
    public LocationFilterVisitor()
    {
        this(new STALocationFilter.Builder());
    }
    
    
    public LocationFilterVisitor(STALocationFilter.Builder filter)
    {
        super(filter);
        
        this.propTypes.put("location", LocationPropVisitor.class);
        this.propTypes.put("Things", ThingAssocVisitor.class);
    }
    
    
    class LocationPropVisitor extends SpatialPropVisitor
    {
        @Override
        protected void assignFilter()
        {
            var spatialFilter = builder.build();
            LocationFilterVisitor.this.builder.withLocation(spatialFilter);
        }       
    }
    
    
    class ThingAssocVisitor extends ThingFilterVisitor
    {
        @Override
        protected void assignFilter()
        {
            var thingsFilter = builder.build();
            LocationFilterVisitor.this.builder.withThings(thingsFilter);
        }
    }


    @Override
    protected LocationFilterVisitor getNewInstance()
    {
        return new LocationFilterVisitor();
    }

}
