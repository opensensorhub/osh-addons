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

import org.sensorhub.api.datastore.command.CommandStreamFilter;
import org.sensorhub.api.datastore.command.ICommandStreamStore;
import org.sensorhub.api.datastore.system.ISystemDescStore;
import org.sensorhub.impl.datastore.postgis.builder.filter.SelectEntriesCommandStreamQuery;
import org.sensorhub.impl.datastore.postgis.builder.filter.SelectEntriesDataStreamQuery;

import java.util.Set;

public class QueryBuilderCommandStreamStore extends QueryBuilder {
    public final static String COMMAND_STREAM_TABLE_NAME = "commandstream";

    public QueryBuilderCommandStreamStore() {
        this(COMMAND_STREAM_TABLE_NAME);
    }

    public QueryBuilderCommandStreamStore(String tableName) {
        super(tableName);
    }

    public String createTableQuery() {
        return "CREATE TABLE " + this.getStoreTableName() + " (id BIGSERIAL PRIMARY KEY,data JSONB)";
    }

    public String createIndexQuery() {
        return "CREATE INDEX " + this.getStoreTableName() + "_data_idx on " + this.getStoreTableName() + " USING GIN(data)";
    }

    public String createUniqueIndexQuery() {
        return "CREATE UNIQUE INDEX " + this.getStoreTableName() + "_data_output_idx ON " + this.getStoreTableName()
                + " USING BTREE((data->>'name'), (data->'system@id'), (data->'validTime'->'begin'))";
    }

    public String createValidTimeBeginIndexQuery() {
        return "CREATE INDEX "+this.getStoreTableName()+"_date_range_begin_idx ON "+this.getStoreTableName()+ " USING GIN((data->'validTime'->'begin'))";
    }

    public String createValidTimeEndIndexQuery() {
        return "CREATE INDEX "+this.getStoreTableName()+"_date_range_end_idx ON "+this.getStoreTableName()+ " USING GIN((data->'validTime'->'end'))";
    }

    public String insertCommandQuery() {
        return "INSERT INTO " + this.getStoreTableName() + " (data) VALUES (?) ";
    }

    public String updateByIdQuery() {
        return "UPDATE " + this.getStoreTableName() + " SET data = ? WHERE id = ?";
    }
    public String updateValidTimeByIdQuery() {
        return "UPDATE " + this.getStoreTableName() + " SET data = jsonb_set(data, '{validTime}'::text[], to_jsonb(?), true) WHERE id = ?";
    }

    public String createTrigramExtensionQuery() {
        return "CREATE EXTENSION IF NOT EXISTS pg_trgm";
    }

    public String createTrigramDescriptionFullTextIndexQuery() {
        return "CREATE INDEX "+this.getStoreTableName()+"_desc_full_text_datastream_idx ON  "+this.getStoreTableName()+" USING GIN ((data->'recordSchema'->>'description') gin_trgm_ops)";
    }

    public String createSelectEntriesQuery(CommandStreamFilter filter, Set<ICommandStreamStore.CommandStreamInfoField> fields) {
        SelectEntriesCommandStreamQuery selectEntriesCommandStreamQuery = new SelectEntriesCommandStreamQuery.Builder()
                .tableName(this.getStoreTableName())
                .linkTo(this.systemStore)
                .withFields(fields)
                .withCommandStreamFilter(filter)
                .build();
        return selectEntriesCommandStreamQuery.toQuery();
    }
}
