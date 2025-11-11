/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis.builder.query.feature;

import org.sensorhub.api.datastore.system.SystemFilter;
import org.sensorhub.impl.datastore.postgis.builder.filter.feature.BaseSystemWithDescFilterQuery;
import org.sensorhub.impl.datastore.postgis.builder.filter.feature.SelectSystemWithDescFilterQuery;
import org.sensorhub.impl.datastore.postgis.builder.generator.SelectFilterQueryGenerator;
import org.sensorhub.impl.datastore.postgis.builder.query.SelectEntriesQuery;

public class SelectEntriesSystemQuery extends SelectEntriesQuery {
    protected static abstract class Init<T extends SelectEntriesSystemQuery.Init<T>> extends SelectEntriesQuery.Init<T> {
        public T withSystemFilter(SystemFilter filter) {
            if(filter != null) {
                SelectSystemWithDescFilterQuery filterQuery =
                                                new SelectSystemWithDescFilterQuery(this.tableName, filterQueryGenerator);
                filterQueryGenerator = filterQuery.build(filter);
            }
            return self();
        }

        public SelectEntriesSystemQuery build() {
            return new SelectEntriesSystemQuery(this);
        }
    }

    public static class Builder extends SelectEntriesSystemQuery.Init<SelectEntriesSystemQuery.Builder> {
        @Override
        protected SelectEntriesSystemQuery.Builder self() {
            return this;
        }
    }

    protected SelectEntriesSystemQuery(Init<?> init) {
        super(init);
    }
}
