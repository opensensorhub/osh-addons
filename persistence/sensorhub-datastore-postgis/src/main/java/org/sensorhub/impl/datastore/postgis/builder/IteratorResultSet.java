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

    private long limit = 10_000;
    private long offset = 0;
    private long maxElements = 0;
    private long totalFetchedElements = 0;
    private String query;
    private ConnectionManager connectionManager;
    private final Function<ResultSet, T> parsingFn;
    private ConcurrentLinkedQueue<T> records = new ConcurrentLinkedQueue<>();
    private boolean ended = false;
    private final Function<T, Boolean> predicateValidator;
    private static final long MAX_FAILED = 5;
    private long currentFailed = 0;

    private boolean disableLimit = false;

    public IteratorResultSet(String query,
                             ConnectionManager connectionManager,
                             long limit,
                             Function<ResultSet, T> parsingFn,
                             Function<T, Boolean> predicateValidator
    ) {
        QueryCleaner queryCleaner = new QueryCleaner(query);
        this.query = queryCleaner.removeNoLimit().removeSqlLimit().build();
        this.disableLimit = queryCleaner.isDisableLimit();
        this.limit = Math.min(limit,this.limit);
        this.maxElements = limit;
        this.parsingFn = parsingFn;
        this.connectionManager = connectionManager;
        this.predicateValidator = predicateValidator;
    }

    private boolean checkFailed() {
        return (currentFailed <= MAX_FAILED);
    }

    @Override
    public boolean hasNext() {
        if(!checkFailed()) {
            logger.error("Max Failed reached, skipping..");
            return false;
        }
        if(!records.isEmpty()) {
            return true;
        }
        if (ended) {
            return false;
        }
        while(records.isEmpty() && !ended && checkFailed()) {
            this.makeRequest();
        }
        return !records.isEmpty();
    }

    private String getQuery() {
        return (!this.disableLimit) ? query + " LIMIT " + limit + " OFFSET " + offset : query;
    }

    @Override
    public T next() {
        return records.poll();
    }

    private void makeRequest() {
        long countRes = 0;
        String nextQuery="";
        try (Connection connection = connectionManager.getConnection()) {
            try(Statement statement = connection.createStatement()) {
                nextQuery = getQuery();
                if(logger.isDebugEnabled()) {
                    logger.debug(nextQuery);
                }
                try (ResultSet resultSet = statement.executeQuery(nextQuery)){
                    while (resultSet.next()) {
                        countRes++;
                        T res = this.parsingFn.apply(resultSet);
                        if(predicateValidator.apply(res)) {
                            records.add(res);
                        }
                    }
                    offset += limit;
                }
            }
            totalFetchedElements += countRes;
            if(countRes == 0 || countRes < limit || totalFetchedElements >= maxElements) {
                ended = true;
            }
        } catch (SQLException e) {
            logger.error("Cannot Execute the request {}, currentFailed={}",query,currentFailed);
            currentFailed++;
        }
    }

    public static class QueryCleaner {
        private String value;
        private boolean disableLimit = false;
        public QueryCleaner(String value) {
            this.value = value;
        }

        public QueryCleaner removeNoLimit() {
            if (this.value != null && this.value.contains("DISABLE_LIMIT")) {
                this.disableLimit = true;
                this.value = this.value.replace("DISABLE_LIMIT", "");
            }
            return this;
        }

        public QueryCleaner removeSqlLimit() {
            if (this.value != null) {
                this.value = this.value.replaceAll("(?i)\\s+LIMIT\\s+\\d+(\\s+OFFSET\\s+\\d+)?", "");
            }
            return this;
        }

        public String build() {
            return this.value;
        }

        public boolean isDisableLimit() {
            return disableLimit;
        }
    }
}
