/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are copyright (C) 2018, Sensia Software LLC
 All Rights Reserved. This software is the property of Sensia Software LLC.
 It cannot be duplicated, used, or distributed without the express written
 consent of Sensia Software LLC.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sta;

import org.sensorhub.api.datastore.feature.FeatureFilter;
import org.sensorhub.api.datastore.feature.IFeatureStoreBase.FeatureField;
import org.sensorhub.impl.datastore.h2.MVBaseFeatureStoreImpl;
import org.sensorhub.impl.datastore.h2.MVDataStoreInfo;
import org.sensorhub.impl.service.sta.ISTAObsPropStore.ObsPropDef;


public class STAObsPropStoreImpl extends MVBaseFeatureStoreImpl<ObsPropDef, FeatureField, FeatureFilter> implements ISTAObsPropStore
{
    
    protected STAObsPropStoreImpl()
    {
    }
    
    
    public static STAObsPropStoreImpl open(STADatabase db, MVDataStoreInfo dataStoreInfo)
    {
        return (STAObsPropStoreImpl)new STAObsPropStoreImpl().init(db.getMVStore(), dataStoreInfo, null);
    }


    @Override
    public FeatureFilter.Builder filterBuilder()
    {
        return new FeatureFilter.Builder();
    }
    

}
