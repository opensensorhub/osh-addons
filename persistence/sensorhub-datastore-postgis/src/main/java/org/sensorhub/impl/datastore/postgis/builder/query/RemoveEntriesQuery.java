package org.sensorhub.impl.datastore.postgis.builder.query;

import org.sensorhub.api.datastore.ValueField;
import org.sensorhub.impl.datastore.postgis.builder.generator.RemoveFilterQueryGenerator;

import java.util.Set;

public class RemoveEntriesQuery extends EntriesQuery<RemoveFilterQueryGenerator> {

    protected static abstract class Init<T extends RemoveEntriesQuery.Init<T>> extends EntriesQuery.Init<T, RemoveFilterQueryGenerator> {
        protected Init() {
            super(new RemoveFilterQueryGenerator());
        }

        protected abstract T self();

        public T withFields(Set<? extends ValueField> valueFields) {
            return self();
        }

        public RemoveEntriesQuery build() {
            return new RemoveEntriesQuery(this);
        }
    }

    protected RemoveEntriesQuery(EntriesQuery.Init<?, RemoveFilterQueryGenerator> init) {
        super(init);
    }
}
