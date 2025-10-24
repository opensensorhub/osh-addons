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

import org.sensorhub.api.datastore.ValueField;

import java.util.Set;
import java.util.stream.Collectors;

public class SelectEntriesQuery {

    protected FilterQueryGenerator filterQueryGenerator;

    protected String tableName;

    protected static abstract class Init<T extends Init<T>> {
        protected String tableName;
        protected FilterQueryGenerator filterQueryGenerator = new FilterQueryGenerator();

        protected abstract T self();

        public T tableName(String tableName) {
            this.tableName = tableName;
            filterQueryGenerator.tableName(tableName);
            return self();
        }

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

    public static class Builder extends Init<Builder> {
        @Override
        protected Builder self() {
            return this;
        }
    }

    protected SelectEntriesQuery(Init<?> init) {
        this.tableName = init.tableName;
        this.filterQueryGenerator = init.filterQueryGenerator;
    }

    public String toQuery() {
        return this.filterQueryGenerator.toQuery();
    }

    public String toCountQuery() {
        return this.filterQueryGenerator.toCountQuery();
    }
}
