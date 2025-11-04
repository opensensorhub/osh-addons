/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis.builder.query.obs;

import org.sensorhub.api.datastore.feature.IFoiStore;
import org.sensorhub.api.datastore.obs.IDataStreamStore;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.datastore.system.ISystemDescStore;
import org.sensorhub.impl.datastore.postgis.builder.filter.obs.SelectObsFilterQuery;
import org.sensorhub.impl.datastore.postgis.builder.generator.SelectFilterQueryGenerator;
import org.sensorhub.impl.datastore.postgis.builder.query.EntriesQuery;
import org.sensorhub.impl.datastore.postgis.builder.query.SelectEntriesQuery;

public class SelectEntriesObsQuery extends SelectEntriesQuery {
    private IFoiStore foiStore;
    private ISystemDescStore systemDescStore;
    private  IDataStreamStore dataStreamStore;

    protected SelectEntriesObsQuery(EntriesQuery.Init<?, SelectFilterQueryGenerator> init) {
        super(init);
    }

    protected static abstract class Init<T extends SelectEntriesObsQuery.Init<T>> extends SelectEntriesQuery.Init<T> {
        private IFoiStore foiStore;
        private ISystemDescStore systemDescStore;
        private  IDataStreamStore dataStreamStore;

        public T linkTo(IFoiStore foiStore) {
            this.foiStore = foiStore;
            return self();
        }
        public T linkTo(ISystemDescStore systemDescStore) {
            this.systemDescStore = systemDescStore;
            return self();
        }

        public T linkTo(IDataStreamStore dataStreamStore) {
            this.dataStreamStore = dataStreamStore;
            return self();
        }

        public T withObsFilter(ObsFilter filter) {
            if(filter != null) {
                SelectObsFilterQuery obsFilterQuery = new SelectObsFilterQuery(this.tableName, filterQueryGenerator);
                if(systemDescStore != null) {
                    obsFilterQuery.setSysDescTableName(systemDescStore.getDatastoreName());
                }
                if(dataStreamStore != null) {
                    obsFilterQuery.setDataStreamTableName(dataStreamStore.getDatastoreName());
                }
                if(foiStore != null) {
                    obsFilterQuery.setFoiTableName(foiStore.getDatastoreName());
                }
                filterQueryGenerator = obsFilterQuery.build(filter);
            }
            return self();
        }

        public SelectEntriesObsQuery build() {
            return new SelectEntriesObsQuery(this);
        }
    }

    public static class Builder extends SelectEntriesObsQuery.Init<SelectEntriesObsQuery.Builder> {
        @Override
        protected SelectEntriesObsQuery.Builder self() {
            return this;
        }
    }

    protected SelectEntriesObsQuery(Init<?> init) {
        super(init);
        this.foiStore = init.foiStore;
        this.systemDescStore = init.systemDescStore;
        this.dataStreamStore = init.dataStreamStore;
    }

}
