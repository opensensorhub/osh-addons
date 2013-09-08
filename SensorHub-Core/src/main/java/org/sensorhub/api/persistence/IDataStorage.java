/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are copyright (C) 2010, Sensia Software LLC
 All Rights Reserved.

 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModule;
import org.vast.cdm.common.DataBlock;
import org.vast.cdm.common.DataComponent;


/**
 * <p><b>Title:</b>
 * IDataStorage
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Base interface for all storage/database/persistence engine implementations
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @date Nov 6, 2010
 */
public interface IDataStorage<KeyType extends DataKey, FilterType extends IDataFilter, ConfigType extends StorageConfig> extends IModule<ConfigType>
{	
    /**
     * Opens a storage using the specified configuration
     * @param conf
     */
    public void open() throws StorageException;
    
    
    /**
     * Closes the storage, freeing all ressources used
     * @throws IOException
     */
    public void close() throws StorageException;
    
    
    /**
     * Cleans up all resources used by this storage, including all
     * persisted data
     * @throws SensorHubException
     */
    @Override
    public void cleanup() throws StorageException;
	
	
	/**
	 * Retrieves description of data blocks persisted in this storage
	 * @return
	 */
	public DataComponent getDataDescription();
	
	
	/**
	 * Persists data block in storage
	 * @paran key 
	 * @param data
	 * @return stored data key (containing auto-generated id and time stamp if non was provided)
	 */
	public KeyType store(KeyType key, DataBlock data);
	
	
	/**
     * Retrieves raw data block with the specified id
     * @param id
     * @return data block or null if no record with the specified id were found
     */
    public DataBlock getDataBlock(long id);
    
    
	/**
	 * Retrieves record with the specified id
	 * @param id
	 * @return record or null if no record with the specified id were found
	 */
	public IDataRecord<KeyType> getRecord(long id);
	
	
	/**
     * Retrieves raw data blocks matching filtering criteria from storage
     * @param filter
     * @return a list of data blocks matching the filter
     */
    public List<DataBlock> getDataBlocks(FilterType filter);
    
    
	/**
	 * Retrieves record matching filtering criteria from storage
	 * @param filter
	 * @return a list of records matching the filter
	 */
	public List<IDataRecord<KeyType>> getRecords(FilterType filter);
	
	
	/**
     * Gets iterator of raw data blocks matching the specified filter
     * @param filter
     * @return an iterator among data blocks matching the filter
     */
    public Iterator<IDataRecord<KeyType>> getDataBlockIterator(FilterType filter);
    
    
	/**
	 * Gets iterator of records matching the specified filter
	 * @param filter
	 * @return an iterator among records matching the filter
	 */
	public Iterator<IDataRecord<KeyType>> getRecordIterator(FilterType filter);
	
	
	/**
     * Updates record with specified id with new key and data
     * @param id id of record to update
     * @paran key new key for upated record
     * @param data new data block to assign to the record
     * @return updated data key (containing auto-generated id and time stamp if non was provided)
     */
    public DataKey update(long id, KeyType key, DataBlock data);
    
    
	/**
	 * Removes record with the specified id
	 * @param id
	 */
	public void remove(long id);
	
	
	/**
     * Removes all records matching the filter
     * @param filter
     * @return number of deleted records
     */
    public int remove(FilterType filter);
	
	
	/**
	 * Backups storage to specified output stream
	 * @param os
	 */
	public void backup(OutputStream os);
	
	
	/**
	 * Restores storage from backup obtained from specified input stream
	 * @param is
	 */
	public void restore(InputStream is);
	
	
	/**
	 * Synchronizes storage with another storage of the same type (potentially remote)
	 * @param storage
	 */
	public void sync(IDataStorage<KeyType, ?, ?> storage);
	
	
	/**
     * Changes the storage behavior on record insertion, update or deletion
     * @param autoCommit true to commit changes automatically when a transactional method is called,
     * false if the commit() method should be called manually to persist changes to storage. 
     */
    public void setAutoCommit(boolean autoCommit);
    
    
    /**
     * Commits all changes generated by transactional methods since the last commit event
     */
    public void commit();
    
    
    /**
     * Cancels all changes generated by transactional methods since the last commit event
     */
    public void rollback();
    
    
    /**
     * Registers a listener to receive events from this storage    
     * @param listener
     */
    public void registerListener(IEventListener listener);
    
    
    /**
     * Unregisters listener
     * @param listener
     */
    public void removeListener(IEventListener listener);
}
