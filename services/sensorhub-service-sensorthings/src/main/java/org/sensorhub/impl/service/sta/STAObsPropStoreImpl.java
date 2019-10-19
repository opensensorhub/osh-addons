/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are copyright (C) 2018, Sensia Software LLC
 All Rights Reserved. This software is the property of Sensia Software LLC.
 It cannot be duplicated, used, or distributed without the express written
 consent of Sensia Software LLC.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sta;

import org.sensorhub.impl.datastore.h2.H2Utils;
import org.sensorhub.impl.datastore.h2.MVBaseFeatureStoreImpl;
import org.sensorhub.impl.datastore.h2.MVDataStoreInfo;
import org.sensorhub.impl.service.sta.ISTADatabase.ObsPropDef;


public class STAObsPropStoreImpl extends MVBaseFeatureStoreImpl<ObsPropDef>
{

    protected STAObsPropStoreImpl()
    {
    }
    
    
    public static STAObsPropStoreImpl open(STADatabase db, String dataStoreName)
    {
        MVDataStoreInfo dataStoreInfo = H2Utils.loadDataStoreInfo(db.getMVStore(), dataStoreName);
        return (STAObsPropStoreImpl)new STAObsPropStoreImpl().init(db.getMVStore(), dataStoreInfo, null);
    }
    
    
    public static STAObsPropStoreImpl create(STADatabase db, MVDataStoreInfo dataStoreInfo)
    {
        H2Utils.addDataStoreInfo(db.getMVStore(), dataStoreInfo);
        return (STAObsPropStoreImpl)new STAObsPropStoreImpl().init(db.getMVStore(), dataStoreInfo, null);
    }
    

}
