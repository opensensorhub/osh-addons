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

import org.sensorhub.api.datastore.command.CommandStatusFilter;
import org.sensorhub.api.datastore.command.ICommandStore;
import org.sensorhub.api.datastore.command.ICommandStreamStore;
import org.sensorhub.api.datastore.system.ISystemDescStore;
import org.sensorhub.impl.datastore.postgis.builder.filter.command.SelectCommandStatusFilterQuery;
import org.sensorhub.impl.datastore.postgis.builder.query.SelectEntriesQuery;

public class SelectEntriesCommandStatusQuery extends SelectEntriesQuery {
    private ISystemDescStore systemDescStore;
    private ICommandStreamStore commandStreamStore;
    private ICommandStore commandStore;

    protected static abstract class Init<T extends SelectEntriesCommandStatusQuery.Init<T>> extends SelectEntriesQuery.Init<T> {
        private ISystemDescStore systemDescStore;
        private ICommandStreamStore commandStreamStore;
        private ICommandStore commandStore;

        public T linkTo(ISystemDescStore systemDescStore) {
            this.systemDescStore = systemDescStore;
            return self();
        }

        public T linkTo(ICommandStreamStore commandStreamStore) {
            this.commandStreamStore = commandStreamStore;
            return self();
        }

        public T linkTo(ICommandStore commandStore) {
            this.commandStore = commandStore;
            return self();
        }
        public T withStatusFilter(CommandStatusFilter filter) {
            if(filter != null) {
                SelectCommandStatusFilterQuery commandStatusFilterQuery = new SelectCommandStatusFilterQuery(this.tableName, filterQueryGenerator);
                commandStatusFilterQuery.setCommandStreamTableName(commandStreamStore.getDatastoreName());
                commandStatusFilterQuery.setCommandTableName(commandStore.getDatastoreName());
                filterQueryGenerator = commandStatusFilterQuery.build(filter);
            }
            return self();
        }

        public SelectEntriesCommandStatusQuery build() {
            return new SelectEntriesCommandStatusQuery(this);
        }
    }

    public static class Builder extends SelectEntriesCommandStatusQuery.Init<SelectEntriesCommandStatusQuery.Builder> {
        @Override
        protected SelectEntriesCommandStatusQuery.Builder self() {
            return this;
        }
    }

    protected SelectEntriesCommandStatusQuery(SelectEntriesCommandStatusQuery.Init<?> init) {
        super(init);
        this.systemDescStore = init.systemDescStore;
        this.commandStreamStore = init.commandStreamStore;
        this.commandStore = init.commandStore;
    }
}
