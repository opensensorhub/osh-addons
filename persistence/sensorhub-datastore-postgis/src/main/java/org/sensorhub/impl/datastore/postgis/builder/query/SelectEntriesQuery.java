package org.sensorhub.impl.datastore.postgis.builder.query;

import org.sensorhub.api.datastore.ValueField;
import org.sensorhub.impl.datastore.postgis.builder.generator.SelectFilterQueryGenerator;

import java.util.Set;
import java.util.stream.Collectors;

public class SelectEntriesQuery extends EntriesQuery<SelectFilterQueryGenerator> {

    protected static abstract class Init<T extends SelectEntriesQuery.Init<T>> extends EntriesQuery.Init<T, SelectFilterQueryGenerator> {
        protected Init() {
            super(new SelectFilterQueryGenerator());
        }

        protected abstract T self();

        public T withFields(Set<? extends ValueField> valueFields) {
            if(valueFields != null && !valueFields.isEmpty()) {
                filterQueryGenerator.setSelectedFields(valueFields.stream().map(ValueField::toString).collect(Collectors.toList()));
                filterQueryGenerator.addSelectField("id");
            } else {
                filterQueryGenerator.addSelectField("*");
            }
            return self();
        }

        public T withLimit(long limit) {
            if(limit != Long.MAX_VALUE) {
                filterQueryGenerator.setLimit(limit);
            }
            return self();
        }

        public SelectEntriesQuery build() {
            return new SelectEntriesQuery(this);
        }
    }

    protected SelectEntriesQuery(EntriesQuery.Init<?, SelectFilterQueryGenerator> init) {
        super(init);
    }

    public String toCountQuery() {
        return this.filterQueryGenerator.toCountQuery();
    }

}
