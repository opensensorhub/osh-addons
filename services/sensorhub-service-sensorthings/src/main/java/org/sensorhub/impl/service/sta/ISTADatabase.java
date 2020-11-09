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

import org.sensorhub.api.database.IProcedureObsDatabase;


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
public interface ISTADatabase extends IProcedureObsDatabase
{
    
    /**
     * @return The datastream store with support for filtering by the
     * associated Things.
     */
    ISTADataStreamStore getDataStreamStore();
    
    
    /**
     * @return The data store handling Things entities
     */
    ISTAThingStore getThingStore();
    
    
    /**
     * @return The data store handling Location and HistoricalLocation entities
     */
    ISTALocationStore getThingLocationStore();
    
    
    /**
     * @return The data store handling ObservedProperty entities
     */
    ISTAObsPropStore getObservedPropertyDataStore();
    
    
    public long toPublicID(long internalID);
    
    
    public long toLocalID(long publicID);
    
    
    public void close();
    
}
