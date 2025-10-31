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

import org.sensorhub.api.datastore.feature.FoiFilter;
import org.sensorhub.api.datastore.feature.IFoiStore;
import org.sensorhub.api.datastore.obs.IDataStreamStore;
import org.sensorhub.api.datastore.obs.IObsStore;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.datastore.system.ISystemDescStore;

public class SelectEntriesFoiQuery extends SelectEntriesQuery {
    protected static abstract class Init<T extends SelectEntriesFoiQuery.Init<T>> extends SelectEntriesQuery.Init<T> {
        private IObsStore obsStore;
        private ISystemDescStore systemDescStore;
        private IDataStreamStore dataStreamStore;

        public T linkTo(IObsStore obsStore) {
            this.obsStore = obsStore;
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

        public T withFoiFilter(FoiFilter filter) {
            if (filter != null) {
                FoiFilterQuery filterQuery = new FoiFilterQuery(this.tableName, filterQueryGenerator);
                if(systemDescStore != null) {
                    filterQuery.setSysDescTableName(systemDescStore.getDatastoreName());
                }
                if(dataStreamStore != null) {
                    filterQuery.setDataStreamTableName(dataStreamStore.getDatastoreName());
                }
                if(obsStore != null) {
                    filterQuery.setObsTableName(obsStore.getDatastoreName());
                }

                filterQueryGenerator = filterQuery.build(filter);
            }
            return self();
        }

        public SelectEntriesFoiQuery build() {
            return new SelectEntriesFoiQuery(this);
        }
    }

    public static class Builder extends SelectEntriesFoiQuery.Init<SelectEntriesFoiQuery.Builder> {
        @Override
        protected SelectEntriesFoiQuery.Builder self() {
            return this;
        }
    }

    protected SelectEntriesFoiQuery(SelectEntriesFoiQuery.Init<?> init) {
        super(init);
    }
}
