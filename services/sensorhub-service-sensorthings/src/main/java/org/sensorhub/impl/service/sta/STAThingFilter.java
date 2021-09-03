/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2019 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sta;

import org.sensorhub.api.datastore.EmptyFilterIntersection;
import org.sensorhub.api.datastore.feature.FeatureFilterBase;
import org.sensorhub.api.resource.ResourceFilter;
import org.vast.ogc.gml.IFeature;


/**
 * <p>
 * Immutable filter object for SensorThings Thing entities.<br/>
 * There is an implicit AND between all filter parameters.
 * </p>
 *
 * @author Alex Robin
 * @date Oct 29, 2019
 */
public class STAThingFilter extends FeatureFilterBase<IFeature>
{
    protected STALocationFilter locations;
    

    public STALocationFilter getLocations()
    {
        return locations;
    }


    @Override
    public ResourceFilter<IFeature> intersect(ResourceFilter<IFeature> filter) throws EmptyFilterIntersection
    {
        throw new UnsupportedOperationException();
    }
    
    
    public static class Builder extends FeatureFilterBaseBuilder<Builder, IFeature, STAThingFilter>
    {
        public Builder()
        {
            super(new STAThingFilter());
        }
        
        
        @Override
        public STAThingFilter.Builder copyFrom(STAThingFilter base)
        {
            super.copyFrom(base);
            instance.locations = base.locations;
            return this;
        }
        
        
        public Builder withLocations(STALocationFilter filter)
        {
            instance.locations = filter;
            return this;
        }


        public Builder withLocations(long... locationIDs)
        {
            instance.locations = new STALocationFilter.Builder()
                .withInternalIDs(locationIDs)
                .build();
            return this;
        }
    }        
}