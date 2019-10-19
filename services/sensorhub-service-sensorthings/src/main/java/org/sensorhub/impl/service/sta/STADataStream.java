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

import org.sensorhub.api.datastore.DataStreamInfo;


/**
 * <p>
 * Special Datastream object for SensorThings containing a reference to a Thing.
 * </p>
 *
 * @author Alex Robin
 * @date Oct 14, 2019
 */
public class STADataStream extends DataStreamInfo
{
    long thingID;
    
    
    public long getThingID()
    {
        return thingID;
    }
    
    
    public static class Builder extends DataStreamInfoBuilder<Builder, STADataStream>
    {
        protected Builder()
        {
            this.instance = new STADataStream();
        }
        
        public static Builder from(DataStreamInfo base)
        {
            return new Builder().copyFrom(base);
        }
        
        protected Builder copyFrom(DataStreamInfo base)
        {
            super.copyFrom(base);
            return this;
        }
        
        public Builder withThing(long thingID)
        {
            ((STADataStream)instance).thingID = thingID;
            return this;
        }
    }
}
