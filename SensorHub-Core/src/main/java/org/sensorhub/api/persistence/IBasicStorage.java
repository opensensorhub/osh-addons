/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are copyright (C) 2010, Sensia Software LLC
 All Rights Reserved.

 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.persistence;


/**
 * <p><b>Title:</b>
 * IBasicStorage
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Tagging interface for basic key/datablock storage implementations
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @date Nov 15, 2010
 */
public interface IBasicStorage<ConfigType extends StorageConfig> extends IDataStorage<DataKey, IDataFilter, ConfigType>
{

}
