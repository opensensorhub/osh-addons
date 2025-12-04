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

        if (value == null) {
            // NULL checks can't use containment
            whereClause.append(buildJsonPathExtract(propertyName)).append(" IS NULL");
        } else if (value instanceof Boolean || value instanceof Number || value instanceof String) {
            // Use containment operator for GIN index optimization
            whereClause.append("result @> ").append(buildNestedJsonObject(propertyName, value));
        } else {
            // Fallback for other types
            whereClause.append(buildJsonPathExtract(propertyName)).append(" = ")
                    .append(escapeSqlString(value.toString()));
        }
    }

    private void handleNotEquals(PropertyIsNotEqualTo filter) {
        String propertyName = getPropertyName(filter.getExpression1());
        Object value = getLiteralValue(filter.getExpression2());

        if (value == null) {
            whereClause.append(buildJsonPathExtract(propertyName)).append(" IS NOT NULL");
        } else if (value instanceof Boolean || value instanceof Number || value instanceof String) {
            // NOT containment for inequality
            whereClause.append("NOT (result @> ").append(buildNestedJsonObject(propertyName, value)).append(")");
        } else {
            whereClause.append(buildJsonPathExtract(propertyName)).append(" != ")
                    .append(escapeSqlString(value.toString()));
        }
    }

    private void handleGreaterThan(PropertyIsGreaterThan filter) {
        String propertyName = getPropertyName(filter.getExpression1());
        Object value = getLiteralValue(filter.getExpression2());

        // Range queries can't use containment, use jsonb_path_query or extraction
        whereClause.append("(").append(buildJsonPathExtract(propertyName)).append(")::numeric > ")
                .append(value);
    }

    private void handleGreaterThanOrEqual(PropertyIsGreaterThanOrEqualTo filter) {
        String propertyName = getPropertyName(filter.getExpression1());
        Object value = getLiteralValue(filter.getExpression2());

        whereClause.append("(").append(buildJsonPathExtract(propertyName)).append(")::numeric >= ")
                .append(value);
    }

    private void handleLessThan(PropertyIsLessThan filter) {
        String propertyName = getPropertyName(filter.getExpression1());
        Object value = getLiteralValue(filter.getExpression2());

        whereClause.append("(").append(buildJsonPathExtract(propertyName)).append(")::numeric < ")
                .append(value);
    }

    private void handleLessThanOrEqual(PropertyIsLessThanOrEqualTo filter) {
        String propertyName = getPropertyName(filter.getExpression1());
        Object value = getLiteralValue(filter.getExpression2());

        whereClause.append("(").append(buildJsonPathExtract(propertyName)).append(")::numeric <= ")
                .append(value);
    }

    private void handleLike(PropertyIsLike filter) {
        String propertyName = ((PropertyName) filter.getExpression()).getPropertyName();
        String pattern = filter.getLiteral();

        // LIKE queries can't use containment, must use extraction
        pattern = pattern.replace("\\", "\\\\")
                .replace("'", "''")
                .replace("%", "\\%")
                .replace("_", "\\_")
                .replace("*", "%")
                .replace("?", "_");

        whereClause.append(buildJsonPathExtract(propertyName)).append(" LIKE '")
                .append(pattern).append("'");
    }

    /**
     * Builds a nested JSON object string for containment queries.
     * Handles dot-notation paths like "foo.bar.baz" -> {"foo": {"bar": {"baz": value}}}
     *
     * @param propertyPath Dot-separated property path
     * @param value        The value to match
     * @return SQL string for containment check
     */
    private String buildNestedJsonObject(String propertyPath, Object value) {
        String[] parts = propertyPath.split("\\.");
        StringBuilder json = new StringBuilder();

        // Build opening braces and keys
        for (int i = 0; i < parts.length; i++) {
            json.append("{\"").append(escapeJsonKey(parts[i])).append("\": ");
        }

        // Add the value
        json.append(formatJsonValue(value));

        // Close all braces
        for (int i = 0; i < parts.length; i++) {
            json.append("}");
        }

        return "'" + json.toString() + "'::jsonb";
    }

    /**
     * Builds a JSONB path extraction expression for nested properties.
     * Handles dot-notation paths like "foo.bar.baz" -> result->'foo'->'bar'->>'baz'
     *
     * @param propertyPath Dot-separated property path
     * @return SQL extraction expression
     */
    private String buildJsonPathExtract(String propertyPath) {
        String[] parts = propertyPath.split("\\.");

        if (parts.length == 1) {
            return "result->>'" + escapeJsonKey(parts[0]) + "'";
        }

        StringBuilder extraction = new StringBuilder("result");

        // Use -> for all but the last key
        for (int i = 0; i < parts.length - 1; i++) {
            extraction.append("->'").append(escapeJsonKey(parts[i])).append("'");
        }

        // Use ->> for the last key to get text
        extraction.append("->>'").append(escapeJsonKey(parts[parts.length - 1])).append("'");

        return extraction.toString();
    }

    /**
     * Formats a value for JSON representation.
     */
    private String formatJsonValue(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof Number) {
            return value.toString();
        } else {
            // String value - escape for JSON
            return "\"" + escapeJsonString(value.toString()) + "\"";
        }
    }

    /**
     * Escapes a string for use in JSON.
     */
    private String escapeJsonString(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Escapes a key for use in JSON/SQL.
     */
    private String escapeJsonKey(String key) {
        return key
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("'", "''");
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