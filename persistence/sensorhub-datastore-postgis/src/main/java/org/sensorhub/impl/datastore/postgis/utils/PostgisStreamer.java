package org.sensorhub.impl.datastore.postgis.utils;

import java.sql.*;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.Spliterator;
import java.util.Spliterators;

public class PostgisStreamer {

    /**
     * Executes a SQL query on a given Statement and returns a Stream<T>.
     *
     * @param statement An already created Statement (must belong to a Connection with autoCommit=false)
     * @param query The full SQL query (SELECT ...)
     * @param fetchSize The number of rows to prefetch on the server side
     * @param mapper A function that converts each row of the ResultSet into a T
     * @param <T> The type of the returned objects
     * @return Stream<T>
     */
    public static <T> Stream<T> streamFromStatement(
            Statement statement,
            String query,
            int fetchSize,
            Function<ResultSet, T> mapper
    ) {
        try {
            statement.setFetchSize(fetchSize);
            ResultSet rs = statement.executeQuery(query);

            Stream<T> stream = StreamSupport.stream(
                    Spliterators.spliteratorUnknownSize(new IteratorFromResultSet<>(rs, mapper), Spliterator.ORDERED),
                    false
            );

            // close the resultSet and the statement when the Stream closes
            return stream.onClose(() -> {
                try {
                    rs.close();
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
        } catch (SQLException e) {
            throw new RuntimeException("Error executing query", e);
        }
    }

    private static class IteratorFromResultSet<T> implements java.util.Iterator<T> {
        private final ResultSet rs;
        private final Function<ResultSet, T> mapper;
        private boolean hasNextChecked = false;
        private boolean hasNext = false;

        IteratorFromResultSet(ResultSet rs, Function<ResultSet, T> mapper) {
            this.rs = rs;
            this.mapper = mapper;
        }

        @Override
        public boolean hasNext() {
            if (hasNextChecked) return hasNext;
            try {
                hasNext = rs.next();
                hasNextChecked = true;
                return hasNext;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public T next() {
            if (!hasNextChecked && !hasNext()) throw new java.util.NoSuchElementException();
            hasNextChecked = false;
            try {
                return mapper.apply(rs);
            } catch (Exception e) {
                throw new RuntimeException("Error mapping row", e);
            }
        }
    }
}

