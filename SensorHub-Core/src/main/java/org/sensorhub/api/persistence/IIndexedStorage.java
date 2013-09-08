/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are copyright (C) 2010, Sensia Software LLC
 All Rights Reserved.

 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.persistence;

import java.util.List;
import org.sensorhub.api.persistence.StorageIndexDescriptor.IndexType;


public interface IIndexedStorage
{

    /**
     * Adds index of specified type on field identified by the given path	
     * The specified field can be scalar or complex but must be compatible with the index type
     * @param fieldPath
     * @param type
     * @return index ID
     */
    public abstract StorageIndexDescriptor addFieldIndex(String fieldPath, IndexType type);


    /**
     * Removes the index with the specified ID
     * @param indexId
     */
    public abstract void removeFieldIndex(String indexId);


    /**
     * Retrieve list of all index added to this storage
     * @return
     */
    public abstract List<StorageIndexDescriptor> getAllFieldIndexes();

}