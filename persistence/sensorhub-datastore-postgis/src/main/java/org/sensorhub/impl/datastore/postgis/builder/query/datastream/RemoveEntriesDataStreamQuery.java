/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis.builder.query.datastream;

import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.system.ISystemDescStore;
import org.sensorhub.impl.datastore.postgis.builder.filter.datastream.RemoveDataStreamFilterQuery;
import org.sensorhub.impl.datastore.postgis.builder.query.RemoveEntriesQuery;

public class RemoveEntriesDataStreamQuery extends RemoveEntriesQuery {
    private ISystemDescStore systemDescStore;

    protected static abstract class Init<T extends RemoveEntriesDataStreamQuery.Init<T>> extends RemoveEntriesQuery.Init<T> {
        private ISystemDescStore systemDescStore;

        public T linkTo(ISystemDescStore systemDescStore) {
            this.systemDescStore = systemDescStore;
            return self();
        }

        public T withDataStreamFilter(DataStreamFilter filter) {
            if(filter != null) {
                RemoveDataStreamFilterQuery dataStreamFilterQuery = new RemoveDataStreamFilterQuery(this.tableName, filterQueryGenerator);
                if(systemDescStore != null) {
                    dataStreamFilterQuery.setSysDescTableName(systemDescStore.getDatastoreName());
                }
                filterQueryGenerator = dataStreamFilterQuery.build(filter);
            }
            return self();
        }

        public RemoveEntriesDataStreamQuery build() {
            return new RemoveEntriesDataStreamQuery(this);
        }
    }

    public static class Builder extends RemoveEntriesDataStreamQuery.Init<RemoveEntriesDataStreamQuery.Builder> {
        @Override
        protected RemoveEntriesDataStreamQuery.Builder self() {
            return this;
        }
    }

    protected RemoveEntriesDataStreamQuery(RemoveEntriesDataStreamQuery.Init<?> init) {
        super(init);
        this.systemDescStore = init.systemDescStore;
    }
}
