package org.sensorhub.impl.datastore.postgis.builder.query.feature;

import org.sensorhub.api.datastore.feature.FoiFilter;
import org.sensorhub.api.datastore.obs.IDataStreamStore;
import org.sensorhub.api.datastore.obs.IObsStore;
import org.sensorhub.api.datastore.system.ISystemDescStore;
import org.sensorhub.impl.datastore.postgis.builder.filter.feature.RemoveFoiFilterQuery;
import org.sensorhub.impl.datastore.postgis.builder.generator.RemoveFilterQueryGenerator;
import org.sensorhub.impl.datastore.postgis.builder.query.EntriesQuery;
import org.sensorhub.impl.datastore.postgis.builder.query.RemoveEntriesQuery;

public class RemoveEntriesFoiQuery extends RemoveEntriesQuery {
    protected RemoveEntriesFoiQuery(EntriesQuery.Init<?, RemoveFilterQueryGenerator> init) {
        super(init);
    }

    protected static abstract class Init<T extends RemoveEntriesFoiQuery.Init<T>> extends RemoveEntriesQuery.Init<T> {
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
                RemoveFoiFilterQuery filterQuery = new RemoveFoiFilterQuery(this.tableName, filterQueryGenerator);
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

        public RemoveEntriesFoiQuery build() {
            return new RemoveEntriesFoiQuery(this);
        }
    }

    public static class Builder extends RemoveEntriesFoiQuery.Init<Builder> {
        @Override
        protected RemoveEntriesFoiQuery.Builder self() {
            return this;
        }
    }
}
