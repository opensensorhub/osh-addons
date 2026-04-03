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

import org.sensorhub.api.datastore.command.CommandFilter;
import org.sensorhub.api.datastore.command.ICommandStatusStore;
import org.sensorhub.api.datastore.command.ICommandStreamStore;
import org.sensorhub.api.datastore.system.ISystemDescStore;
import org.sensorhub.impl.datastore.postgis.builder.filter.command.RemoveCommandFilterQuery;
import org.sensorhub.impl.datastore.postgis.builder.query.RemoveEntriesQuery;

public class RemoveEntriesCommandQuery extends RemoveEntriesQuery {
    private ISystemDescStore systemDescStore;
    private ICommandStreamStore commandStreamStore;
    private ICommandStatusStore commandStatusStore;

    protected static abstract class Init<T extends RemoveEntriesCommandQuery.Init<T>> extends RemoveEntriesQuery.Init<T> {
        private ISystemDescStore systemDescStore;
        private ICommandStreamStore commandStreamStore;
        private ICommandStatusStore commandStatusStore;

        public T linkTo(ISystemDescStore systemDescStore) {
            this.systemDescStore = systemDescStore;
            return self();
        }

        public T linkTo(ICommandStreamStore commandStreamStore) {
            this.commandStreamStore = commandStreamStore;
            return self();
        }

        public T linkTo(ICommandStatusStore commandStatusStore) {
            this.commandStatusStore = commandStatusStore;
            return self();
        }
        public T withCommandFilter(CommandFilter filter) {
            if(filter != null) {
                RemoveCommandFilterQuery commandFilterQuery = new RemoveCommandFilterQuery(this.tableName, filterQueryGenerator);
                commandFilterQuery.setCommandStreamTableName(commandStreamStore.getDatastoreName());
                commandFilterQuery.setCommandStatusTableName(commandStatusStore.getDatastoreName());
                if(systemDescStore != null) {
                    commandFilterQuery.setSysDescTableName(systemDescStore.getDatastoreName());
                }
                filterQueryGenerator = commandFilterQuery.build(filter);
            }
            return self();
        }

        public RemoveEntriesCommandQuery build() {
            return new RemoveEntriesCommandQuery(this);
        }
    }

    public static class Builder extends RemoveEntriesCommandQuery.Init<RemoveEntriesCommandQuery.Builder> {
        @Override
        protected RemoveEntriesCommandQuery.Builder self() {
            return this;
        }
    }

    protected RemoveEntriesCommandQuery(RemoveEntriesCommandQuery.Init<?> init) {
        super(init);
        this.systemDescStore = init.systemDescStore;
        this.commandStreamStore = init.commandStreamStore;
        this.commandStatusStore = init.commandStatusStore;
    }
}
