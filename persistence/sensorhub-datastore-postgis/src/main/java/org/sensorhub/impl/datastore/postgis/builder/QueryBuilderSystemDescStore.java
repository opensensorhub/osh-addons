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

import org.sensorhub.api.datastore.system.ISystemDescStore;
import org.sensorhub.api.datastore.system.SystemFilter;
import org.sensorhub.api.system.ISystemWithDesc;
import org.sensorhub.impl.datastore.postgis.builder.filter.SelectEntriesFeatureQuery;
import org.sensorhub.impl.datastore.postgis.builder.filter.SelectEntriesSystemQuery;

import java.util.Set;

import static org.sensorhub.api.datastore.feature.IFeatureStoreBase.FeatureField.GEOMETRY;
import static org.sensorhub.api.datastore.feature.IFeatureStoreBase.FeatureField.VALID_TIME;

public class QueryBuilderSystemDescStore extends QueryBuilderBaseFeatureStore<ISystemWithDesc, ISystemDescStore.SystemField, SystemFilter> {
    public final static String FEATURE_TABLE_NAME = "system";

    public QueryBuilderSystemDescStore() {
        super(FEATURE_TABLE_NAME);
    }

    public QueryBuilderSystemDescStore(String tableName) {
        super(tableName);
    }


    @Override
        public String existsByUidQuery() {
            return "SELECT EXISTS(SELECT 1 from "+this.getStoreTableName()+" WHERE (data->>'uniqueId') = ?)";
        }

    @Override
        public String selectUidQuery() {
            return "SELECT count(*) as uidCount FROM "+this.getStoreTableName()+" where data->>'uniqueId' = ? LIMIT 1";
        }

    @Override
    public String selectLastVersionByUidQuery(String uid, String timestamp) {
        return "SELECT DISTINCT ON (id) id,validTime " +
                "FROM " + this.getStoreTableName() + " WHERE (data->>'uniqueId') = '" + uid + "' AND " +
                this.getStoreTableName()+".validTime @> ? " +
                "order by id, lower(validTime) DESC";
    }

    @Override
        public String createUidUniqueIndexQuery() {
            return "CREATE UNIQUE INDEX "+this.getStoreTableName()+"_feature_uid_idx ON "+this.getStoreTableName()+" " +
                    "((data->>'uniqueId'), "+VALID_TIME+")";
        }

    @Override
        public String createValidTimeBeginIndexQuery() {
            return "CREATE INDEX "+this.getStoreTableName()+"_feature_valid_time_0_idx ON "+this.getStoreTableName()+ " (validTime)";
        }

    @Override
        public String createValidTimeEndIndexQuery() {
            return "CREATE INDEX "+this.getStoreTableName()+"_feature_valid_time_1_idx ON "+this.getStoreTableName()+ " (validTime)";
        }
    @Override
        public String createTrigramDescriptionFullTextIndexQuery() {
            return "CREATE INDEX "+this.getStoreTableName()+"_feature_desc_full_text_datastream_idx ON  "+this.getStoreTableName()+" USING GIN ((data->>'description') gin_trgm_ops)";
        }
    @Override
        public String createTrigramUidFullTextIndexQuery() {
            return "CREATE INDEX "+this.getStoreTableName()+"_feature_uid_full_text_datastream_idx ON  "+this.getStoreTableName()+" USING GIN ((data->>'uniqueId') gin_trgm_ops)";
        }

    public String addOrUpdateByIdQuery() {
        return this.insertFeatureByIdQuery()+" ON CONFLICT ((data->>'uniqueId'), "+VALID_TIME +") DO "+
                "UPDATE SET "+GEOMETRY+" = ?, " +VALID_TIME+" = ?, data = ?  ";
    }

    @Override
    public String countFeatureQuery() {
        return "SELECT COUNT(DISTINCT data->>'uid') AS recordsCount FROM " + this.getStoreTableName();
    }

    @Override
    public String createSelectEntriesQuery(SystemFilter filter, Set<ISystemDescStore.SystemField> fields) {
        SelectEntriesSystemQuery selectEntriesSystemQuery = new SelectEntriesSystemQuery.Builder()
                .tableName(this.getStoreTableName())
                .withFields(fields)
                .withSystemFilter(filter)
                .build();
        return selectEntriesSystemQuery.toQuery();
    }
}
