/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis.builder.query.procedure;

import org.sensorhub.api.datastore.procedure.ProcedureFilter;
import org.sensorhub.impl.datastore.postgis.builder.filter.procedure.SelectProcedureFilterQuery;
import org.sensorhub.impl.datastore.postgis.builder.query.SelectEntriesQuery;

public class SelectEntriesProcedureQuery extends SelectEntriesQuery {
    protected static abstract class Init<T extends SelectEntriesProcedureQuery.Init<T>> extends SelectEntriesQuery.Init<T> {

        public T withProcedureFilter(ProcedureFilter filter) {
            if(filter != null) {
                SelectProcedureFilterQuery filterQuery = new SelectProcedureFilterQuery(this.tableName, filterQueryGenerator);
                filterQueryGenerator = filterQuery.build(filter);
            }
            return self();
        }

        public SelectEntriesProcedureQuery build() {
            return new SelectEntriesProcedureQuery(this);
        }
    }

    public static class Builder extends SelectEntriesProcedureQuery.Init<SelectEntriesProcedureQuery.Builder> {
        @Override
        protected SelectEntriesProcedureQuery.Builder self() {
            return this;
        }
    }

    protected SelectEntriesProcedureQuery(SelectEntriesProcedureQuery.Init<?> init) {
        super(init);
    }
}
