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

import org.sensorhub.api.common.BigId;
import org.sensorhub.api.datastore.EmptyFilterIntersection;
import org.sensorhub.api.datastore.feature.FeatureFilterBase;
import org.sensorhub.api.resource.ResourceFilter;
import org.vast.ogc.gml.IFeature;


/**
 * <p>
 * Immutable filter object for SensorThings Location entities.<br/>
 * There is an implicit AND between all filter parameters.
 * </p>
 *
 * @author Alex Robin
 * @date Oct 16, 2019
 */
public class STALocationFilter extends FeatureFilterBase<IFeature>
{
    protected STAThingFilter things;
    

    public STAThingFilter getThings()
    {
        return things;
    }


    @Override
    public ResourceFilter<IFeature> intersect(ResourceFilter<IFeature> filter) throws EmptyFilterIntersection
    {
        throw new UnsupportedOperationException();
    }
    
    
    public static class Builder extends FeatureFilterBaseBuilder<STALocationFilter.Builder, IFeature, STALocationFilter>
    {
        public Builder()
        {
            super(new STALocationFilter());
        }
        
        
        @Override
        public STALocationFilter.Builder copyFrom(STALocationFilter base)
        {
            super.copyFrom(base);
            instance.things = base.things;
            return this;
        }
        
        
        public STALocationFilter.Builder withThings(STAThingFilter filter)
        {
            instance.things = filter;
            return this;
        }


        public STALocationFilter.Builder withThings(BigId... thingIDs)
        {
            instance.things = new STAThingFilter.Builder()
                .withInternalIDs(thingIDs)
                .build();
            return this;
        }
    }       
}