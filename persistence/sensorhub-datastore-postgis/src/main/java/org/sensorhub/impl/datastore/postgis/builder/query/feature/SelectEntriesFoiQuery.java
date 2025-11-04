package org.sensorhub.impl.datastore.postgis.builder.query.feature;

import org.sensorhub.api.datastore.feature.FoiFilter;
import org.sensorhub.api.datastore.obs.IDataStreamStore;
import org.sensorhub.api.datastore.obs.IObsStore;
import org.sensorhub.api.datastore.system.ISystemDescStore;
import org.sensorhub.impl.datastore.postgis.builder.filter.feature.SelectFoiFilterQuery;
import org.sensorhub.impl.datastore.postgis.builder.generator.SelectFilterQueryGenerator;
import org.sensorhub.impl.datastore.postgis.builder.query.EntriesQuery;
import org.sensorhub.impl.datastore.postgis.builder.query.SelectEntriesQuery;

public class SelectEntriesFoiQuery extends SelectEntriesQuery {

    protected SelectEntriesFoiQuery(EntriesQuery.Init<?, SelectFilterQueryGenerator> init) {
        super(init);
    }

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
                SelectFoiFilterQuery filterQuery = new SelectFoiFilterQuery(this.tableName, filterQueryGenerator);
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

    public static class Builder extends SelectEntriesFoiQuery.Init<Builder> {
        @Override
        protected Builder self() {
            return this;
        }
    }
}
