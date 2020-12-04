/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.persistence;

import java.util.Collection;


/**
 * <p>
 * Interface for multi-source storage implementations
 * </p>
 *
 * @author Alex Robin
 * @param <StorageType> Type of record storage used for each source
 * @since March 1, 2014
 */
public interface IMultiSourceStorage<StorageType extends IBasicStorage>
{    
    
    /**
     * @return Collection of producer IDs that feed data into this storage
     */
    public Collection<String> getProducerIDs();
    
     
    /**
     * Retrieves the data store holding data for the given producer.
     * @param producerID
     * @return Data store instance
     */
	public StorageType getDataStore(String producerID);
	
	
	/**
	 * Creates a new record data store for the given producer
	 * @param producerID ID of producer to create a new record store for
	 * @return Newly created data store instance
	 */
	public StorageType addDataStore(String producerID);
}
