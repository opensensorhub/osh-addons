package org.sensorhub.impl.datastore.postgis.builder.query.feature;

import org.sensorhub.api.datastore.feature.FoiFilter;
import org.sensorhub.api.datastore.obs.IDataStreamStore;
import org.sensorhub.api.datastore.obs.IObsStore;
import org.sensorhub.api.datastore.system.ISystemDescStore;
import org.sensorhub.impl.datastore.postgis.builder.generator.FilterQueryGenerator;
import org.sensorhub.impl.datastore.postgis.builder.query.EntriesQuery;

//todo: TO IMPLEMENT
public class EntriesFoiQuery<FQ extends FilterQueryGenerator> extends EntriesQuery<FQ>  {

    protected static abstract class Init<T extends EntriesFoiQuery.Init<T,FQ>, FQ> extends EntriesQuery.Init<T, FilterQueryGenerator> {
        private IObsStore obsStore;
        private ISystemDescStore systemDescStore;
        private IDataStreamStore dataStreamStore;

        protected Init(FilterQueryGenerator filterQueryGenerator) {
            super(filterQueryGenerator);
        }

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
           /* if (filter != null) {
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
            }*/
            return self();
        }

        public EntriesFoiQuery<FilterQueryGenerator> build() {
            return new EntriesFoiQuery<>(this);
        }
    }

    protected EntriesFoiQuery(EntriesQuery.Init<?, FQ> init) {
        super(init);
    }

}
