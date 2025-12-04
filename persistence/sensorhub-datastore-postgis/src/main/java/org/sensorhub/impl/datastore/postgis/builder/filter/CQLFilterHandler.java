package org.sensorhub.impl.datastore.postgis.builder.filter;

import org.geotools.api.filter.Filter;
import org.geotools.api.filter.PropertyIsEqualTo;
import org.geotools.api.filter.PropertyIsLike;
import org.geotools.api.filter.PropertyIsGreaterThan;
import org.geotools.api.filter.PropertyIsLessThan;
import org.geotools.api.filter.PropertyIsGreaterThanOrEqualTo;
import org.geotools.api.filter.PropertyIsLessThanOrEqualTo;
import org.geotools.api.filter.PropertyIsNotEqualTo;
import org.geotools.api.filter.And;
import org.geotools.api.filter.Or;
import org.geotools.api.filter.Not;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.api.filter.expression.Literal;

import java.util.List;

public class CQLFilterHandler {

    private StringBuilder whereClause;

    public CQLFilterHandler() {
        this.whereClause = new StringBuilder();
    }

    public String buildWhereClause(Filter filter) {
        whereClause.setLength(0);

        if (filter != null && filter != Filter.INCLUDE) {
            processFilter(filter);
        }

        return whereClause.toString();
    }


    private void processFilter(Filter filter) {
        if (filter instanceof And) {
            handleAnd((And) filter);
        } else if (filter instanceof Or) {
            handleOr((Or) filter);
        } else if (filter instanceof Not) {
            handleNot((Not) filter);
        } else if (filter instanceof PropertyIsEqualTo) {
            handleEquals((PropertyIsEqualTo) filter);
        } else if (filter instanceof PropertyIsNotEqualTo) {
            handleNotEquals((PropertyIsNotEqualTo) filter);
        } else if (filter instanceof PropertyIsGreaterThan) {
            handleGreaterThan((PropertyIsGreaterThan) filter);
        } else if (filter instanceof PropertyIsGreaterThanOrEqualTo) {
            handleGreaterThanOrEqual((PropertyIsGreaterThanOrEqualTo) filter);
        } else if (filter instanceof PropertyIsLessThan) {
            handleLessThan((PropertyIsLessThan) filter);
        } else if (filter instanceof PropertyIsLessThanOrEqualTo) {
            handleLessThanOrEqual((PropertyIsLessThanOrEqualTo) filter);
        } else if (filter instanceof PropertyIsLike) {
            handleLike((PropertyIsLike) filter);
        }
    }

    private void handleAnd(And filter) {
        whereClause.append("(");
        List<Filter> children = filter.getChildren();
        for (int i = 0; i < children.size(); i++) {
            if (i > 0) {
                whereClause.append(" AND ");
            }
            processFilter(children.get(i));
        }
        whereClause.append(")");
    }

    private void handleOr(Or filter) {
        whereClause.append("(");
        List<Filter> children = filter.getChildren();
        for (int i = 0; i < children.size(); i++) {
            if (i > 0) {
                whereClause.append(" OR ");
            }
            processFilter(children.get(i));
        }
        whereClause.append(")");
    }

    private void handleNot(Not filter) {
        whereClause.append("NOT (");
        processFilter(filter.getFilter());
        whereClause.append(")");
    }

    private void handleEquals(PropertyIsEqualTo filter) {
        String propertyName = getPropertyName(filter.getExpression1());
        Object value = getLiteralValue(filter.getExpression2());

        if (value instanceof Boolean) {
            whereClause.append("(result->>'").append(propertyName).append("')::boolean = ")
                    .append(value);
        } else if (value instanceof Number) {
            whereClause.append("(result->>'").append(propertyName).append("')::numeric = ")
                    .append(value);
        } else if (value == null) {
            whereClause.append("result->>'").append(propertyName).append("' IS NULL");
        } else {
            whereClause.append("result->>'").append(propertyName).append("' = ")
                    .append(escapeSqlString(value.toString()));
        }
    }

    private void handleNotEquals(PropertyIsNotEqualTo filter) {
        String propertyName = getPropertyName(filter.getExpression1());
        Object value = getLiteralValue(filter.getExpression2());

        if (value instanceof Boolean) {
            whereClause.append("(result->>'").append(propertyName).append("')::boolean != ")
                    .append(value);
        } else if (value instanceof Number) {
            whereClause.append("(result->>'").append(propertyName).append("')::numeric != ")
                    .append(value);
        } else if (value == null) {
            whereClause.append("result->>'").append(propertyName).append("' IS NOT NULL");
        } else {
            whereClause.append("result->>'").append(propertyName).append("' != ")
                    .append(escapeSqlString(value.toString()));
        }
    }

    private void handleGreaterThan(PropertyIsGreaterThan filter) {
        String propertyName = getPropertyName(filter.getExpression1());
        Object value = getLiteralValue(filter.getExpression2());

        whereClause.append("(result->>'").append(propertyName).append("')::numeric > ")
                .append(value);
    }

    private void handleGreaterThanOrEqual(PropertyIsGreaterThanOrEqualTo filter) {
        String propertyName = getPropertyName(filter.getExpression1());
        Object value = getLiteralValue(filter.getExpression2());

        whereClause.append("(result->>'").append(propertyName).append("')::numeric >= ")
                .append(value);
    }

    private void handleLessThan(PropertyIsLessThan filter) {
        String propertyName = getPropertyName(filter.getExpression1());
        Object value = getLiteralValue(filter.getExpression2());

        whereClause.append("(result->>'").append(propertyName).append("')::numeric < ")
                .append(value);
    }

    private void handleLessThanOrEqual(PropertyIsLessThanOrEqualTo filter) {
        String propertyName = getPropertyName(filter.getExpression1());
        Object value = getLiteralValue(filter.getExpression2());

        whereClause.append("(result->>'").append(propertyName).append("')::numeric <= ")
                .append(value);
    }

    private void handleLike(PropertyIsLike filter) {
        String propertyName = ((PropertyName) filter.getExpression()).getPropertyName();
        String pattern = filter.getLiteral();

        // Convert CQL LIKE pattern to SQL LIKE pattern
        // CQL uses * for wildcard, SQL uses %
        pattern = pattern.replace("\\", "\\\\")  // Escape backslashes first
                .replace("'", "''")      // Escape single quotes
                .replace("%", "\\%")     // Escape existing %
                .replace("_", "\\_")     // Escape existing _
                .replace("*", "%")       // CQL wildcard to SQL
                .replace("?", "_");      // CQL single char to SQL

        whereClause.append("result->>'").append(propertyName).append("' LIKE '")
                .append(pattern).append("'");
    }

    private String getPropertyName(org.geotools.api.filter.expression.Expression expr) {
        if (expr instanceof PropertyName) {
            return ((PropertyName) expr).getPropertyName();
        }
        throw new IllegalArgumentException("Expected PropertyName expression");
    }

    private Object getLiteralValue(org.geotools.api.filter.expression.Expression expr) {
        if (expr instanceof Literal) {
            return ((Literal) expr).getValue();
        }
        throw new IllegalArgumentException("Expected Literal expression");
    }

    private String escapeSqlString(String value) {
        if (value == null) {
            return "NULL";
        }
        String escaped = value.replace("'", "''");
        return "'" + escaped + "'";
    }
}