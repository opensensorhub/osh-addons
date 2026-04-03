package org.sensorhub.impl.datastore.postgis.builder.generator;

import java.util.ArrayList;
import java.util.List;

public abstract class FilterQueryGenerator {
    protected List<String> addConditions;
    protected List<String> orConditions;
    protected long limit = -1;

    protected  String tableName;
    protected List<String> selectFields;

    public void tableName(String tableName) {
        this.tableName = tableName;
    }

    public void setLimit(long limit){
        this.limit = limit;
    }

    protected void checkAddConditions() {
        if (this.addConditions == null) {
            this.addConditions = new ArrayList<>();
        }
    }

    protected void checkOrConditions() {
        if (this.orConditions == null) {
            this.orConditions = new ArrayList<>();
        }
    }

    protected void checkFields() {
        if (this.selectFields == null) {
            this.selectFields = new ArrayList<>();
        }
    }

    public void addCondition(String condition) {
        this.checkAddConditions();
        this.addConditions.add(condition);
    }

    public void orCondition(String condition) {
        this.checkOrConditions();
        this.orConditions.add(condition);
    }

    public void addSelectField(String field) {
        this.checkFields();
        this.selectFields.add(field);
    }

    public void setSelectedFields(List<String> fields) {
        this.selectFields = fields;
    }

    public abstract  String toQuery();
}
