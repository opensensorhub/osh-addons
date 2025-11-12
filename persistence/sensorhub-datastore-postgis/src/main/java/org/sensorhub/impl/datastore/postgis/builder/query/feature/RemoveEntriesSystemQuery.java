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
import org.sensorhub.impl.datastore.postgis.builder.filter.feature.RemoveSystemWithDescFilterQuery;
import org.sensorhub.impl.datastore.postgis.builder.filter.feature.SelectSystemWithDescFilterQuery;
import org.sensorhub.impl.datastore.postgis.builder.generator.RemoveFilterQueryGenerator;
import org.sensorhub.impl.datastore.postgis.builder.query.RemoveEntriesQuery;

public class RemoveEntriesSystemQuery extends RemoveEntriesQuery {
    protected static abstract class Init<T extends RemoveEntriesSystemQuery.Init<T>> extends RemoveEntriesQuery.Init<T> {
        public T withSystemFilter(SystemFilter filter) {
            if(filter != null) {
                RemoveSystemWithDescFilterQuery filterQuery =
                                                new RemoveSystemWithDescFilterQuery(this.tableName, filterQueryGenerator);
                filterQueryGenerator = filterQuery.build(filter);
            }
            return self();
        }

        public RemoveEntriesSystemQuery build() {
            return new RemoveEntriesSystemQuery(this);
        }
    }

    public static class Builder extends RemoveEntriesSystemQuery.Init<RemoveEntriesSystemQuery.Builder> {
        @Override
        protected RemoveEntriesSystemQuery.Builder self() {
            return this;
        }
    }

    protected RemoveEntriesSystemQuery(Init<?> init) {
        super(init);
    }
}
