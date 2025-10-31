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

import org.sensorhub.api.datastore.procedure.ProcedureFilter;
import org.sensorhub.api.datastore.system.SystemFilter;
import org.sensorhub.api.procedure.IProcedureWithDesc;

import java.util.stream.Collectors;

public class ProcedureFilterQuery extends BaseFeatureFilterQuery<IProcedureWithDesc, ProcedureFilter> {

    protected ProcedureFilterQuery(String tableName, FilterQueryGenerator filterQueryGenerator) {
        super(tableName, filterQueryGenerator);
    }

    protected ProcedureFilterQuery(String tableName, FilterQueryGenerator filterQueryGenerator, FilterQueryGenerator.InnerJoin innerJoin) {
        super(tableName, filterQueryGenerator, innerJoin);
    }

    @Override
    public FilterQueryGenerator build(ProcedureFilter filter) {
        this.filterQueryGenerator = super.build(filter);
        this.handleParentFilter(filter.getParentFilter());
        return this.filterQueryGenerator;
    }

    protected void handleParentFilter(ProcedureFilter parentFilter) {
        if (parentFilter != null) {
            if (parentFilter.getInternalIDs() != null && !parentFilter.getInternalIDs().isEmpty()) {
                addCondition(tableName+".parentId in (" +
                        parentFilter.getInternalIDs().stream().map(bigId -> String.valueOf(bigId.getIdAsLong())).collect(Collectors.joining(",")) +
                        ")");
            }
            if(parentFilter.getUniqueIDs() != null) {
                FilterQueryGenerator.InnerJoin innerJoin1 =
                        new FilterQueryGenerator.InnerJoin(this.tableName+ " t2 ON " + this.tableName + ".parentId" + " = t2.id");
                filterQueryGenerator.addInnerJoin(innerJoin1);
                for(String uid: parentFilter.getUniqueIDs()) {
                    innerJoin1.addCondition("t2.data->'properties'->>'uid' = '"+uid+"'");
                }
            }
        }
    }
}
