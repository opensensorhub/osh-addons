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

import org.sensorhub.impl.datastore.postgis.connection.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

public class IteratorResultSet<T> implements Iterator<T> {
    private static final Logger logger = LoggerFactory.getLogger(IteratorResultSet.class);

    private long internalLimit;

    private long filterLimit;

    private long offset = 0;

    private long deliveredCount = 0;

    private String query;

    private ConnectionManager connectionManager;

    private final Function<ResultSet, T> parsingFn;

    private ConcurrentLinkedQueue<T> records = new ConcurrentLinkedQueue<>();

    private boolean ended = false;

    private final Function<T, Boolean> predicateValidator;
    private final boolean useInternalLimit;

    public IteratorResultSet(String query,
                             ConnectionManager connectionManager,
                             long internalLimit,
                             long filterLimit,
                             Function<ResultSet, T> parsingFn,
                             Function<T, Boolean> predicateValidator,
                             boolean hasValuePredicate
    ) {
        this.query = !hasValuePredicate ? query : removeSqlLimit(query);
        this.internalLimit = internalLimit;
        this.filterLimit = filterLimit;
        this.parsingFn = parsingFn;
        this.connectionManager = connectionManager;
        this.predicateValidator = predicateValidator;
        this.useInternalLimit = !query.contains("LIMIT") || hasValuePredicate;
    }

    @Override
    public boolean hasNext() {
        if (deliveredCount >= filterLimit) {
            return false;
        }
        if(!records.isEmpty()) {
            return true;
        }
        if (ended) {
            return false;
        }
        while(records.isEmpty() && !ended && deliveredCount < filterLimit   ) {
            this.makeRequest();
        }
        return !records.isEmpty();
    }

    private String getQuery() {
        if(useInternalLimit) {
            return query + " LIMIT " + internalLimit + " OFFSET " + offset;
        } else {
            // limit set by the filter itself
            return query + " OFFSET " + offset;
        }
    }

    private String removeSqlLimit(String sql) {
        return sql.replaceAll("(?i)\\s+LIMIT\\s+\\d+(\\s+OFFSET\\s+\\d+)?", "");
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

        try (Connection connection = connectionManager.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                String nextQuery = getQuery();
                if (logger.isDebugEnabled()) {
                    logger.debug(nextQuery);
                }
                try (ResultSet resultSet = statement.executeQuery(nextQuery)) {
                    while (resultSet.next()) {
                        fetchedFromDb++;

                        if (deliveredCount + records.size() >= filterLimit) {
                            ended = true;
                            break;
                        }

                        T res = parsingFn.apply(resultSet);

                        if (predicateValidator.apply(res)) {
                            records.add(res);
                            if (deliveredCount + records.size() >= filterLimit) {
                                ended = true;
                                break;
                            }
                        }
                    }
                }
            }

            // Move offset only when internal limit is active
            if (useInternalLimit) {
                offset += internalLimit;
            } else {
                // If SQL LIMIT is controlling the batch size, we must rely on SQL side
                offset += filterLimit > 0 ? filterLimit : internalLimit;
            }

            // If DB returned fewer rows than internalLimit, this is the final page
            if (fetchedFromDb == 0 ||
                    (useInternalLimit && fetchedFromDb < internalLimit)) {
                ended = true;
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


//    private void makeRequest() {
//        long countRes = 0;
//        try (Connection connection = connectionManager.getConnection()) {
//            try(Statement statement = connection.createStatement()) {
//                String nextQuery = getQuery();
//                if(logger.isDebugEnabled()) {
//                    logger.debug(nextQuery);
//                }
//                try (ResultSet resultSet = statement.executeQuery(nextQuery)){
//                    while (resultSet.next()) {
//                        countRes++;
//                        T res = this.parsingFn.apply(resultSet);
//                        if(predicateValidator.apply(res)) {
//                            records.add(res);
//                        }
//                    }
//                    offset += limit;
//                }
//            }
//            if(countRes == 0 || countRes < limit) {
//                ended = true;
//            }
//        } catch (SQLException e) {
//            throw new RuntimeException(e);
//        }
//    }
}
