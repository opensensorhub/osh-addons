/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis.builder.generator;

import java.util.stream.Collectors;

public class SelectFilterStatsQueryGenerator extends SelectFilterQueryGenerator {
    
    public String toQuery() {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT COUNT(*) ");
        sb.append(" FROM ").append(this.tableName);

        if (this.innerJoin != null && !this.innerJoin.isEmpty()) {
            // add Inner join and its corresponding conditions
            sb.append(" INNER JOIN ");
            sb.append(this.innerJoin.stream().collect(Collectors.joining(" INNER JOIN ")));
        }

        // check WHERE clause
        if (this.addConditions != null && !this.addConditions.isEmpty()) {
            sb.append(" WHERE ");
            sb.append(this.addConditions.stream().collect(Collectors.joining(" AND ")));
        }
        if (this.orConditions != null && !this.orConditions.isEmpty()) {
            if (addConditions == null || addConditions.isEmpty()) {
                sb.append(" WHERE ");
            }
            sb.append(this.orConditions.stream().collect(Collectors.joining(" OR ")));
        }
        if (this.orderBy != null && !this.orderBy.isEmpty()) {
            sb.append(" ORDER BY ");
            sb.append(this.orderBy.stream().collect(Collectors.joining(", ")));
        }
        if (this.limit >= 0 && this.limit != Long.MAX_VALUE) {
            sb.append(" LIMIT ").append(this.limit);
        }

        return sb.toString();
    }

}
