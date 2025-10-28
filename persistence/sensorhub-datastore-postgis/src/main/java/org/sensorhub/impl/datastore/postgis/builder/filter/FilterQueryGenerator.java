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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class FilterQueryGenerator {
    public static class InnerJoin {
        private String innerJoinMapping;
        private List<String> innerJoinConditions;

        public InnerJoin(String innerJoinMapping) {
            this.innerJoinMapping = innerJoinMapping;
        }

        public void addCondition(String condition) {
            if(innerJoinConditions == null) {
                innerJoinConditions = new ArrayList<>();
            }
            innerJoinConditions.add(condition);
        }

        boolean hasConditions() {
            return innerJoinConditions != null;
        }

        public String getInnerJoinMapping() {
            return innerJoinMapping;
        }

        public List<String> getInnerJoinConditions() {
            return innerJoinConditions;
        }
    }

    protected List<String> addConditions;
    protected List<String> orConditions;
    protected List<String> orderBy;
    protected List<String> groupBy;
    protected List<String> distinct;
    protected long limit = -1;

    protected  String tableName;
    protected List<String> selectFields;
    protected List<InnerJoin> innerJoin;

    public void tableName(String tableName) {
        this.tableName = tableName;
    }

    public void setLimit(long limit){
        this.limit = limit;
    }

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
                sb.append(selectFields.stream().collect(Collectors.joining(",")));
            } else {
                sb.append(" ").append(this.tableName).append(".* ");
            }

            sb.append(" FROM ").append(this.tableName);
        }

        if (this.innerJoin != null && !this.innerJoin.isEmpty()) {
            // add Inner join and its corresponding conditions
            innerJoin.forEach( innerJoinObj -> {
                sb.append(" INNER JOIN ");
                sb.append(
                        innerJoinObj.getInnerJoinMapping())
                        .append(innerJoinObj.hasConditions() ? innerJoinObj.getInnerJoinConditions().stream().map(value -> " AND "+value).collect(Collectors.joining(" ")): " ");
            });
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

        if (this.innerJoin != null && !this.innerJoin.isEmpty()) {
            // add Inner join and its corresponding conditions
            innerJoin.forEach( innerJoinObj -> {
                sb.append(" INNER JOIN ");
                sb.append(
                        innerJoinObj.getInnerJoinMapping())
                        .append(" ")
                        .append(innerJoinObj.hasConditions() ? innerJoinObj.getInnerJoinConditions().stream().map(value -> " AND "+value).collect(Collectors.joining(" ")): " ");
            });
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

    private void checkAddConditions() {
        if (this.addConditions == null) {
            this.addConditions = new ArrayList<>();
        }
    }

    private void checkOrConditions() {
        if (this.orConditions == null) {
            this.orConditions = new ArrayList<>();
        }
    }

    private void checkFields() {
        if (this.selectFields == null) {
            this.selectFields = new ArrayList<>();
        }
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

    private void checkInnerJoin() {
        if (this.innerJoin == null) {
            this.innerJoin = new ArrayList<>();
        }
    }

    protected void addCondition(String condition) {
        this.checkAddConditions();
        this.addConditions.add(condition);
    }

    protected void orCondition(String condition) {
        this.checkOrConditions();
        this.orConditions.add(condition);
    }

    protected void addInnerJoin(InnerJoin innerJoin) {
        this.checkInnerJoin();
        this.innerJoin.add(innerJoin);
    }

    protected void addDistinct(String distinct) {
        this.checkDistinct();
        this.distinct.add(distinct);
    }

    protected void addGroupBy(String groupBy) {
        this.checkGroupBy();
        this.groupBy.add(groupBy);
    }

    protected void addOrderBy(String orderBy) {
        this.checkOrderBy();
        this.orderBy.add(orderBy);
    }

    protected void addSelectField(String field) {
        this.checkFields();
        this.selectFields.add(field);
    }

    protected void setSelectedFields(List<String> fields) {
        this.selectFields = fields;
    }

}
