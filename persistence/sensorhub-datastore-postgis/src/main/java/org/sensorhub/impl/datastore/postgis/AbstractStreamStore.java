/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.sensorhub.api.data.IDataStreamInfo;
import org.sensorhub.api.datastore.obs.IDataStreamStore;
import org.sensorhub.api.datastore.system.ISystemDescStore;
import org.sensorhub.impl.datastore.postgis.builder.QueryBuilder;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class AbstractStreamStore<T extends QueryBuilder> extends PostgisStore<T> implements IDataStreamStore  {

    protected volatile Cache<Long, IDataStreamInfo> cache = CacheBuilder.newBuilder()
            .maximumSize(150)
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build();

    protected ISystemDescStore systemStore;

    private final Lock lock = new ReentrantLock();

    protected AbstractStreamStore(int idScope, IdProviderType dsIdProviderType, T queryBuilder) {
        super(idScope, dsIdProviderType, queryBuilder);
    }

    @Override
    public void linkTo(ISystemDescStore systemStore) {
//        this.systemStore = Asserts.checkNotNull(systemStore, ISystemDescStore.class);
//        queryBuilder.linkTo(this.systemStore);
    }

}
