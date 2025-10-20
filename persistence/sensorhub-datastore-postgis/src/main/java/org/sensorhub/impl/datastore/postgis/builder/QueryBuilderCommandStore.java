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

import org.sensorhub.api.datastore.command.CommandFilter;
import org.sensorhub.api.datastore.command.ICommandStatusStore;
import org.sensorhub.api.datastore.command.ICommandStore;
import org.sensorhub.api.datastore.command.ICommandStreamStore;
import org.sensorhub.api.datastore.feature.IFoiStore;
import org.sensorhub.api.datastore.system.ISystemDescStore;
import org.sensorhub.impl.datastore.postgis.builder.filter.SelectEntriesCommandQuery;

import java.util.Set;

import static org.sensorhub.api.datastore.command.ICommandStore.CommandField.*;
import static org.sensorhub.api.datastore.obs.IObsStore.ObsField.FOI_ID;

public class QueryBuilderCommandStore extends QueryBuilder {
    public final static String COMMAND_TABLE_NAME = "command";

    public QueryBuilderCommandStore() {
        this(COMMAND_TABLE_NAME);
    }

    public QueryBuilderCommandStore(String tableName) {
        super(tableName);
    }


    public String createTableQuery() {
        return "CREATE TABLE "+this.getStoreTableName()+
                " (" +
                "id BIGSERIAL PRIMARY KEY,"+
                COMMANDSTREAM_ID +" BIGINT, "+
                SENDER_ID+" VARCHAR,"+
                FOI_ID+" bigint,"+
                ISSUE_TIME+" TIMESTAMPTZ,"+
                STATUS+" VARCHAR,"+
                ERROR_MSG+" VARCHAR,"+
                PARAMETERS+" JSONB" +
                ")";
    }

    public String createDataIndexQuery() {
        return "CREATE INDEX "+this.getStoreTableName()+"_data_idx on "+this.getStoreTableName()+" USING GIN("+PARAMETERS+")";
    }

    public String createCommandStreamIndexQuery() {
        return "CREATE INDEX "+this.getStoreTableName()+"_commandstream_idx on "+this.getStoreTableName()+" ("+COMMANDSTREAM_ID+")";
    }

    public String createSenderIdIndexQuery() {
        return "CREATE INDEX "+this.getStoreTableName()+"_senderid_idx on "+this.getStoreTableName()+" ("+SENDER_ID+")";
    }

    public String createFoidIdIndexQuery() {
        return "CREATE INDEX "+this.getStoreTableName()+"_foidid_idx on "+this.getStoreTableName()+" ("+FOI_ID+")";
    }

    public String createIssueTimeIndexQuery() {
        return "CREATE INDEX "+this.getStoreTableName()+"_issue_time_idx on "+this.getStoreTableName()+" ("+ ISSUE_TIME +")";
    }

    public String insertCommandQuery() {
        return "INSERT INTO "+this.getStoreTableName()+" ("+COMMANDSTREAM_ID+", "+SENDER_ID+", "+FOI_ID+","+ISSUE_TIME+", "+PARAMETERS+") VALUES (?,?,?,?,?)";
    }

    public String updateByIdQuery() {
        return "UPDATE "+this.getStoreTableName()+" SET "+
                COMMANDSTREAM_ID+" = ?, " +
                SENDER_ID+" = ?, " +
                FOI_ID+" = ?, " +
                ISSUE_TIME+" = ?, " +
                PARAMETERS+" = ? " +
                "WHERE id = ?";
    }

    public String updateStatusByIdQuery() {
        return "UPDATE "+this.getStoreTableName()+" SET "+STATUS+" = ?, WHERE id = ?";
    }

    public String updateErrorMessageByIdQuery() {
        return "UPDATE "+this.getStoreTableName()+" SET "+ERROR_MSG+" = ?, WHERE id = ?";
    }

    public String createSelectEntriesQuery(CommandFilter filter, Set<ICommandStore.CommandField> fields) {
        SelectEntriesCommandQuery selectEntriesCommandQueryBuilder = new SelectEntriesCommandQuery.Builder()
                .tableName(this.getStoreTableName())
                .linkTo(this.systemStore)
                .linkTo(this.commandStreamStore)
                .linkTo(this.commandStatusStore)
                .withCommandFilter(filter)
                .withFields(fields)
                .build();
        return selectEntriesCommandQueryBuilder.toQuery();
    }

}
