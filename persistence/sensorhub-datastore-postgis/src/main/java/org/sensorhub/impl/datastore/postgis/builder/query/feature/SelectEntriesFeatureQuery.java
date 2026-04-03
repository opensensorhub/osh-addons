package org.sensorhub.impl.datastore.postgis.builder.query.feature;

import org.sensorhub.api.datastore.feature.FeatureFilter;
import org.sensorhub.impl.datastore.postgis.builder.filter.feature.SelectFeatureFilterQuery;
import org.sensorhub.impl.datastore.postgis.builder.generator.SelectFilterQueryGenerator;
import org.sensorhub.impl.datastore.postgis.builder.query.EntriesQuery;
import org.sensorhub.impl.datastore.postgis.builder.query.SelectEntriesQuery;

public class SelectEntriesFeatureQuery extends SelectEntriesQuery {

    protected SelectEntriesFeatureQuery(EntriesQuery.Init<?, SelectFilterQueryGenerator> init) {
        super(init);
    }

    protected static abstract class Init<T extends SelectEntriesFeatureQuery.Init<T>> extends SelectEntriesQuery.Init<T> {
        public T withFeatureFilter(FeatureFilter filter) {
            if(filter != null) {
                SelectFeatureFilterQuery featureFilterQuery = new SelectFeatureFilterQuery(this.tableName, filterQueryGenerator);
                filterQueryGenerator = featureFilterQuery.build(filter);
            }
            return self();
        }

        public SelectEntriesFeatureQuery build() {
            return new SelectEntriesFeatureQuery(this);
        }
    }

    public static class Builder extends SelectEntriesFeatureQuery.Init<Builder> {
        @Override
        protected Builder self() {
            return this;
        }
    }
}
