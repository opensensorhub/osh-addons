/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis.builder;

import org.sensorhub.api.common.BigId;
import org.sensorhub.api.datastore.TemporalFilter;
import org.sensorhub.api.datastore.feature.FeatureFilterBase;
import org.sensorhub.api.datastore.feature.FeatureKey;
import org.sensorhub.impl.datastore.postgis.connection.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.ogc.gml.IFeature;
import org.vast.util.TimeExtent;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.function.Function;

public class FoiIteratorResultSet<T> implements Iterator<T> {
    private final Logger logger = LoggerFactory.getLogger(FoiIteratorResultSet.class);

    private final ConnectionManager connectionManager;
    private final Function<ResultSet, T> parsingFn;
    private final Function<T, Boolean> predicateValidator;
    private final boolean useInternalLimit;
    private final boolean hasValuePredicate;

    private final long internalLimit;
    private final long filterLimit;
    private final boolean latestTime;

    private final String baseQuery;
    private final String tableName;
    private Instant lastPhenomenonTime = null;
    private BigId lastId = null; // optional tie-breaker
    private boolean ended = false;

    private long offset = 0; // for OFFSET-based pagination (latestTime with value predicate)
    private long deliveredCount = 0;
    private Queue<T> records = new ArrayDeque<>();

    public FoiIteratorResultSet(String query,
                                String tableName,
                                ConnectionManager connectionManager,
                                long internalLimit,
                                long filterLimit,
                                Function<ResultSet, T> parsingFn,
                                Function<T, Boolean> predicateValidator,
                                FeatureFilterBase<org.vast.ogc.gml.IFeature> filter
    ) {
        this.hasValuePredicate = filter.getValuePredicate() != null;
        boolean latestTime = (filter.getValidTime() != null && filter.getValidTime().isLatestTime());

        this.tableName = tableName;
        // For latestTime with value predicate, remove LIMIT so we can paginate with OFFSET
        this.baseQuery = (latestTime && hasValuePredicate) ? removeSqlLimit(query) :
                (!hasValuePredicate ? query : removeSqlLimit(query));
        this.connectionManager = connectionManager;
        this.internalLimit = internalLimit;
        this.filterLimit = filterLimit;
        this.parsingFn = parsingFn;
        this.predicateValidator = predicateValidator;
        this.latestTime = latestTime;

        this.useInternalLimit = !query.toUpperCase().contains("LIMIT") || hasValuePredicate;
    }

    private String removeSqlLimit(String sql) {
        return sql.replaceAll("(?i)\\s+LIMIT\\s+\\d+(\\s+OFFSET\\s+\\d+)?", "");
    }

    private String removeOrderByAndLimit(String sql) {
        String result = sql.replaceAll("(?i)\\s+LIMIT\\s+\\d+(\\s+OFFSET\\s+\\d+)?", "");
        result = result.replaceAll("(?i)\\s+ORDER\\s+BY\\s+[^;]*$", "");
        return result;
    }

    @Override
    public boolean hasNext() {
        if (deliveredCount >= filterLimit) {
            return false;
        }
        if (!records.isEmpty()) {
            return true;
        }
        if (ended) {
            return false;
        }
        while (records.isEmpty() && !ended && deliveredCount < filterLimit) {
            makeRequest();
        }
        return !records.isEmpty();
    }

    @Override
    public T next() {
        T value = records.poll();
        if (value != null) {
            deliveredCount++;
        }
        return value;
    }

    private void makeRequest() {
        long fetchedFromDb = 0;
        T lastFetchedRecord = null;

        try (Connection connection = connectionManager.getConnection();
             Statement statement = connection.createStatement()) {

            String nextQuery = buildQuery();
            if (logger.isDebugEnabled()) {
                logger.debug(nextQuery);
            }

            try (ResultSet rs = statement.executeQuery(nextQuery)) {
                while (rs.next()) {
                    fetchedFromDb++;
                    T row = parsingFn.apply(rs);

                    // Track the last fetched record for cursor update
                    lastFetchedRecord = row;

                    // Check if we've already hit the filter limit before applying predicate
                    if (deliveredCount + records.size() >= filterLimit) {
                        ended = true;
                        break;
                    }

                    // Apply value predicate if present
                    if (predicateValidator.apply(row)) {
                        records.add(row);

                        // Check again after adding to queue
                        if (deliveredCount + records.size() >= filterLimit) {
                            ended = true;
                            break;
                        }
                    }
                }
            }

            // Update pagination state based on query type
            if (latestTime && hasValuePredicate) {
                // OFFSET-based pagination for latestTime with value predicate
                offset += internalLimit;

                // End if no rows or fewer than expected
                if (fetchedFromDb == 0 || fetchedFromDb < internalLimit) {
                    ended = true;
                }
            } else if (!latestTime && lastFetchedRecord != null) {
                // Cursor-based pagination for normal queries
                updateCursor(lastFetchedRecord);

                // End if no rows or fewer than expected
                if (fetchedFromDb == 0 || (useInternalLimit && fetchedFromDb < internalLimit)) {
                    ended = true;
                }
            } else if (latestTime && !hasValuePredicate) {
                // latestTime without value predicate: single query, always end
                ended = true;
            } else if (fetchedFromDb == 0) {
                // No rows at all
                ended = true;
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String buildQuery() {
        String query = baseQuery;

        if (latestTime) {
            if (hasValuePredicate) {
                // latestTime WITH value predicate: use OFFSET pagination
                // The baseQuery already has LIMIT removed and has ORDER BY
                StringBuilder sb = new StringBuilder(baseQuery);
                sb.append(" LIMIT ").append(internalLimit);
                sb.append(" OFFSET ").append(offset);
                return sb.toString();
            } else {
                // latestTime WITHOUT value predicate: single query, no pagination
                return baseQuery;
            }
        } else if (lastPhenomenonTime != null) {
            // Subsequent pages: use cursor
            String cleanQuery = removeOrderByAndLimit(baseQuery);
            StringBuilder sb = new StringBuilder(cleanQuery);
            String firstOperator = !cleanQuery.contains("WHERE") ? " WHERE " : " AND";
            sb.append(firstOperator).append(" (").append("lower(").append(tableName).append(".validTime) > '")
                    .append(lastPhenomenonTime).append("'");

            // tie-breaker if needed
            if (lastId != null) {
                sb.append(" OR (").append("lower(").append(tableName).append(".validTime) = '")
                        .append(lastPhenomenonTime).append("' AND ").append(tableName).append(".id > ")
                        .append(lastId.getIdAsLong()).append(")");
            }

            sb.append(")");

            sb.append(" ORDER BY ").append("lower(").append(tableName).append(".validTime) DESC, ")
                    .append(tableName).append(".id DESC ");

            if (useInternalLimit) {
                sb.append(" LIMIT ").append(internalLimit);
            }

            return sb.toString();
        } else {
            String cleanQuery = removeOrderByAndLimit(baseQuery);
            StringBuilder sb = new StringBuilder(cleanQuery);
            sb.append(" ORDER BY ").append("lower(").append(tableName).append(".validTime) DESC, ")
                    .append(tableName).append(".id DESC ");

            if (useInternalLimit) {
                sb.append(" LIMIT ").append(internalLimit);
            } else {
                sb.append(" LIMIT ").append(filterLimit);
            }

            return sb.toString();
        }
    }

    private void updateCursor(T lastRecord) {
        if (lastRecord instanceof Map.Entry<?, ?>) {
            Map.Entry<FeatureKey, org.vast.ogc.gml.IFeature> entry = (Map.Entry<FeatureKey, org.vast.ogc.gml.IFeature>) lastRecord;
            FeatureKey fk = entry.getKey();
            lastPhenomenonTime = fk.getValidStartTime();
            lastId = entry.getKey().getInternalID();
        }
    }
}