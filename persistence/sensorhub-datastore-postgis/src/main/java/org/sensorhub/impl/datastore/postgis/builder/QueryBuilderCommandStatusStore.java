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

import org.sensorhub.api.datastore.command.CommandStatusFilter;
import org.sensorhub.api.datastore.command.ICommandStatusStore;
import org.sensorhub.impl.datastore.postgis.builder.query.command.SelectEntriesCommandStatusQuery;

import java.util.Set;

import static org.sensorhub.api.datastore.command.ICommandStatusStore.CommandStatusField.*;

public class QueryBuilderCommandStatusStore extends QueryBuilder {
    public final static String COMMAND_TABLE_NAME = "commandstatus";

    protected static final String PROGRESS = "progress";
    protected static final String MESSAGE = "message";
    protected static final String RESULT = "result";

    public QueryBuilderCommandStatusStore() {
        super(COMMAND_TABLE_NAME);
    }

    protected QueryBuilderCommandStatusStore(String tableName) {
        super(tableName);
    }

    @Override
    public String createTableQuery() {
        return "CREATE TABLE "+this.getStoreTableName()+
                " (" +
                "id BIGSERIAL PRIMARY KEY,"+
                COMMAND_ID+" BIGINT, "+
                PROGRESS+" INT, "+
                REPORT_TIME+" TIMESTAMP, "+
                STATUS_CODE+" VARCHAR, "+
                EXEC_TIME+" tsrange, "+
                MESSAGE+" VARCHAR, "+
                RESULT+" json"+
                ")";
    }

    public String insertCommandQuery() {
        return "INSERT INTO "+this.getStoreTableName()+" " +
                "(commandid, progress, reportTime, statusCode, executionTime, message, result) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
    }

    public String updateByIdQuery() {
        return "UPDATE "+this.getStoreTableName()+" SET "+
                COMMAND_ID+" = ?, " +
                PROGRESS+" = ?, " +
                REPORT_TIME+" = ?, " +
                STATUS_CODE+" = ?, " +
                EXEC_TIME+" = ?, " +
                MESSAGE+" = ?, " +
                RESULT+" = ?, " +
                "WHERE id = ?";
    }

    public String createSelectEntriesQuery(CommandStatusFilter filter, Set<ICommandStatusStore.CommandStatusField> fields) {
        SelectEntriesCommandStatusQuery selectEntriesCommandStatusQuery = new SelectEntriesCommandStatusQuery.Builder()
                .tableName(this.getStoreTableName())
                .linkTo(this.systemStore)
                .linkTo(this.commandStreamStore)
                .linkTo(this.commandStore)
                .withStatusFilter(filter)
                .withFields(fields)
                .build();
        return selectEntriesCommandStatusQuery.toQuery();
    }
}
