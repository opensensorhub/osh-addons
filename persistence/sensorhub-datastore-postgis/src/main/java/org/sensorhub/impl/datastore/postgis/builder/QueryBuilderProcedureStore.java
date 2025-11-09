/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis.builder;

import org.sensorhub.api.datastore.procedure.IProcedureStore;
import org.sensorhub.api.datastore.procedure.ProcedureFilter;
import org.sensorhub.api.procedure.IProcedureWithDesc;
import org.sensorhub.impl.datastore.postgis.builder.query.procedure.RemoveEntriesProcedureQuery;
import org.sensorhub.impl.datastore.postgis.builder.query.procedure.SelectEntriesProcedureQuery;

import java.util.Set;

public class QueryBuilderProcedureStore extends QueryBuilderBaseFeatureStore<IProcedureWithDesc, IProcedureStore.ProcedureField, ProcedureFilter> {
    public final static String FEATURE_TABLE_NAME = "procedure";

    public QueryBuilderProcedureStore() {
        super(FEATURE_TABLE_NAME);
    }

    public QueryBuilderProcedureStore(String tableName) {
        super(tableName);
    }

    @Override
    public String createSelectEntriesQuery(ProcedureFilter filter, Set<IProcedureStore.ProcedureField> fields) {
        SelectEntriesProcedureQuery selectEntriesProcedureQuery = new SelectEntriesProcedureQuery.Builder()
                .tableName(this.getStoreTableName())
                .withFields(fields)
                .withProcedureFilter(filter)
                .build();
        return selectEntriesProcedureQuery.toQuery();
    }

    @Override
    public String createRemoveEntriesQuery(ProcedureFilter filter) {
        RemoveEntriesProcedureQuery removeEntriesProcedureQuery = new RemoveEntriesProcedureQuery.Builder()
                .tableName(this.getStoreTableName())
                .withProcedureFilter(filter)
                .build();
        return removeEntriesProcedureQuery.toQuery();
    }
}
