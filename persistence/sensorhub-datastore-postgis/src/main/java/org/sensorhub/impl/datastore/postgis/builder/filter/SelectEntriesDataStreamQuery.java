/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis.builder.filter;

import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.system.ISystemDescStore;

public class SelectEntriesDataStreamQuery extends SelectEntriesQuery {
    private ISystemDescStore systemDescStore;
    protected static abstract class Init<T extends SelectEntriesDataStreamQuery.Init<T>> extends SelectEntriesQuery.Init<T> {
        private ISystemDescStore systemDescStore;

        public T linkTo(ISystemDescStore systemDescStore) {
            this.systemDescStore = systemDescStore;
            return self();
        }

        public T withDataStreamFilter(DataStreamFilter filter) {
            if(filter != null) {
                DataStreamFilterQuery dataStreamFilterQuery = new DataStreamFilterQuery(this.tableName, filterQueryGenerator);
                if(systemDescStore != null) {
                    dataStreamFilterQuery.setSysDescTableName(systemDescStore.getDatastoreName());
                }
                filterQueryGenerator = dataStreamFilterQuery.build(filter);
            }
            return self();
        }

        public SelectEntriesDataStreamQuery build() {
            return new SelectEntriesDataStreamQuery(this);
        }
    }

    public static class Builder extends SelectEntriesDataStreamQuery.Init<SelectEntriesDataStreamQuery.Builder> {
        @Override
        protected SelectEntriesDataStreamQuery.Builder self() {
            return this;
        }
    }

    protected SelectEntriesDataStreamQuery(SelectEntriesDataStreamQuery.Init<?> init) {
        super(init);
        this.systemDescStore = init.systemDescStore;
    }
}
