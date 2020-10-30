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

import org.sensorhub.api.obs.DataStreamFilter;


/**
 * <p>
 * Immutable filter object for SensorThings Datastream entities.<br/>
 * There is an implicit AND between all filter parameters.<br/>
 * This adds the possibility to filter based on the parent Thing.
 * </p>
 *
 * @author Alex Robin
 * @date Oct 14, 2019
 */
class STADataStreamFilter extends DataStreamFilter
{
    protected STAThingFilter things;
    

    public STAThingFilter getThings()
    {
        return things;
    }
    
    
    public static class Builder extends DataStreamFilterBuilder<Builder, STADataStreamFilter>
    {
        protected Builder()
        {
            super(new STADataStreamFilter());
        }
        
        
        public Builder withThings(STAThingFilter filter)
        {
            ((STADataStreamFilter)instance).things = filter;
            return this;
        }


        public Builder withThings(long... thingIDs)
        {
            instance.things = new STAThingFilter.Builder()
                .withInternalIDs(thingIDs)
                .build();
            return this;
        }
    }
}
