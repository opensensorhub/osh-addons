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

import org.sensorhub.api.datastore.obs.IObsStore;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.datastore.obs.ObsStatsQuery;
import org.sensorhub.impl.datastore.postgis.builder.query.obs.RemoveEntriesObsQuery;
import org.sensorhub.impl.datastore.postgis.builder.query.obs.SelectEntriesObsQuery;
import org.sensorhub.impl.datastore.postgis.builder.query.stats.StatsObsQuery;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.sensorhub.api.datastore.obs.IObsStore.ObsField.*;

public class QueryBuilderObsStore extends QueryBuilder {

    public final static String OBS_STORE_TABLE_NAME = "obs";
    public QueryBuilderObsStore() {
        this(OBS_STORE_TABLE_NAME);
    }

    public QueryBuilderObsStore(String tableName) {
        super(tableName);
    }

    public String createTableQuery() {
        return "CREATE TABLE IF NOT EXISTS "+this.getStoreTableName()+
                " (" +
                "id BIGSERIAL PRIMARY KEY,"+
                DATASTREAM_ID +" bigint, "+
                FOI_ID+" bigint,"+
                PHENOMENON_TIME+" TIMESTAMP,"+
                RESULT_TIME+" TIMESTAMP,"+
                RESULT+" JSON" + // VERSUS JSONB but the parser does not keep order
                ")";
    }
    public String createDataIndexQuery() {
        return "CREATE INDEX "+this.getStoreTableName()+"_data_idx on "+this.getStoreTableName()+" USING GIN("+RESULT+")";
//        return "CREATE INDEX "+this.getStoreTableName()+"_data_idx on "+this.getStoreTableName()+" ("+RESULT+")";
    }

    public String createDataStreamIndexQuery() {
        return "CREATE INDEX IF NOT EXISTS "+this.getStoreTableName()+"_datastream_idx on "+this.getStoreTableName()+" ("+DATASTREAM_ID+")";
    }

    public String createPhenomenonTimeIndexQuery() {
        return "CREATE INDEX IF NOT EXISTS "+this.getStoreTableName()+"_datastream_id_phenomenon_time_idx on "+this.getStoreTableName()+" ("+ DATASTREAM_ID + ", " + PHENOMENON_TIME +")";
    }

    public String createResultTimeIndexQuery() {
        return "CREATE INDEX IF NOT EXISTS "+this.getStoreTableName()+"_datastream_id_result_time_idx on "+this.getStoreTableName()+" ("+ DATASTREAM_ID + ", " + RESULT_TIME +")";
    }

    public String createFoiIndexQuery() {
        return "CREATE INDEX IF NOT EXISTS "+this.getStoreTableName()+"_foi_idx on "+this.getStoreTableName()+" ("+ FOI_ID +")";
    }

    public String insertObsQuery() {
        return "INSERT INTO "+this.getStoreTableName()+" " +
                "("+DATASTREAM_ID+", "+FOI_ID+", "+PHENOMENON_TIME+", "+RESULT_TIME+", "+RESULT+") VALUES (?,?,?,?,?) " +
                "ON CONFLICT (dataStreamID, foiID, phenomenonTime, resultTime) DO UPDATE SET id = "+this.getStoreTableName()+".id RETURNING id";
    }

    public String createUniqueConstraint() {
        return "CREATE UNIQUE INDEX  IF NOT EXISTS "+this.getStoreTableName()+"_unique_constraint on "+this.getStoreTableName()+" (dataStreamID, foiID, phenomenonTime, resultTime)";
    }

    public String updateByIdQuery() {
        return "UPDATE "+this.getStoreTableName()+" SET "+DATASTREAM_ID+" = ?, " +
                FOI_ID+" = ?, " +
                PHENOMENON_TIME+" = ?, " +
                RESULT_TIME+" = ?, " +
                RESULT+" = ? " +
                "WHERE id = ?";
    }

    public String getPhenomenonTimeRangeByDataStreamIdQuery(long dataStreamID) {
        return "SELECT Min("+PHENOMENON_TIME+"),Max("+PHENOMENON_TIME+") FROM "+this.getStoreTableName()+" WHERE "+DATASTREAM_ID+" = "+dataStreamID;
    }

    public String getPhenomenonTimeRangeByDataStreamIdsQuery() {
        return "SELECT " + DATASTREAM_ID + ", " +
                "       MIN(" + PHENOMENON_TIME + ") AS min, " +
                "       MAX(" + PHENOMENON_TIME + ") AS max " +
                "FROM " + getStoreTableName() +
                " WHERE " + DATASTREAM_ID + " = ANY (?) " +
                "GROUP BY " + DATASTREAM_ID;
    }

    public String getBinCountByPhenomenontime(long seconds, List<Long> dsIds, List<Long> foiIds) {
        StringBuilder idsQuery = new StringBuilder();
        for(int i=0;i < dsIds.size();i++) {
            idsQuery.append(" datastreamid = ").append(dsIds.get(i));
            if(i < dsIds.size()) {
                idsQuery.append(" AND ");
            }
        }
        // foiIds filter
        idsQuery.append(" AND ");
        for(int i=0;i < foiIds.size();i++) {
            idsQuery.append(" foiid = ").append(foiIds.get(i));
            if(i < foiIds.size()) {
                idsQuery.append(" AND ");
            }
        }
        return "SELECT COUNT(*), floor((extract('epoch' from "+PHENOMENON_TIME+") / "+seconds+" )) * "+seconds+" as interval_alias " +
                " WHERE "+idsQuery.toString() +
                " FROM "+this.getStoreTableName()+" GROUP BY interval_alias order BY interval_alias";
    }

    public String getResultTimeRangeByDataStreamIdQuery(long dataStreamID) {
        return "SELECT Min("+RESULT_TIME+"),Max("+RESULT_TIME+") FROM "+this.getStoreTableName()+" WHERE "+DATASTREAM_ID+" = "+dataStreamID;
    }

    public String getResultTimeRangeByDataStreamIdsQuery() {
        return "SELECT " + DATASTREAM_ID + ", " +
                "       MIN(" + RESULT_TIME + ") AS min, " +
                "       MAX(" + RESULT_TIME + ") AS max " +
                "FROM " + getStoreTableName() +
                " WHERE " + DATASTREAM_ID + " = ANY (?) " +
                "GROUP BY " + DATASTREAM_ID;
    }

    public String countByPhenomenonTimeRangeQuery(Instant min, Instant max) {
            String minTimestamp = min.toString();
            if(min == Instant.MIN) {
                minTimestamp = "-infinity";
            }
            String maxTimestamp = max.toString();
            if(max == Instant.MAX) {
                maxTimestamp = "infinity";
            }
            String sb = "tsrange((" + this.getStoreTableName() + "."+PHENOMENON_TIME+")::timestamp," +
                    " (" + this.getStoreTableName()  + "."+PHENOMENON_TIME+")::timestamp)" +
                    " && '[" + minTimestamp + "," + maxTimestamp + "]'";

        return "SELECT COUNT(*) FROM "+this.getStoreTableName()+" "+sb;
    }

    public String createSelectEntriesQuery(ObsFilter filter, Set<IObsStore.ObsField> fields) {
        SelectEntriesObsQuery selectEntriesObsQuery = new SelectEntriesObsQuery.Builder()
                .tableName(this.getStoreTableName())
                .linkTo(this.systemStore)
                .linkTo(this.dataStreamStore)
                .linkTo(this.foiStore)
                .withFields(fields)
                .withObsFilter(filter)
                .withLimit(filter.getLimit())
                .build();
        return selectEntriesObsQuery.toQuery();
    }

    public String createSelectEntriesCountQuery(ObsFilter filter) {
        SelectEntriesObsQuery selectEntriesObsQuery = new SelectEntriesObsQuery.Builder()
                .tableName(this.getStoreTableName())
                .linkTo(this.systemStore)
                .linkTo(this.dataStreamStore)
                .linkTo(this.foiStore)
                .withObsFilter(filter)
                .build();
        return selectEntriesObsQuery.toCountQuery();
    }

    public String statsQueryByFoi(ObsStatsQuery obsStatsQuery, long foiId) {
        StatsObsQuery statsObsQuery = new StatsObsQuery.Builder()
                .tableName(this.getStoreTableName())
                .linkTo(this.systemStore)
                .linkTo(this.dataStreamStore)
                .linkTo(this.foiStore)
                .withFoi(foiId)
                .withObsStatsFilter(obsStatsQuery)
                .withLimit(obsStatsQuery.getLimit())
                .build();
        return statsObsQuery.toQuery();
    }

    public String statsQueryByDataStream(ObsStatsQuery obsStatsQuery, long dsId) {
        StatsObsQuery statsObsQuery = new StatsObsQuery.Builder()
                .tableName(this.getStoreTableName())
                .linkTo(this.systemStore)
                .linkTo(this.dataStreamStore)
                .linkTo(this.foiStore)
                .withDataStream(dsId)
                .withObsStatsFilter(obsStatsQuery)
                .withLimit(obsStatsQuery.getLimit())
                .build();
        return statsObsQuery.toQuery();
    }

    public String createRemoveEntriesQuery(ObsFilter filter) {
        RemoveEntriesObsQuery removeEntriesObsQuery = new RemoveEntriesObsQuery.Builder()
                .tableName(this.getStoreTableName())
                .linkTo(this.systemStore)
                .linkTo(this.dataStreamStore)
                .linkTo(this.foiStore)
                .withObsFilter(filter)
                .build();
        return removeEntriesObsQuery.toQuery();
    }
}
