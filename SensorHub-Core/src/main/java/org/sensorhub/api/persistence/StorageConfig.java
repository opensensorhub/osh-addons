package org.sensorhub.api.persistence;

import org.sensorhub.api.module.ModuleConfig;
import org.vast.cdm.common.DataComponent;


/**
 * <p><b>Title:</b>
 * StorageConfig
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Simple data structure for describing storage configuration options
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @date Nov 5, 2010
 */
public class StorageConfig extends ModuleConfig
{
    private static final long serialVersionUID = -7764098131948260556L;
	
	
	/**
	 * Path to storage on storage device (file, folder, URL, etc...)
	 */
	public String storagePath;
	
	
	/**
	 * Memory cache size in kilobytes
	 */
	public int memoryCacheSize;
	
	
	/**
	 * Structure of record data persisted in this storage
	 */
	public DataComponent dataDescription;
}
