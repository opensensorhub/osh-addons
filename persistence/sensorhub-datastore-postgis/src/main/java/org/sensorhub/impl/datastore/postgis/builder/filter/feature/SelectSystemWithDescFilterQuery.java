/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis.builder.filter.feature;

import org.sensorhub.api.datastore.TemporalFilter;
import org.sensorhub.api.datastore.feature.FeatureFilter;
import org.sensorhub.api.datastore.system.SystemFilter;
import org.sensorhub.impl.datastore.postgis.builder.generator.SelectFilterQueryGenerator;
import org.sensorhub.impl.datastore.postgis.utils.PostgisUtils;

import java.util.stream.Collectors;

public class SelectSystemWithDescFilterQuery extends BaseSystemWithDescFilterQuery<SelectFilterQueryGenerator> {

    public SelectSystemWithDescFilterQuery(String tableName, SelectFilterQueryGenerator filterQueryGenerator) {
        super(tableName, filterQueryGenerator);
    }

    @Override
    protected void handleValidTimeFilter(TemporalFilter temporalFilter) {
        if (temporalFilter == null)
            return;

        if (temporalFilter.isLatestTime()) {
            filterQueryGenerator.addDistinct("(" + this.tableName + ".id)");
            filterQueryGenerator.addDistinct("(" + this.tableName + ".data->>'uniqueId')");

            filterQueryGenerator.addOrderBy("(" + this.tableName + ".id)");
            filterQueryGenerator.addOrderBy("(" + this.tableName + ".data->>'uniqueId')");
            filterQueryGenerator.addOrderBy(this.tableName + ".validTime DESC");
        } else if (temporalFilter.isCurrentTime()) {
            String sb =
                    "(" + this.tableName + ".validTime IS NULL " +
                            " OR (" +
                            " lower(" + this.tableName + ".validTime) <= now() AND " +
                            " (upper(" + this.tableName + ".validTime) IS NULL " +
                            "      OR upper(" + this.tableName + ".validTime) >= now()" +
                            " )" +
                            ")" +
                            ")";
            addCondition(sb);
        } else {
            String min = PostgisUtils.checkAndGetValidInstant(temporalFilter.getMin());
            String max = PostgisUtils.checkAndGetValidInstant(temporalFilter.getMax());

            String sb =
                    "(" + this.tableName + ".validTime IS NULL " +
                            " OR " + this.tableName + ".validTime " +
                            PostgisUtils.getOperator(temporalFilter) + " " +
                            "'[" + min + "," + max + "]'::tsrange" +
                            ")";
            addCondition(sb);
        }
    }

    @Override
    protected void handleParentFilter(FeatureFilter parentFilter) {
        throw new UnsupportedOperationException();
    }

    protected void handleParentFilter(SystemFilter parentFilter) {
        if (parentFilter != null) {
            if (parentFilter.getInternalIDs() != null && !parentFilter.getInternalIDs().isEmpty()) {
                addCondition(tableName+".parentId in (" +
                        parentFilter.getInternalIDs().stream().map(bigId -> String.valueOf(bigId.getIdAsLong())).collect(Collectors.joining(",")) +
                        ")");
            }
            if(parentFilter.getUniqueIDs() != null) {
                this.filterQueryGenerator.addInnerJoin( this.tableName+ " t4 ON " + this.tableName + ".parentId" + " = t4.id");
                for(String uid: parentFilter.getUniqueIDs()) {
                    this.filterQueryGenerator.addCondition("t4.data->'properties'->>'uid' = '"+uid+"'");
                }
            }
        }
    }
}
