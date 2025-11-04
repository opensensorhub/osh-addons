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

import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.IDataStreamStore;
import org.sensorhub.impl.datastore.postgis.builder.query.datastream.SelectEntriesDataStreamQuery;
import org.sensorhub.impl.datastore.postgis.utils.PostgisUtils;

import java.time.Instant;
import java.util.Set;

public class QueryBuilderDataStreamStore extends QueryBuilder {
    public final static String DATASTREAM_TABLE_NAME = "datastreams";

    public QueryBuilderDataStreamStore() {
        this(DATASTREAM_TABLE_NAME);
    }

    public QueryBuilderDataStreamStore(String tableName) {
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
                + " USING BTREE((data->'name'), (data->'system@id'), (data->'validTime'))";
    }

    public String createDateRangeIndexQuery() {
        return "CREATE INDEX "+this.getStoreTableName()+"_date_range_idx ON " + this.getStoreTableName() +
                " USING gist (int8range( " +
                "(data->'validTime'->'begin')::bigint, " +
                "(data->'validTime'->'end')::bigint" +
                ") )";
    }

    public String createValidTimeBeginIndexQuery() {
        return "CREATE INDEX "+this.getStoreTableName()+"_date_range_begin_idx ON " + this.getStoreTableName() + " USING GIN((data->'validTime'->'begin'))";
    }

    public String createValidTimeEndIndexQuery() {
        return "CREATE INDEX "+this.getStoreTableName()+"_date_range_end_idx ON " + this.getStoreTableName() + " USING GIN((data->'validTime'->'end'))";
    }

    public String createTrigramExtensionQuery() {
        return "CREATE EXTENSION IF NOT EXISTS pg_trgm";
    }

    public String createTrigramDescriptionFullTextIndexQuery() {
        return "CREATE INDEX "+this.getStoreTableName()+"_desc_full_text_datastream_idx ON  " + this.getStoreTableName() + " USING GIN ((data->'recordSchema'->>'description') gin_trgm_ops)";
    }

    //SELECT * from test_obs_datastreams where to_tsvector(data->'recordStruct'->'description') @@ to_tsquery('video | Air');
    public String createDescriptionFullTextIndexQuery() {
        return "CREATE INDEX "+this.getStoreTableName()+"_desc_full_text_datastream_idx ON " + this.getStoreTableName() + " USING " +
                "GIN (to_tsvector('english', data->'recordSchema'->'description'))";
    }

    public String updateByIdQuery() {
        return "UPDATE " + this.getStoreTableName() + " SET data = ? WHERE id = ?";
    }

    public String updateValidTimeByIdQuery() {
        return "UPDATE " + this.getStoreTableName() + " SET data = jsonb_set(data, '{validTime}'::text[], to_jsonb(?), true) WHERE id = ?";
    }

    public String insertInfoQuery() {
        return "INSERT INTO " + this.getStoreTableName() + " (data) SELECT (?) ";
    }

    public String getAllDataStreams(Instant min, Instant max) {
        return "SELECT id FROM "+this.getStoreTableName()+" WHERE tstzrange((data->'validTime'->>'begin')::timestamptz," +
                "(data->'validTime'->>'end')::timestamptz) <@ '["+ PostgisUtils.checkAndGetValidInstant(min)+","+PostgisUtils.checkAndGetValidInstant(max)+"]'::tstzrange";
    }

    public String createSelectEntriesQuery(DataStreamFilter filter, Set<IDataStreamStore.DataStreamInfoField> fields) {
        SelectEntriesDataStreamQuery selectEntriesDataStreamQuery = new SelectEntriesDataStreamQuery.Builder()
                .tableName(this.getStoreTableName())
                .linkTo(this.systemStore)
                .withFields(fields)
                .withDataStreamFilter(filter)
                .build();
        return selectEntriesDataStreamQuery.toQuery();
    }
}
