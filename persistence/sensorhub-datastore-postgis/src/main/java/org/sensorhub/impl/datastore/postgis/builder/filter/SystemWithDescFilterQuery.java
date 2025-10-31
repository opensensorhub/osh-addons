/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis.builder.filter;

import org.sensorhub.api.datastore.system.SystemFilter;
import org.sensorhub.api.system.ISystemWithDesc;

import java.time.Instant;
import java.util.SortedSet;
import java.util.stream.Collectors;

public class SystemWithDescFilterQuery extends BaseFeatureFilterQuery<ISystemWithDesc, SystemFilter> {

    protected SystemWithDescFilterQuery(String tableName, FilterQueryGenerator filterQueryGenerator) {
        super(tableName, filterQueryGenerator);
    }

    protected SystemWithDescFilterQuery(String tableName, FilterQueryGenerator filterQueryGenerator, FilterQueryGenerator.InnerJoin innerJoin) {
        super(tableName, filterQueryGenerator, innerJoin);
    }

    @Override
    public FilterQueryGenerator build(SystemFilter filter) {
        this.filterQueryGenerator = super.build(filter);
//        filter.getDataStreamFilter()
//        filter.getProcedureFilter()
        this.handleParentFilter(filter.getParentFilter());
        return this.filterQueryGenerator;
    }

    protected void handleParentFilter(SystemFilter parentFilter) {
        if (parentFilter != null) {
            if (parentFilter.getInternalIDs() != null && !parentFilter.getInternalIDs().isEmpty()) {
                addCondition(tableName+".parentId in (" +
                        parentFilter.getInternalIDs().stream().map(bigId -> String.valueOf(bigId.getIdAsLong())).collect(Collectors.joining(",")) +
                        ")");
            }
            if(parentFilter.getUniqueIDs() != null) {
                FilterQueryGenerator.InnerJoin innerJoin1 = new FilterQueryGenerator.InnerJoin(
                        this.tableName+ " t2 ON " + this.tableName + ".parentId" + " = t2.id"
                );
                filterQueryGenerator.addInnerJoin(innerJoin1);
                for(String uid: parentFilter.getUniqueIDs()) {
                    innerJoin1.addCondition("t2.data->'properties'->>'uid' = '"+uid+"'");
                }
            }
        }
    }

    protected void handleUniqueIds(SortedSet<String> uniqueIds) {
        if (uniqueIds != null) {
            StringBuilder sb = new StringBuilder();
            // Id can be regex
            // we have to use ILIKE behind trigram INDEX
            String currentId;
            int i = 0;
            sb.append("(");
            for(String uid: uniqueIds) {
                // ILIKE use % OPERATOR
                currentId = uid.replaceAll("\\*","%");
                sb.append("(").append(tableName).append(".data->>'uniqueId') ILIKE '%").append(currentId).append("'");
                if(++i < uniqueIds.size()) {
                    sb.append(" OR ");
                }
            }
            sb.append(")");
            addCondition(sb.toString());
        }
    }
}
