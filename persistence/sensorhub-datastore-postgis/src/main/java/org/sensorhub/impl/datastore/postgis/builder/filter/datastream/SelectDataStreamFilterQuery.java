/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis.builder.filter.datastream;

import org.sensorhub.api.datastore.TemporalFilter;
import org.sensorhub.impl.datastore.postgis.builder.generator.SelectFilterQueryGenerator;
import org.sensorhub.impl.datastore.postgis.utils.PostgisUtils;

public class SelectDataStreamFilterQuery extends DataStreamFilterQuery<SelectFilterQueryGenerator> {
    private Long dsId;

    public SelectDataStreamFilterQuery(String tableName, SelectFilterQueryGenerator filterQueryGenerator) {
        super(tableName, filterQueryGenerator);
    }

    protected void handleValidTimeFilter(TemporalFilter temporalFilter) {
        if (temporalFilter != null) {
            if (temporalFilter.isLatestTime()) {
                filterQueryGenerator.addDistinct("(" + tableName + ".data->>'name')");
                filterQueryGenerator.addDistinct("(" + tableName + ".data->'system@id'->'internalID'->'id')::bigint");
                filterQueryGenerator.addOrderBy(tableName + ".data->>'name'");
                filterQueryGenerator.addOrderBy("(" + tableName + ".data->'system@id'->'internalID'->'id')::bigint");
                filterQueryGenerator.addOrderBy("(" + tableName + ".data->'validTime'->>'end')::timestamp DESC ");
            }
            else if (temporalFilter.isCurrentTime()) {
                filterQueryGenerator.addDistinct("(" + tableName + ".data->>'name')");
                filterQueryGenerator.addDistinct("(" + tableName + ".data->'system@id'->'internalID'->'id')::bigint");
                String sb = "(" +
                        tableName + ".data->'validTime' IS NULL " +
                        "OR (" +
                        tableName + ".data->'validTime'->'begin' IS NOT NULL " +
                        "AND (" + tableName + ".data->'validTime'->>'begin')::timestamp <= now() " +
                        "AND ((" + tableName + ".data->'validTime'->>'end') IS NULL " +
                        "OR (" + tableName + ".data->'validTime'->>'end')::timestamp >= now())" +
                        ")" +
                        ")";
                addCondition(sb);
            }
            else {
                String min = PostgisUtils.checkAndGetValidInstant(temporalFilter.getMin());
                String max = PostgisUtils.checkAndGetValidInstant(temporalFilter.getMax());

                String sb = "(" +
                        tableName + ".data->'validTime' IS NULL " +
                        "OR tsrange((" +
                        tableName + ".data->'validTime'->>'begin')::timestamp, (" +
                        tableName + ".data->'validTime'->>'end')::timestamp) " +
                        PostgisUtils.getOperator(temporalFilter) + " " +
                        "'[" + min + "," + max + "]'::tsrange" +
                        ")";
                addCondition(sb);
            }
        }
    }
}
