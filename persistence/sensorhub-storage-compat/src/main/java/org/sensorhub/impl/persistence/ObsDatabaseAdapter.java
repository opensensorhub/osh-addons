/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2019 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence;

import java.util.concurrent.Callable;
import org.sensorhub.api.database.IProcedureObsDatabase;
import org.sensorhub.api.datastore.feature.IFoiStore;
import org.sensorhub.api.datastore.obs.IObsStore;
import org.sensorhub.api.datastore.procedure.IProcedureStore;


/**
 * <p>
 * Adapter to wrap old storage implementations using the new database API.
 * </p>
 *
 * @author Alex Robin
 * @date Apr 2, 2020
 */
public class ObsDatabaseAdapter implements IProcedureObsDatabase
{

    @Override
    public int getDatabaseID()
    {
        // TODO Auto-generated method stub
        return 0;
    }


    @Override
    public <T> T executeTransaction(Callable<T> transaction) throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public void commit()
    {
        // TODO Auto-generated method stub

    }


    @Override
    public IProcedureStore getProcedureStore()
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public IObsStore getObservationStore()
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public IFoiStore getFoiStore()
    {
        // TODO Auto-generated method stub
        return null;
    }

}
