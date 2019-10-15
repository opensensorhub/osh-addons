/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are copyright (C) 2018, Sensia Software LLC
 All Rights Reserved. This software is the property of Sensia Software LLC.
 It cannot be duplicated, used, or distributed without the express written
 consent of Sensia Software LLC.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sta;

import org.h2.mvstore.MVStore;
import org.sensorhub.impl.datastore.h2.H2Utils;
import org.sensorhub.impl.datastore.h2.MVBaseFeatureStoreImpl;
import org.sensorhub.impl.datastore.h2.MVDataStoreInfo;
import org.sensorhub.impl.service.sta.ISTADatabase.ObservedProperty;


public class MVObsPropStoreImpl extends MVBaseFeatureStoreImpl<ObservedProperty>
{

    protected MVObsPropStoreImpl()
    {
    }
    
    
    /**
     * Opens an existing observed property store with the specified name
     * @param mvStore MVStore instance containing the required maps
     * @param dataStoreName name of data store to open
     * @return The existing datastore instance 
     */
    public static MVObsPropStoreImpl open(MVStore mvStore, String dataStoreName)
    {
        MVDataStoreInfo dataStoreInfo = H2Utils.loadDataStoreInfo(mvStore, dataStoreName);
        return (MVObsPropStoreImpl)new MVObsPropStoreImpl().init(mvStore, dataStoreInfo);
    }
    
    
    /**
     * Create a new observed property store with the provided info
     * @param mvStore MVStore instance where the maps will be created
     * @param dataStoreInfo new data store info
     * @return The new datastore instance 
     */
    public static MVObsPropStoreImpl create(MVStore mvStore, MVDataStoreInfo dataStoreInfo)
    {
        H2Utils.addDataStoreInfo(mvStore, dataStoreInfo);
        return (MVObsPropStoreImpl)new MVObsPropStoreImpl().init(mvStore, dataStoreInfo);
    }
    

}
