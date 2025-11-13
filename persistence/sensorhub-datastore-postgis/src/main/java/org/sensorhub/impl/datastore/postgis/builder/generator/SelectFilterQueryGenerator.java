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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SelectFilterQueryGenerator extends FilterQueryGenerator {
    protected List<String> orderBy;
    protected List<String> groupBy;
    protected List<String> distinct;
    protected List<String> innerJoin;

    public String toQuery() {
        StringBuilder sb = new StringBuilder();
        if (selectFields != null) {
            // filters fields or *
            sb.append("SELECT ");
            if (distinct != null && !distinct.isEmpty()) {
                sb.append("DISTINCT on (").append(this.distinct.stream().collect(Collectors.joining(","))).append(")");
            }

            // to get count and use it with LIMIT and OFFSET
            if (this.selectFields != null && !this.selectFields.isEmpty()) {
                sb.append(selectFields.stream().map(fieldName -> this.tableName+"."+fieldName).collect(Collectors.joining(",")));
            } else {
                sb.append(" ").append(this.tableName).append(".* ");
            }

            sb.append(" FROM ").append(this.tableName);
        }
        if (this.joins != null && !this.joins.isEmpty()) {
            sb.append(" JOIN ");
            sb.append(this.joins.stream().collect(Collectors.joining(" JOIN ")));
        }
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
        if (this.groupBy != null && !this.groupBy.isEmpty()) {
            sb.append(" GROUP BY ");
            sb.append(this.groupBy.stream().collect(Collectors.joining(", ")));
        }

        if (this.orderBy != null && !this.orderBy.isEmpty()) {
            sb.append(" ORDER BY ");
            sb.append(this.orderBy.stream().collect(Collectors.joining(", ")));
        }
        if (this.limit >= 0) {
            sb.append(" LIMIT ").append(this.limit);
        }

        return sb.toString();
    }

    public String toCountQuery() {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT COUNT(*) ");
        sb.append(" FROM ").append(this.tableName);

        if (this.joins != null && !this.joins.isEmpty()) {
            sb.append(" JOIN ");
            sb.append(this.joins.stream().collect(Collectors.joining(" JOIN ")));
        }
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
        if (this.groupBy != null && !this.groupBy.isEmpty()) {
            sb.append(" GROUP BY ");
            sb.append(this.groupBy.stream().collect(Collectors.joining(", ")));
        }
        if (this.limit >= 0) {
            sb.append(" LIMIT ").append(this.limit);
        }

        return sb.toString();
    }


    private void checkGroupBy() {
        if (this.groupBy == null) {
            this.groupBy = new ArrayList<>();
        }
    }

    private void checkOrderBy() {
        if (this.orderBy == null) {
            this.orderBy = new ArrayList<>();
        }
    }

    private void checkDistinct() {
        if (this.distinct == null) {
            this.distinct = new ArrayList<>();
        }
    }

    public void addDistinct(String distinct) {
        this.checkDistinct();
        this.distinct.add(distinct);
    }

    public void addGroupBy(String groupBy) {
        this.checkGroupBy();
        this.groupBy.add(groupBy);
    }

    public void addOrderBy(String orderBy) {
        this.checkOrderBy();
        this.orderBy.add(orderBy);
    }

    public void addInnerJoin(String innerJoin) {
        this.checkInnerJoin();
        this.innerJoin.add(innerJoin);
    }

    protected void checkInnerJoin() {
        if (this.innerJoin == null) {
            this.innerJoin = new ArrayList<>();
        }
    }
}
