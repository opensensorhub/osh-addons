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

import org.sensorhub.api.datastore.FeatureKey;
import org.sensorhub.api.datastore.IFeatureStore;
import org.sensorhub.api.datastore.IHistoricalObsDatabase;
import org.vast.ogc.gml.GenericFeature;
import org.vast.ogc.gml.IFeature;


/**
 * <p>
 * Interface for a complete SensorThings database.<br/>
 * This can be implemented fully but can also be a full or partial wrapper
 * for data stores/databases already existing on the hub.
 * </p>
 *
 * @author Alex Robin
 * @date Oct 12, 2019
 */
public interface ISTADatabase extends IHistoricalObsDatabase
{
    public static class ObsPropDef implements IFeature
    {
        String uri;
        String name;
        String description;
        
        public ObsPropDef(String uri, String name, String description)
        {
            this.uri = uri;
            this.name = name;
            this.description = description;
        }
        
        @Override
        public String getName()
        {
            return name;
        }
        
        @Override
        public String getUniqueIdentifier()
        {
            return uri;
        }
        
        @Override
        public String getDescription()
        {
            return description;
        }
    }
    
    
    /**
     * @return The datastream store with support for filtering by the
     * associated Things.
     */
    ISTADataStreamStore getDataStreamStore();
    
    
    /**
     * @return The data store handling Things entities
     */
    IFeatureStore<FeatureKey, GenericFeature> getThingStore();
    
    
    /**
     * @return The data store handling Location and HistoricalLocation entities
     */
    ILocationStore getThingLocationStore();
    
    
    /**
     * @return The data store handling ObservedProperty entities
     */
    IFeatureStore<FeatureKey, ObsPropDef> getObservedPropertyDataStore();
    
    
    public long toPublicID(long internalID);
    
    
    public long toLocalID(long publicID);
    
    
    public void close();
    
}
