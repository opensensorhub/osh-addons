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

import org.sensorhub.api.datastore.feature.FeatureFilter;
import org.sensorhub.api.datastore.system.SystemFilter;
import org.sensorhub.api.system.ISystemWithDesc;
import org.sensorhub.impl.datastore.postgis.builder.generator.FilterQueryGenerator;

import java.util.SortedSet;

public abstract class BaseSystemWithDescFilterQuery<F extends FilterQueryGenerator> extends BaseFeatureFilterQuery<ISystemWithDesc, SystemFilter, F> {

    public BaseSystemWithDescFilterQuery(String tableName, F filterQueryGenerator) {
        super(tableName, filterQueryGenerator);
    }


    @Override
    public F build(SystemFilter filter) {
        this.filterQueryGenerator = super.build(filter);
//        filter.getDataStreamFilter()
//        filter.getProcedureFilter()
        this.handleParentFilter(filter.getParentFilter());
        if (filter.includeMembers()) {
            this.handleIncludeMembers();
        }
        return this.filterQueryGenerator;
    }

    protected void handleIncludeMembers() {
        // TODO: Implement
    }

    @Override
    protected void handleParentFilter(FeatureFilter parentFilter) {
        throw new UnsupportedOperationException();
    }

    protected abstract void handleParentFilter(SystemFilter parentFilter);

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
