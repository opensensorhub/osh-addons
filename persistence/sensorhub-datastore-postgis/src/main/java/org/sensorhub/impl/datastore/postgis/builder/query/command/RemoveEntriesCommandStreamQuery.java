/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis.builder.query.command;

import org.sensorhub.api.datastore.command.CommandStreamFilter;
import org.sensorhub.api.datastore.system.ISystemDescStore;
import org.sensorhub.impl.datastore.postgis.builder.filter.command.RemoveCommandStreamFilterQuery;
import org.sensorhub.impl.datastore.postgis.builder.query.RemoveEntriesQuery;

public class RemoveEntriesCommandStreamQuery extends RemoveEntriesQuery {
    private ISystemDescStore systemDescStore;

    protected static abstract class Init<T extends RemoveEntriesCommandStreamQuery.Init<T>> extends RemoveEntriesQuery.Init<T> {
        private ISystemDescStore systemDescStore;

        public T linkTo(ISystemDescStore systemDescStore) {
            this.systemDescStore = systemDescStore;
            return self();
        }

        public T withCommandStreamFilter(CommandStreamFilter filter) {
            if(filter != null) {
                RemoveCommandStreamFilterQuery commandStreamFilterQuery = new RemoveCommandStreamFilterQuery(this.tableName, filterQueryGenerator);
                if(systemDescStore != null) {
                    commandStreamFilterQuery.setSysDescTableName(systemDescStore.getDatastoreName());
                }
                filterQueryGenerator = commandStreamFilterQuery.build(filter);
            }
            return self();
        }

        public RemoveEntriesCommandStreamQuery build() {
            return new RemoveEntriesCommandStreamQuery(this);
        }
    }

    public static class Builder extends RemoveEntriesCommandStreamQuery.Init<RemoveEntriesCommandStreamQuery.Builder> {
        @Override
        protected RemoveEntriesCommandStreamQuery.Builder self() {
            return this;
        }
    }

    protected RemoveEntriesCommandStreamQuery(RemoveEntriesCommandStreamQuery.Init<?> init) {
        super(init);
        this.systemDescStore = init.systemDescStore;
    }
}
