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

import org.sensorhub.api.datastore.FeatureFilter;


/**
 * <p>
 * Immutable filter object for SensorThings Location entities.<br/>
 * There is an implicit AND between all filter parameters.
 * </p>
 *
 * @author Alex Robin
 * @date Oct 16, 2019
 */
class STALocationFilter extends FeatureFilter
{
    protected FeatureFilter things;
    

    public FeatureFilter getThings()
    {
        return things;
    }
    
    
    public static class Builder extends FeatureFilterBuilder<STALocationFilter.Builder, STALocationFilter>
    {
        protected Builder()
        {
            this.instance = new STALocationFilter();
        }
        
        
        public STALocationFilter.Builder withThings(FeatureFilter filter)
        {
            ((STALocationFilter)instance).things = filter;
            return this;
        }


        public STALocationFilter.Builder withThings(Long... thingIDs)
        {
            ((STALocationFilter)instance).things = new FeatureFilter.Builder()
                .withInternalIDs(thingIDs)
                .build();
            return this;
        }
    }        
}