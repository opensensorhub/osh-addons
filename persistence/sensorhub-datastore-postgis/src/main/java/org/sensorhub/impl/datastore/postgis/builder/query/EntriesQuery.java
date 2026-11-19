/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis.builder.query;

import org.sensorhub.impl.datastore.postgis.builder.generator.FilterQueryGenerator;

public class EntriesQuery<V extends FilterQueryGenerator> {

    protected V filterQueryGenerator;
    protected String tableName;

    protected static abstract class Init<T extends Init<T,F>, F extends FilterQueryGenerator> {
        protected String tableName;
        protected F filterQueryGenerator;

        protected Init(F filterQueryGenerator) {
            this.filterQueryGenerator = filterQueryGenerator;
        }

        protected abstract T self();

        public T tableName(String tableName) {
            this.tableName = tableName;
            filterQueryGenerator.tableName(tableName);
            return self();
        }
    }

    public static class Builder extends Init<Builder, FilterQueryGenerator> {
        protected Builder(FilterQueryGenerator filterQueryGenerator) {
            super(filterQueryGenerator);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }

    protected EntriesQuery(Init<?,V> init) {
        this.tableName = init.tableName;
        this.filterQueryGenerator = init.filterQueryGenerator;
    }

    public String toQuery() {
        return this.filterQueryGenerator.toQuery();
    }

}
