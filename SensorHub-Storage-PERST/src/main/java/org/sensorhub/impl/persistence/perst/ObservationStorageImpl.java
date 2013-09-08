/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are copyright (C) 2010, Sensia Software LLC
 All Rights Reserved.

 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence.perst;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.module.IModuleStateLoader;
import org.sensorhub.api.module.IModuleStateSaver;
import org.sensorhub.api.persistence.DataKey;
import org.sensorhub.api.persistence.IDataRecord;
import org.sensorhub.api.persistence.IDataStorage;
import org.sensorhub.api.persistence.IObsFilter;
import org.sensorhub.api.persistence.IObsStorage;
import org.sensorhub.api.persistence.ObsKey;
import org.sensorhub.api.persistence.StorageException;
import org.vast.cdm.common.DataBlock;
import org.vast.cdm.common.DataComponent;


/**
 * <p><b>Title:</b>
 * ObservationStorageImpl
 * </p>
 *
 * <p><b>Description:</b><br/>
 * TODO ObservationStorageImpl type description
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @date Nov 16, 2010
 */
public class ObservationStorageImpl implements IObsStorage<ObsStorageConfig>
{

    @Override
    public void open() throws StorageException
    {
        // TODO Auto-generated method stub

    }


    @Override
    public void close() throws StorageException
    {
        // TODO Auto-generated method stub

    }


    @Override
    public DataComponent getDataDescription()
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public ObsKey store(ObsKey key, DataBlock data)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DataBlock getDataBlock(long id)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public IDataRecord<ObsKey> getRecord(long id)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public List<DataBlock> getDataBlocks(IObsFilter filter)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public List<IDataRecord<ObsKey>> getRecords(IObsFilter filter)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public Iterator<IDataRecord<ObsKey>> getDataBlockIterator(IObsFilter filter)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public Iterator<IDataRecord<ObsKey>> getRecordIterator(IObsFilter filter)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DataKey update(long id, ObsKey key, DataBlock data)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public void remove(long id)
    {
        // TODO Auto-generated method stub

    }


    @Override
    public int remove(IObsFilter filter)
    {
        // TODO Auto-generated method stub
        return 0;
    }


    @Override
    public void backup(OutputStream os)
    {
        // TODO Auto-generated method stub

    }


    @Override
    public void restore(InputStream is)
    {
        // TODO Auto-generated method stub

    }


    @Override
    public void sync(IDataStorage<ObsKey, ?, ?> storage)
    {
        // TODO Auto-generated method stub

    }


    @Override
    public void setAutoCommit(boolean autoCommit)
    {
        // TODO Auto-generated method stub

    }


    @Override
    public void commit()
    {
        // TODO Auto-generated method stub

    }


    @Override
    public void rollback()
    {
        // TODO Auto-generated method stub

    }


    @Override
    public void registerListener(IEventListener listener)
    {
        // TODO Auto-generated method stub

    }


    @Override
    public void removeListener(IEventListener listener)
    {
        // TODO Auto-generated method stub

    }


    @Override
    public void init(ObsStorageConfig config)
    {
        // TODO Auto-generated method stub

    }
    
    
    @Override
    public void updateConfig(ObsStorageConfig config)
    {
        // TODO Auto-generated method stub
        
    }


    @Override
    public ObsStorageConfig getConfiguration()
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public void saveState(IModuleStateSaver saver)
    {
        // TODO Auto-generated method stub

    }


    @Override
    public void loadState(IModuleStateLoader loader)
    {
        // TODO Auto-generated method stub

    }


    @Override
    public List<String> getFeatureOfInterestIds()
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public String getName()
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public String getLocalID()
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public void cleanup() throws StorageException
    {
        // TODO Auto-generated method stub
        
    }
}
