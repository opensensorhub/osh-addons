/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis.builder.filter.procedure;

import org.sensorhub.api.datastore.feature.FeatureFilter;
import org.sensorhub.api.datastore.procedure.ProcedureFilter;
import org.sensorhub.api.datastore.system.SystemFilter;
import org.sensorhub.api.procedure.IProcedureWithDesc;
import org.sensorhub.impl.datastore.postgis.builder.filter.feature.BaseFeatureFilterQuery;
import org.sensorhub.impl.datastore.postgis.builder.generator.FilterQueryGenerator;
import org.sensorhub.impl.datastore.postgis.builder.generator.SelectFilterQueryGenerator;

import java.util.stream.Collectors;

public class SelectProcedureFilterQuery extends BaseFeatureFilterQuery<IProcedureWithDesc, ProcedureFilter, SelectFilterQueryGenerator> {

    public SelectProcedureFilterQuery(String tableName, SelectFilterQueryGenerator filterQueryGenerator) {
        super(tableName, filterQueryGenerator);
    }

    @Override
    public SelectFilterQueryGenerator build(ProcedureFilter filter) {
        this.filterQueryGenerator = super.build(filter);
        this.handleParentFilter(filter.getParentFilter());
        return this.filterQueryGenerator;
    }

    @Override
    protected void handleParentFilter(FeatureFilter parentFilter) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void handleParentFilter(SystemFilter parentFilter) {
        throw new UnsupportedOperationException();
    }

    protected void handleParentFilter(ProcedureFilter parentFilter) {
        if (parentFilter != null) {
            if (parentFilter.getInternalIDs() != null && !parentFilter.getInternalIDs().isEmpty()) {
                addCondition(tableName+".parentId in (" +
                        parentFilter.getInternalIDs().stream().map(bigId -> String.valueOf(bigId.getIdAsLong())).collect(Collectors.joining(",")) +
                        ")");
            }
            if(parentFilter.getUniqueIDs() != null) {
                this.filterQueryGenerator.addInnerJoin(this.tableName+ " t3 ON " + this.tableName + ".parentId" + " = t3.id");
                for(String uid: parentFilter.getUniqueIDs()) {
                    this.filterQueryGenerator.addCondition("t3.data->'properties'->>'uid' = '"+uid+"'");
                }
            }
        }
    }
}
