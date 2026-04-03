package org.sensorhub.impl.datastore.postgis.builder.query.feature;

import org.sensorhub.api.datastore.feature.FeatureFilter;
import org.sensorhub.impl.datastore.postgis.builder.filter.feature.RemoveFeatureFilterQuery;
import org.sensorhub.impl.datastore.postgis.builder.generator.RemoveFilterQueryGenerator;
import org.sensorhub.impl.datastore.postgis.builder.query.EntriesQuery;
import org.sensorhub.impl.datastore.postgis.builder.query.RemoveEntriesQuery;

public class RemoveEntriesFeatureQuery extends RemoveEntriesQuery {

    protected RemoveEntriesFeatureQuery(EntriesQuery.Init<?, RemoveFilterQueryGenerator> init) {
        super(init);
    }

    protected static abstract class Init<T extends RemoveEntriesFeatureQuery.Init<T>> extends RemoveEntriesQuery.Init<T> {
        public T withFeatureFilter(FeatureFilter filter) {
            if(filter != null) {
                RemoveFeatureFilterQuery featureFilterQuery = new RemoveFeatureFilterQuery(this.tableName, filterQueryGenerator);
                filterQueryGenerator = featureFilterQuery.build(filter);
            }
            return self();
        }

        public RemoveEntriesFeatureQuery build() {
            return new RemoveEntriesFeatureQuery(this);
        }
    }

    public static class Builder extends RemoveEntriesFeatureQuery.Init<Builder> {
        @Override
        protected Builder self() {
            return this;
        }
    }
}
