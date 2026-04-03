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

    private long limit = Long.MAX_VALUE;

    private long offset = 0;

    private String query;

    private ConnectionManager connectionManager;

    private final Function<ResultSet, T> parsingFn;

    private ConcurrentLinkedQueue<T> records = new ConcurrentLinkedQueue<>();

    private boolean ended = false;

    private final Function<T, Boolean> predicateValidator;
    private final boolean useInternalLimit;

    public IteratorResultSet(String query,
                             ConnectionManager connectionManager,
                             long limit,
                             Function<ResultSet, T> parsingFn,
                             Function<T, Boolean> predicateValidator
    ) {
        this.query = query;
        this.limit = limit;
        this.parsingFn = parsingFn;
        this.connectionManager = connectionManager;
        this.predicateValidator = predicateValidator;
        this.useInternalLimit = !query.contains("LIMIT");
    }

    @Override
    public boolean hasNext() {
        if(!records.isEmpty()) {
            return true;
        }
        if (ended) {
            return false;
        }
        while(records.isEmpty() && !ended) {
            this.makeRequest();
        }
        return !records.isEmpty();
    }

    private String getQuery() {
        if(useInternalLimit) {
            return query + " LIMIT " + limit + " OFFSET " + offset;
        } else {
            // limit set by the filter itself
            return query + " OFFSET " + offset;
        }

    }

    @Override
    public T next() {
        return records.poll();
    }

    private void makeRequest() {
        long countRes = 0;
        try (Connection connection = connectionManager.getConnection()) {
            try(Statement statement = connection.createStatement()) {
                String nextQuery = getQuery();
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
            if(countRes == 0 || countRes < limit) {
                ended = true;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
