/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.postgresql.util.PGobject;

import java.sql.*;
import java.util.Random;
import java.util.UUID;

import static org.junit.Assert.assertTrue;

public class TestPostgisConnection {
    private Connection connection;

    @Before
    public void init() {
        try {
            connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/gis", "postgres", "postgres");
            createTable();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @After
    public void close() {
        if(connection != null) {
            try {
                removeTable();
                connection.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void createTable() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS datastreams (id VARCHAR(255), data JSONB, PRIMARY KEY (id));");
        }
    }

    private void removeTable() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE datastreams;");
        }
    }

    @Test
    public void insertAndSelect() throws SQLException {
           // test insert JSONB
            try(Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("INSERT INTO datastreams (id,data) VALUES ('"+ UUID.randomUUID().toString() +"','{\"a\": 1}')");
            }

            // test insert JSONB 3
            for(int i=0;i < 10000; i++) {
                try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO datastreams (id,data) VALUES (?, ?)")) {
                    preparedStatement.setString(1, UUID.randomUUID().toString());

                    var id = new Random().ints(0, 20).findFirst().getAsInt();
                    String json = "{\"name\":\"out1\",\"systemID\":{\"internalID\":{\"scope\":2,\"id\":" + id + "},\"uniqueID\":\"urn:osh:test:sensor:BigId {scope\\u003d2, id\\u003d\\u002704\\u0027(1L)}\"},\"recordStruct\":{\"fieldList\":[{\"uom\":{\"href\":\"http://qudt.org/vocab/unit/UNITLESS\"},\"dataType\":\"DOUBLE\",\"qualityList\":[],\"scalarCount\":1,\"name\":\"comp0\",\"extensionList\":[]},{\"uom\":{\"href\":\"http://qudt.org/vocab/unit/UNITLESS\"},\"dataType\":\"DOUBLE\",\"qualityList\":[],\"scalarCount\":1,\"name\":\"comp1\",\"extensionList\":[]},{\"uom\":{\"href\":\"http://qudt.org/vocab/unit/UNITLESS\"},\"dataType\":\"DOUBLE\",\"qualityList\":[],\"scalarCount\":1,\"name\":\"comp2\",\"extensionList\":[]},{\"uom\":{\"href\":\"http://qudt.org/vocab/unit/UNITLESS\"},\"dataType\":\"DOUBLE\",\"qualityList\":[],\"scalarCount\":1,\"name\":\"comp3\",\"extensionList\":[]},{\"uom\":{\"href\":\"http://qudt.org/vocab/unit/UNITLESS\"},\"dataType\":\"DOUBLE\",\"qualityList\":[],\"scalarCount\":1,\"name\":\"comp4\",\"extensionList\":[]}],\"scalarCount\":-1,\"name\":\"out1\",\"extensionList\":[]},\"recordEncoding\":{\"collapseWhiteSpaces\":true,\"decimalSeparator\":\".\",\"tokenSeparator\":\",\",\"blockSeparator\":\"\\n\",\"extensionList\":[]}}";

                    PGobject jsonObject = new PGobject();
                    jsonObject.setType("json");
                    jsonObject.setValue(json);

                    preparedStatement.setObject(2, jsonObject);
                    int rows = preparedStatement.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }

        // query JSONB data
            try(Statement stmt = connection.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("SELECT * FROM datastreams WHERE data->'systemID'->'internalID'->>'id' = '1'")) {
                    assertTrue(rs.next());
                    int count = 0;
                    while (rs.next()) {
                        System.out.println(rs.getString(1));
                        count++;
                    }
                    System.out.println(count);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
    }
}
