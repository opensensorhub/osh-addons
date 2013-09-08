/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are copyright (C) 2010, Sensia Software LLC
 All Rights Reserved.

 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.persistence;

import java.util.List;


/**
 * <p><b>Title:</b>
 * IObsStorage
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Interface for storing and retrieving persistent observation data
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @date Nov 6, 2010
 */
public interface IObsStorage<ConfigType extends StorageConfig> extends IDataStorage<ObsKey, IObsFilter, ConfigType>
{    
    /**
     * @return list of fois for which observations are available on this storage
     */
    public List<String> getFeatureOfInterestIds();
}
