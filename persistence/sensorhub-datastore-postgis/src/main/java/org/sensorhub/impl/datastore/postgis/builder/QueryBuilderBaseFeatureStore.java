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

import org.sensorhub.api.datastore.feature.FeatureFilterBase;
import org.sensorhub.api.datastore.feature.IFeatureStoreBase;
import org.vast.ogc.gml.IFeature;

import java.util.Set;

import static org.sensorhub.api.datastore.feature.IFeatureStoreBase.FeatureField.GEOMETRY;
import static org.sensorhub.api.datastore.feature.IFeatureStoreBase.FeatureField.VALID_TIME;

public abstract class QueryBuilderBaseFeatureStore<V extends IFeature,VF extends IFeatureStoreBase.FeatureField,
        F extends FeatureFilterBase<? super V>> extends QueryBuilder {

    public final static String EXTENT_COLUMN_NAME = "bextent";

    public QueryBuilderBaseFeatureStore(String tableName) {
        super(tableName);
    }

    public String createTableQuery() {
        // NOTE: do not use JSONB because we need to keep order to the properties for geoGSON
        // see: https://www.postgresql.org/docs/current/datatype-json.html
        return "CREATE TABLE " + this.getStoreTableName() +
                " (id BIGSERIAL," +
                " parentId bigint,"+
                GEOMETRY +" GEOMETRY, "+
                VALID_TIME +" tsrange,"+
                "data JSONB)";
    }

    public String insertFeatureQuery() {
        return "INSERT INTO "+this.getStoreTableName()+" (parentId,"+GEOMETRY+", "+VALID_TIME+", data) VALUES (?,?,?,?)";
    }

    public String insertFeatureByIdQuery() {
//        return "INSERT INTO "+this.getStoreTableName()+" (id, parentId,"+GEOMETRY+", "+VALID_TIME+", data) " +
//                "SELECT ?,?,?,?,? WHERE (EXISTS(SELECT 1 from "+this.getStoreTableName()+" where id = ?)) ";
        return "INSERT INTO "+this.getStoreTableName()+" (id, parentId,"+GEOMETRY+", "+VALID_TIME+", data) " +
                "SELECT ?,?,?,?,? WHERE NOT EXISTS (SELECT 1 FROM "+this.getStoreTableName()+" WHERE (data->>'uniqueId')::text = ?)";
    }

    public String selectByPrimaryKeyQuery() {
        return "SELECT data FROM " + this.getStoreTableName() + " WHERE id = ? AND lower(" + this.getStoreTableName() + ".validTime) = ?::timestamp";
    }

    public String existsByIdQuery() {
        return "SELECT EXISTS(SELECT 1 from "+this.getStoreTableName()+" WHERE id = ?)";
    }

    public String existsByUidQuery() {
        return "SELECT EXISTS(SELECT 1 from "+this.getStoreTableName()+" WHERE (data->'properties'->>'uid') = ?)";
    }
    public String updateByIdQuery() {
        return "UPDATE "+this.getStoreTableName()+" SET "+GEOMETRY+" = ?, " +VALID_TIME+" = ?, " +
                "data = ?  " +
                "WHERE id = ?";
    }

    public String addOrUpdateByIdQuery() {
        return this.insertFeatureByIdQuery()+" ON CONFLICT ((data->'properties'->>'uid'), "+VALID_TIME +") DO "+
                "UPDATE SET "+GEOMETRY+" = ?, " +VALID_TIME+" = ?, data = ?  ";
    }

    public String selectExtentQuery() {
        return "SELECT ST_Extent("+GEOMETRY+") as "+EXTENT_COLUMN_NAME+" FROM "+this.getStoreTableName();
    }

    public String selectUidQuery() {
        return "SELECT count(*) as uidCount FROM "+this.getStoreTableName()+" where data->'properties'->>'uid' = ? LIMIT 1";
    }

    public String selectLastVersionByUidQuery(String uid, String timestamp) {
        return "SELECT DISTINCT ON (id) id,validTime " +
                "FROM " + this.getStoreTableName() + " WHERE (data->'properties'->>'uid') = '" + uid + "' AND " +
                this.getStoreTableName()+".validTime @> '" + timestamp + "'::timestamp "+
                "order by id, lower(validTime) DESC";
    }

    public String selectLastVersionByIdQuery(long id, String timestamp) {
        return "SELECT DISTINCT ON (id) id,validTime "+
                "FROM "+this.getStoreTableName()+" WHERE id = "+id+" AND " +
                this.getStoreTableName()+".validTime @> '" + timestamp + "'::timestamp "+
                "order by id, lower(validTime) DESC";
    }

    public String countFeatureQuery() {
        return "SELECT COUNT(DISTINCT data->'properties'->>'uid') AS recordsCount FROM " + this.getStoreTableName();
    }

    public String removeByPrimaryKeyQuery() {
        return "DELETE FROM "+this.getStoreTableName()+" WHERE id = ? AND "+this.getStoreTableName()+".validTime @> ?";
    }

    public String createUidUniqueIndexQuery() {
        return "CREATE UNIQUE INDEX "+this.getStoreTableName()+"_feature_uid_idx ON "+this.getStoreTableName()+" " +
                "((data->'properties'->>'uid'), "+VALID_TIME+")";
    }
    public String createValidTimeIndexQuery() {
        return "CREATE INDEX "+this.getStoreTableName()+"_feature_valid_time_0_idx ON "+this.getStoreTableName()+ " using GIST (validTime)";
    }

    public String createTrigramExtensionQuery() {
        return "CREATE EXTENSION IF NOT EXISTS pg_trgm";
    }

    public String createTrigramDescriptionFullTextIndexQuery() {
        return "CREATE INDEX "+this.getStoreTableName()+"_feature_desc_full_text_datastream_idx ON  "+this.getStoreTableName()+" USING GIN ((data->'properties'->>'description') gin_trgm_ops)";
    }

    public String createTrigramUidFullTextIndexQuery() {
        return "CREATE INDEX "+this.getStoreTableName()+"_feature_uid_full_text_datastream_idx ON  "+this.getStoreTableName()+" USING GIN ((data->'properties'->>'uid') gin_trgm_ops)";
    }

    public abstract String createSelectEntriesQuery(F filter, Set<VF> fields);
}
