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

import org.sensorhub.api.datastore.command.CommandFilter;
import org.sensorhub.api.datastore.command.ICommandStatusStore;
import org.sensorhub.api.datastore.command.ICommandStreamStore;
import org.sensorhub.api.datastore.system.ISystemDescStore;

public class SelectEntriesCommandQuery extends SelectEntriesQuery {
    private ISystemDescStore systemDescStore;
    private ICommandStreamStore commandStreamStore;
    private ICommandStatusStore commandStatusStore;
    protected static abstract class Init<T extends SelectEntriesCommandQuery.Init<T>> extends SelectEntriesQuery.Init<T> {
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
                CommandFilterQuery commandFilterQuery = new CommandFilterQuery(this.tableName, filterQueryGenerator);
                commandFilterQuery.setCommandStreamTableName(commandStreamStore.getDatastoreName());
                commandFilterQuery.setCommandStatusTableName(commandStatusStore.getDatastoreName());
                if(systemDescStore != null) {
                    commandFilterQuery.setSysDescTableName(systemDescStore.getDatastoreName());
                }
                filterQueryGenerator = commandFilterQuery.build(filter);
            }
            return self();
        }

        public SelectEntriesCommandQuery build() {
            return new SelectEntriesCommandQuery(this);
        }
    }

    public static class Builder extends SelectEntriesCommandQuery.Init<SelectEntriesCommandQuery.Builder> {
        @Override
        protected SelectEntriesCommandQuery.Builder self() {
            return this;
        }
    }

    protected SelectEntriesCommandQuery(SelectEntriesCommandQuery.Init<?> init) {
        super(init);
        this.systemDescStore = init.systemDescStore;
        this.commandStreamStore = init.commandStreamStore;
        this.commandStatusStore = init.commandStatusStore;
    }
}
