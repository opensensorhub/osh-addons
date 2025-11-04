/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis.utils;

import com.google.common.collect.Range;
import net.opengis.gml.v32.AbstractGeometry;
import net.opengis.gml.v32.LinearRing;
import net.opengis.gml.v32.impl.PolygonJTS;
import org.postgresql.util.PGobject;
import org.sensorhub.api.datastore.RangeFilter;
import org.sensorhub.api.datastore.TemporalFilter;
import org.vast.ogc.gml.JTSUtils;

import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

public class PostgisUtils {
    private static final DateTimeFormatter FLEXIBLE_FORMATTER =
            new DateTimeFormatterBuilder()
                    .appendPattern("yyyy-MM-dd HH:mm:ss")
                    .optionalStart()
                    .appendFraction(ChronoField.NANO_OF_SECOND, 9, 9, true)
                    .optionalEnd()
                    .optionalStart()
                    .appendInstant()
                    .optionalEnd()
                    .optionalStart()
                    .appendOffset("+HH:MM", "+00:00")
                    .optionalEnd()
                    .optionalStart()
                    .appendOffset("+HH", "+00")
                    .optionalEnd()
                    .toFormatter();

    public static Timestamp MIN_TIMESTAMP;
    public  static Instant MIN_INSTANT;
    public  static Instant MAX_INSTANT;

    public static final Range<Instant> ALL_TIMES_RANGE = Range.closed(Instant.MIN, Instant.MAX);

    static {
        try {
            MIN_INSTANT = new SimpleDateFormat("yyyy G").parse("4700 BC").toInstant();
            MAX_INSTANT = Instant.parse("3000-01-01T00:00:00Z");
            MIN_TIMESTAMP = Timestamp.from(MIN_INSTANT);

            Class.forName("org.postgresql.Driver");
        } catch (ParseException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean checkTable(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData databaseMetaData = connection.getMetaData();
        try (ResultSet resultSet = databaseMetaData.getTables(null, null, tableName, new String[]{"TABLE"})) {
            return resultSet.next();
        }
    }

    public static void executeQueries(Connection connection, String[] queries) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            for (String query : queries) {
                statement.execute(query);
            }
        }
    }

    public static org.locationtech.jts.geom.Geometry toJTSGeometry(AbstractGeometry abstractGeometry) {
        // check polygon closed rings
        if(abstractGeometry.getClass() == PolygonJTS.class) {
            PolygonJTS poly = (PolygonJTS) abstractGeometry;
            if(poly.getInteriorList() != null) {
                for (LinearRing interiorRings : poly.getInteriorList()) {
                    int length = interiorRings.getPosList().length;
                    System.out.println(Arrays.asList(interiorRings.getPosList()));
//                if(interiorRings.getPosList()[0] != interiorRings.getPosList()[length - 1] ||
//                        interiorRings.getPosList()[0] != interiorRings.getPosList()[length - 1]
//                )
                }
            }
            if(poly.getExterior() != null) {
                int length = poly.getExterior().getPosList().length;
                double[] pos = poly.getExterior().getPosList();
                // 0 == length - 1
                if(pos[0] != pos[length - 2] || pos[1] != pos[length - 1]) {
                    double[] newPosList = new double[length+2];
                    System.arraycopy(pos,0,newPosList,0, length);
                    newPosList[newPosList.length-2] = pos[0];
                    newPosList[newPosList.length-1] = pos[1];

                    poly.getExterior().setPosList(newPosList);
                }
            }
        }
        return JTSUtils.getAsJTSGeometry(abstractGeometry);
    }

    public static Instant readInstantFromString(String time, boolean truncated) {
        if(time == null || time.isEmpty()) {
            throw new RuntimeException("Cannot Parse time "+time);
        }
        if(time.equalsIgnoreCase("-infinity")){
            return Instant.MIN;
        } else if(time.equalsIgnoreCase("infinity")) {
            return Instant.MAX;
        } else {
            if(truncated) {
                return Instant.parse(time).truncatedTo(ChronoUnit.SECONDS);
            } else {
                return Instant.parse(time);
            }
        }
    }

    public static String writeInstantToString(Instant instant, boolean truncated) {
        if(instant.getEpochSecond() < MIN_INSTANT.getEpochSecond()) {
            return "-infinity";
        } else if(instant.getEpochSecond() > MAX_INSTANT.getEpochSecond()) {
            return "infinity";
        } else {
            if(truncated) {
                return instant.truncatedTo(ChronoUnit.SECONDS).toString();
            } else {
                return instant.toString();
            }
        }
    }

    public static String getOperator(TemporalFilter temporalFilter) {
        return getOperator(temporalFilter.getOperator());
    }

    public static String getOperator(RangeFilter.RangeOp operator) {
        String operatorString;
        switch (operator) {
            case CONTAINS ->  operatorString = "@>";
            case EQUALS ->  operatorString = "=";
            default ->  operatorString = "&&";
        }
        return  operatorString;
    }

    public static  String[] getRangeFromTemporal(TemporalFilter temporalFilter) {
        String minTimestamp = temporalFilter.getMin().toString();
        if(temporalFilter.getMin() == Instant.MIN) {
            minTimestamp = "-infinity";
        }
        String maxTimestamp = temporalFilter.getMax().toString();
        if(temporalFilter.getMax() == Instant.MAX) {
            maxTimestamp = "infinity";
        }
        return new String[] {minTimestamp, maxTimestamp};
    }

    public static Instant[] getInstantFromPGObject(PGobject pgRange) {
        String rangeStr = pgRange.getValue();
        String inner = rangeStr.substring(1, rangeStr.length() - 1);
        String[] parts = inner.split(",", 2);
        String startStr = parts[0].trim().replaceAll("\"","");
        String endStr = parts[0].trim().replaceAll("\"","");

        return new Instant[]{getInstantFromPGFormat(startStr), getInstantFromPGFormat(endStr)};
    }
    protected static Instant getInstantFromPGFormat(String pgDataValueStr) {
        if (pgDataValueStr.equals("-infinity")) {
            return Instant.MIN.truncatedTo(ChronoUnit.SECONDS);
        } else if (pgDataValueStr.equals("infinity")) {
            return Instant.MAX.truncatedTo(ChronoUnit.SECONDS);
        } else {
            LocalDateTime ldt = LocalDateTime.parse(pgDataValueStr, FLEXIBLE_FORMATTER);
            return ldt.toInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
        }
    }
    public static String getPgTimestampFromInstant(Instant instant) {
        if(instant.getEpochSecond() < MIN_INSTANT.getEpochSecond()) {
            return "-infinity";
        } else if(instant.getEpochSecond() > MAX_INSTANT.getEpochSecond()) {
            return "infinity";
        } else {
            String instantAsStr = instant.toString();
            return instantAsStr.replace("T", " ").substring(0, instantAsStr.length() - 1);
        }

    }

    // POSTGRES LIMIT
    public static String checkAndGetValidInstant(Instant instant) {
        if(instant.getEpochSecond() < PostgisUtils.MIN_INSTANT.getEpochSecond()) {
            return "-infinity";
        } else if(instant.getEpochSecond() > PostgisUtils.MAX_INSTANT.getEpochSecond()) {
            return "infinity";
        } else {
            return instant.truncatedTo(ChronoUnit.SECONDS).toString();
        }
    }

    public static PGobject createPGobjectValidTimeRange(TemporalFilter temporalFilter) throws SQLException {
        PGobject range = new PGobject();
        range.setType("tsrange");  // type PostgreSQL
        StringBuffer rangeValue = new StringBuffer("[");

        Instant min = temporalFilter.getRange().lowerEndpoint();
        Instant max = temporalFilter.getRange().upperEndpoint();

        if (min.getEpochSecond() < MIN_INSTANT.getEpochSecond()) {
            rangeValue.append("-infinity");
        } else {
            rangeValue.append(PostgisUtils.writeInstantToString(min.truncatedTo(ChronoUnit.SECONDS), false));
        }

        rangeValue.append(",");

        if (max.getEpochSecond() > MAX_INSTANT.getEpochSecond()) {
            rangeValue.append("infinity");
        } else {
            rangeValue.append(PostgisUtils.writeInstantToString(max.truncatedTo(ChronoUnit.SECONDS), false));
        }
        rangeValue.append("]");
        range.setValue(rangeValue.toString());
        return range;
    }
}
